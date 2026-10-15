package com.github.zhangwei0621.asplugin.jsondifftool.ui

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.JBColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.invoke.MethodHandles
import java.awt.image.BufferedImage
import javax.swing.Icon

/** 验证工具窗口和原生菜单要求的图标接口，防止尺寸修复导致整个工具窗口注册失败。 */
class JsonDiffToolIconsTest {
    @Before
    fun activateIcons() {
        // 普通单元测试不会启动 IDE，需要显式允许 IconLoader 加载实际 SVG 尺寸。
        IconLoader.activate()
    }

    @Test
    fun `工具窗口入口提供可缩放图标接口`() {
        val icon = JsonDiffToolIcons.jsonDiffTool
        assertTrue(icon is ScalableIcon)
        assertEquals(16, icon.iconWidth)
        assertEquals(16, icon.iconHeight)
    }

    @Test
    fun `XML图标入口可按平台要求的精确字段类型反射加载`() {
        // 与新版 AS 的 XML 图标加载方式一致，字段类型必须精确匹配 Icon 而非子接口。
        val getter = MethodHandles.publicLookup().findStaticGetter(
            JsonDiffToolIcons::class.java, "jsonDiffTool", Icon::class.java,
        )
        val icon = getter.invoke() as Icon
        assertTrue(icon is ScalableIcon)
        assertEquals(16, icon.iconWidth)
        assertEquals(16, icon.iconHeight)
    }

    @Test
    fun `工具窗口放大图标后可以恢复入口基础尺寸`() {
        val enlarged = (JsonDiffToolIcons.jsonDiffTool as ScalableIcon).scale(1.25f)
        assertTrue(enlarged is ScalableIcon)
        assertEquals(20, enlarged.iconWidth)
        assertEquals(20, enlarged.iconHeight)
        val restored = (enlarged as ScalableIcon).scale(1f)
        assertEquals(16, restored.iconWidth)
        assertEquals(16, restored.iconHeight)
    }

    @Test
    fun `原生菜单转换在两种主题下都保留入口尺寸`() {
        listOf(false, true).forEach { dark ->
            val menuIcon = IconLoader.getMenuBarIcon(JsonDiffToolIcons.jsonDiffTool, dark)
            assertTrue(menuIcon is ScalableIcon)
            assertEquals(16, menuIcon.iconWidth)
            assertEquals(16, menuIcon.iconHeight)
        }
    }

    @Test
    fun `切换主题后仍能按工具窗口要求缩放`() {
        val darkIcon = IconLoader.getDarkIcon(JsonDiffToolIcons.jsonDiffTool, true)
        assertTrue(darkIcon is ScalableIcon)
        val enlarged = (darkIcon as ScalableIcon).scale(1.25f)
        assertEquals(20, enlarged.iconWidth)
        assertEquals(20, enlarged.iconHeight)
    }

    @Test
    fun `同一个侧边栏图标跟随界面主题切换并恢复原颜色`() {
        val previousDark = !JBColor.isBright()
        try {
            JBColor.setDark(false)
            val icon = (JsonDiffToolIcons.jsonDiffTool as ScalableIcon).scale(1.25f)
            val lightPixels = render(icon)
            JBColor.setDark(true)
            val darkPixels = render(icon)
            assertFalse(lightPixels.contentEquals(darkPixels))
            JBColor.setDark(false)
            assertTrue(lightPixels.contentEquals(render(icon)))
        } finally {
            // 恢复全局主题状态，避免影响其他 Swing 测试。
            JBColor.setDark(previousDark)
        }
    }

    @Test
    fun `菜单指定的图标主题不受界面主题切换影响`() {
        val previousDark = !JBColor.isBright()
        try {
            JBColor.setDark(false)
            val darkMenu = IconLoader.getMenuBarIcon(JsonDiffToolIcons.jsonDiffTool, true)
            val lightMenu = IconLoader.getMenuBarIcon(JsonDiffToolIcons.jsonDiffTool, false)
            val darkPixels = render(darkMenu)
            val lightPixels = render(lightMenu)
            assertFalse(darkPixels.contentEquals(lightPixels))
            JBColor.setDark(true)
            assertTrue(darkPixels.contentEquals(render(darkMenu)))
            assertTrue(lightPixels.contentEquals(render(lightMenu)))
        } finally {
            JBColor.setDark(previousDark)
        }
    }

    /** 绘制实际像素，避免只有尺寸正确但颜色一直停留在旧主题的问题漏过测试。 */
    private fun render(icon: Icon): IntArray {
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            icon.paintIcon(null, graphics, 0, 0)
        } finally {
            graphics.dispose()
        }
        return image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    }
}
