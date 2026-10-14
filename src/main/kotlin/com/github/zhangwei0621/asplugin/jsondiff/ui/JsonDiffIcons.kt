package com.github.zhangwei0621.asplugin.jsondiff.ui

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.JBColor
import com.intellij.ui.icons.DarkIconProvider
import com.intellij.ui.icons.MenuBarIconProvider
import com.intellij.util.IconUtil
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * 工具窗口和菜单入口共用的 JsonDiff 图标。
 * 复用品牌 SVG，由 IconLoader 根据主题选择同目录的 _dark 版本，避免重复维护素材。
 */
internal object JsonDiffIcons {
    /**
     * 将 40×40 的品牌图标缩放为入口使用的 16×16，保留平台的缩放与主题支持。
     * 工具窗口需要 ScalableIcon，原生菜单需要保留缩放后的尺寸，两种入口共用同一实现。
     * JvmField 导出静态字段，供 plugin.xml 按完整类名和字段名加载图标。
     * 字段必须声明为 Icon：新版平台通过 MethodHandles 按此精确类型查找字段。
     * 实际对象由 EntryIcon 实现 ScalableIcon，满足侧边栏的运行时缩放要求。
     */
    @JvmField
    val jsonDiff: Icon = EntryIcon(
        IconLoader.getIcon("/META-INF/pluginIcon.svg", JsonDiffIcons::class.java),
    )

    /**
     * 将入口图标的基础尺寸定义为 16×16，并提供平台要求的缩放、主题和菜单转换接口。
     * 不实现 RetrievableIcon，防止原生菜单解包后重新加载 40×40 的品牌原图。
     * 内层 SVG 图标负责实际绘制及屏幕缩放；这里的 factor 只表示入口尺寸的缩放倍数。
     * darkOverride 为空时跟随 IDE 界面主题，菜单显式指定主题时则优先使用指定值。
     */
    private class EntryIcon(
        private val source: Icon,
        private val factor: Float = 1f,
        private val darkOverride: Boolean? = null,
    ) : ScalableIcon, DarkIconProvider, MenuBarIconProvider {
        // 两种主题分别缓存，但每次绘制重新选择主题，已创建的侧边栏图标也能立即更新颜色。
        private val lightDelegate by lazy { themedIcon(false) }
        private val darkDelegate by lazy { themedIcon(true) }
        private val delegate: Icon
            get() = if (darkOverride ?: !JBColor.isBright()) darkDelegate else lightDelegate

        private fun themedIcon(isDark: Boolean): Icon = IconUtil.scale(
            IconLoader.getDarkIcon(source, isDark), null, (16f / 40f) * factor,
        )

        override fun getIconWidth() = delegate.iconWidth

        override fun getIconHeight() = delegate.iconHeight

        override fun paintIcon(component: Component?, graphics: Graphics, x: Int, y: Int) =
            delegate.paintIcon(component, graphics, x, y)

        override fun getScale() = factor

        /** 缩放相对于 16×16 的入口基础尺寸，重复调用不会累积倍数或回到品牌原图大小。 */
        override fun scale(scaleFactor: Float): Icon =
            if (scaleFactor == factor) this else EntryIcon(source, scaleFactor, darkOverride)

        /** 菜单主题可能与 IDE 主题不同，因此保留平台显式选择浅色和深色图标的能力。 */
        override fun getDarkIcon(isDark: Boolean): Icon =
            EntryIcon(source, factor, isDark)

        /** 原生菜单使用当前入口尺寸和指定主题，不重新读取品牌原图尺寸。 */
        override fun getMenuBarIcon(isDark: Boolean): Icon = getDarkIcon(isDark)
    }
}
