package com.github.zhangwei0621.asplugin.jsondiff.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JPanel

/** 用固定组件尺寸验证工具栏单行、换行和恢复布局，排除 IDE 主题与字体的干扰。 */
class JsonDiffToolbarLayoutTest {
    @Test
    fun `宽窗口中操作与选项分居两端并保持垂直居中`() {
        val panel = toolbar(1200)
        panel.doLayout()
        val commands = panel.getComponent(0)
        val options = panel.getComponent(1)
        // 12 是左右内边距；两组高度不同，但垂直中心必须一致。
        assertEquals(12, commands.x)
        assertEquals(1188, options.x + options.width)
        assertEquals(commands.y + commands.height / 2, options.y + options.height / 2)
        assertEquals(48, panel.preferredSize.height)
    }

    @Test
    fun `窄窗口中选项换行且所有控件仍在工具栏内`() {
        val panel = toolbar(360)
        panel.doLayout()
        val commands = panel.getComponent(0)
        val options = panel.getComponent(1)
        assertEquals(commands.x, options.x)
        assertTrue(options.y >= commands.y + commands.height + 6)
        assertEquals(78, panel.preferredSize.height)
        // 不只检查是否换行，还检查每个组件是否落在可用内容区域内。
        panel.components.forEach { component ->
            assertTrue(component.x + component.width <= panel.width - panel.insets.right)
            assertTrue(component.y + component.height <= panel.height - panel.insets.bottom)
        }
    }

    @Test
    fun `窗口宽度反复变化时工具栏高度恢复且不重叠`() {
        val panel = toolbar(1200)
        // 模拟用户反复拖动窗口，检查双行高度不会在恢复宽窗口后残留。
        listOf(360, 1200, 360, 1200).forEach { width ->
            panel.setSize(width, panel.height)
            panel.setSize(width, panel.preferredSize.height)
            panel.doLayout()
            val commands = panel.getComponent(0)
            val options = panel.getComponent(1)
            assertTrue(!commands.bounds.intersects(options.bounds))
            assertEquals(if (width == 360) 78 else 48, panel.height)
        }
    }

    /** 创建固定操作区和选项区，并按布局报告的高度分配容器尺寸。 */
    private fun toolbar(width: Int) = JPanel(JsonDiffToolbarLayout(24, 6)).apply {
        border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
        add(JPanel().apply { preferredSize = Dimension(112, 32) })
        add(JPanel().apply { preferredSize = Dimension(240, 24) })
        setSize(width, 0)
        setSize(width, preferredSize.height)
    }
}
