package com.github.zhangwei0621.asplugin.jsondiff.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 直接测试纯解析逻辑，覆盖标准 JSON、保真格式化和非法输入，不需要启动 IDE。 */
class JsonFormatterTest {
    @Test
    fun `自动格式化对象并保留空值及中文`() {
        // 同时检查 null 不被丢弃、中文正常输出、HTML 特殊字符不被额外编码。
        assertEquals(
            "{\n  \"名称\": \"测试\",\n  \"值\": null,\n  \"网址\": \"<a>&中文\"\n}",
            valid("""{"名称":"测试","值":null,"网址":"<a>&中文"}"""),
        )
    }

    @Test
    fun `对象键可递归排序但数组顺序保持不变`() {
        // 顶层及数组中的对象键顺序不同，排序后应一致；数组中的 3、2、1 不能重排。
        val left = """{"z":[{"b":2,"a":1},3,2,1],"a":true}"""
        val right = """{"a":true,"z":[{"a":1,"b":2},3,2,1]}"""
        assertEquals(valid(left, true), valid(right, true))
        assertFalse(valid(left) == valid(right))
        assertTrue(valid(left, true).endsWith("    3,\n    2,\n    1\n  ]\n}"))
    }

    @Test
    fun `大整数高精度小数和指数数字不会丢失精度`() {
        // 数值范围和精度超出普通 Double，字符串断言可直接发现截断或表示被改写。
        val text = """[123456789012345678901234567890,0.12345678901234567890123456789,1e+100,-0]"""
        assertEquals(
            "[\n  123456789012345678901234567890,\n  0.12345678901234567890123456789,\n  1e+100,\n  -0\n]",
            valid(text),
        )
    }

    @Test
    fun `空输入与各种顶层 JSON 值均被正确处理`() {
        // 标准 JSON 的顶层不局限于对象；空白输入则单独返回等待输入状态。
        assertEquals(JsonFormatter.Result.Empty, JsonFormatter.format(" \n\t"))
        listOf("null", "true", "42", "\"中文\"", "[]", "{}").forEach { text ->
            assertEquals(text, valid(text))
        }
    }

    @Test
    fun `拒绝非标准 JSON 和未完成输入`() {
        // 覆盖宽松解析器可能接受的语法，同时检查多个顶层值和非法字符串转义。
        listOf(
            "{", "[1,]", "{'a':1}", "{a:1}", "{\"a\":NaN}",
            "{\"a\":1}//注释", "{}{}", "null true", "01", "\"未转义\n换行\"",
            "\"\\uZZZZ\"", "\"\\x00\"", "[Infinity]", "[1;2]", "/*注释*/{}",
        ).forEach { text ->
            assertTrue(text, JsonFormatter.format(text) is JsonFormatter.Result.Invalid)
        }
    }

    @Test
    fun `拒绝重复键避免格式化覆盖原数据`() {
        // "a" 与 Unicode 转义后的同名键相同，检查的是解析后的键而不是原始写法。
        val result = JsonFormatter.format("""{"items":[{"a":1,"\u0061":2}]}""")
        assertTrue(result is JsonFormatter.Result.Invalid)
        result as JsonFormatter.Result.Invalid
        assertTrue(result.duplicateKey)
        assertEquals("$.items[0].a", result.path)
    }

    @Test
    fun `格式化重复执行结果不变`() {
        // 反复格式化不应持续改变排版，字符串内部的引号和反斜杠也应正确保留。
        val formatted = valid("""{"a":[1,{"b":"引号\"和反斜杠\\"}]}""")
        assertEquals(formatted, valid(formatted))
    }

    @Test
    fun `过深的嵌套返回格式错误`() {
        // 超出 Gson reader 的嵌套限制时应返回 Invalid，不能让异常传到 UI。
        val text = "[".repeat(300) + "0" + "]".repeat(300)
        assertTrue(JsonFormatter.format(text) is JsonFormatter.Result.Invalid)
    }

    /** 合法用例共用的断言入口：先确认结果类型，再取格式化文本进行具体比对。 */
    private fun valid(text: String, sortKeys: Boolean = false): String {
        val result = JsonFormatter.format(text, sortKeys)
        assertTrue(result.toString(), result is JsonFormatter.Result.Valid)
        return (result as JsonFormatter.Result.Valid).text
    }
}
