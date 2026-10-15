package com.github.zhangwei0621.asplugin.jsondifftool.util

/**
 * 在格式化前后按非排版字符的位置映射光标，减少缩进变化造成的跳动。
 *
 * 这种映射适合仅改变字符串外空白的格式化。键排序或数字表示发生变化时，
 * 字符序列可能不同，此时只是近似位置映射，不保证继续指向同一个业务字段。
 */
internal object JsonCaretMapper {
    /**
     * 把 source 中的光标或选区偏移映射到 target。
     * source 末尾映射到 target 末尾；空白位置映射到下一个有效字符。
     */
    fun mapOffset(source: String, target: String, offset: Int): Int {
        if (offset >= source.length) return target.length
        val sourceOffsets = significantOffsets(source)
        val targetOffsets = significantOffsets(target)
        // binarySearch 的负返回值编码了插入位置，用它找到空白之后的有效字符。
        val index = sourceOffsets.binarySearch(offset.coerceAtLeast(0)).let {
            if (it >= 0) it else -it - 1
        }
        return targetOffsets.getOrElse(index) { target.length }
    }

    /** 收集有效字符的实际偏移，仅忽略字符串外的空白，字符串中的空格仍属于数据。 */
    private fun significantOffsets(text: String): List<Int> {
        var inString = false
        var escaped = false
        return buildList {
            text.forEachIndexed { index, char ->
                if (inString || !char.isWhitespace()) add(index)
                // 转义符后的引号属于字符串内容，不能切换字符串内外状态。
                if (escaped) {
                    escaped = false
                } else if (inString && char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = !inString
                }
            }
        }
    }
}
