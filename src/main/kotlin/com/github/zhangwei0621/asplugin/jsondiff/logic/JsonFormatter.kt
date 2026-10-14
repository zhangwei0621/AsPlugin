package com.github.zhangwei0621.asplugin.jsondiff.logic

import com.github.zhangwei0621.asplugin.internal.gson.GsonBuilder
import com.github.zhangwei0621.asplugin.internal.gson.JsonArray
import com.github.zhangwei0621.asplugin.internal.gson.JsonElement
import com.github.zhangwei0621.asplugin.internal.gson.JsonObject
import com.github.zhangwei0621.asplugin.internal.gson.Strictness
import com.github.zhangwei0621.asplugin.internal.gson.stream.JsonReader
import com.github.zhangwei0621.asplugin.internal.gson.stream.JsonToken
import java.io.IOException
import java.io.StringReader

/**
 * 纯 JSON 解析与格式化逻辑，不依赖 IDE 文档或 UI，可单独进行单元测试。
 *
 * 使用严格模式拒绝非标准 JSON，显式检查重复键，避免解析时悄悄覆盖原数据。
 * sortKeys 仅影响对象键顺序，数组元素顺序始终保留。
 */
internal object JsonFormatter {
    /** 校验结果携带业务数据，具体状态文字和颜色由 UI 层决定。 */
    sealed interface Result {
        /** 空串或只有空白的输入，作为等待输入状态处理。 */
        data object Empty : Result

        /** 校验通过；text 是带缩进且按指定选项处理后的完整 JSON。 */
        data class Valid(val text: String) : Result

        /** path 是解析器定位到的 JSON 路径，duplicateKey 用于区分重复键错误。 */
        data class Invalid(val path: String, val duplicateKey: Boolean = false) : Result
    }

    // 保留 null 字段，并让 <、>、& 等字符保持可读；字符串引号仍正常转义。
    private val gson = GsonBuilder()
        .setStrictness(Strictness.STRICT)
        .setPrettyPrinting()
        .serializeNulls()
        .disableHtmlEscaping()
        .create()
    // 基础值交给 Gson 的 JsonElement 适配器读取，不转换为 Double，避免丢失数字精度。
    private val elementAdapter = gson.getAdapter(JsonElement::class.java)

    /**
     * 校验整段输入并生成格式化文本，原始字符串不会被修改。
     *
     * JSON 后存在额外内容也视为错误；解析失败时返回路径，不向 UI 抛出普通格式异常。
     * 是否用结果替换编辑器原文，由控制器根据自动格式化选项决定。
     */
    fun format(text: String, sortKeys: Boolean = false): Result {
        if (text.isBlank()) return Result.Empty

        JsonReader(StringReader(text)).use { reader ->
            reader.strictness = Strictness.STRICT
            return try {
                val element = readElement(reader, sortKeys)
                // 成功读取一个值还不代表整段输入合法，例如 "{}{}" 含有两个顶层值。
                if (reader.peek() != JsonToken.END_DOCUMENT) {
                    Result.Invalid(reader.path)
                } else {
                    Result.Valid(gson.toJson(element))
                }
            } catch (exception: DuplicateKeyException) {
                Result.Invalid(exception.jsonPath, duplicateKey = true)
            } catch (_: IOException) {
                Result.Invalid(reader.path)
            } catch (_: IllegalStateException) {
                Result.Invalid(reader.path)
            }
        }
    }

    /**
     * 递归构建 JSON 树：对象自行读取以检查重复键，数组保持原顺序，基础值委托 Gson。
     * 每一级对象都应用 sortKeys，因此嵌套对象也能按键名排序。
     */
    private fun readElement(reader: JsonReader, sortKeys: Boolean): JsonElement = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            // 默认记录输入顺序；先查重再写入，防止同名字段被 Map 覆盖。
            val members = linkedMapOf<String, JsonElement>()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (members.containsKey(name)) throw DuplicateKeyException(reader.path)
                members[name] = readElement(reader, sortKeys)
            }
            reader.endObject()
            JsonObject().apply {
                val orderedMembers = if (sortKeys) members.toSortedMap() else members
                orderedMembers.forEach { (name, value) -> add(name, value) }
            }
        }
        JsonToken.BEGIN_ARRAY -> {
            reader.beginArray()
            JsonArray().apply {
                while (reader.hasNext()) add(readElement(reader, sortKeys))
                reader.endArray()
            }
        }
        JsonToken.STRING, JsonToken.NUMBER, JsonToken.BOOLEAN, JsonToken.NULL -> elementAdapter.read(reader)
        // END_DOCUMENT、结束括号等不能作为一个新的 JSON 值起始标记。
        else -> throw IllegalStateException()
    }

    /** 内部控制流程使用的异常，携带重复字段路径，最终转换为 Invalid 结果。 */
    private class DuplicateKeyException(val jsonPath: String) : RuntimeException()
}
