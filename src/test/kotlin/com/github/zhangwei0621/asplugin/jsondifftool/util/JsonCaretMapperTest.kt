package com.github.zhangwei0621.asplugin.jsondifftool.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** 验证新增缩进和换行后，光标仍能映射到对应的有效字符。 */
class JsonCaretMapperTest {
    @Test
    fun `格式化后光标仍指向原来的值`() {
        val source = """{"a":123}"""
        val target = "{\n  \"a\": 123\n}"
        // 同时覆盖数值内部的光标和文件末尾，两者都不能被新增缩进推到错误位置。
        assertEquals(target.indexOf('2'), JsonCaretMapper.mapOffset(source, target, source.indexOf('2')))
        assertEquals(target.length, JsonCaretMapper.mapOffset(source, target, source.length))
    }

    @Test
    fun `字符串内的空格及转义引号不会影响光标映射`() {
        // 转义引号不能被误认为字符串结束，字符串内的空格也不能当作排版空白忽略。
        val source = """{"a":"空 格\" 内","b":2}"""
        val target = "{\n  \"a\": \"空 格\\\" 内\",\n  \"b\": 2\n}"
        assertEquals(target.indexOf('内'), JsonCaretMapper.mapOffset(source, target, source.indexOf('内')))
        assertEquals(target.indexOf('2'), JsonCaretMapper.mapOffset(source, target, source.indexOf('2')))
    }
}
