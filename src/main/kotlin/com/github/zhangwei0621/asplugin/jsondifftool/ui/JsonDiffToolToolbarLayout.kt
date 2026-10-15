package com.github.zhangwei0621.asplugin.jsondifftool.ui

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

/**
 * 仅用于包含两个子组件的工具栏：第一个是操作区，第二个是选项区。
 * 宽度足够时左右分布并垂直居中；宽度不足时选项区换到第二行。
 *
 * 间距由调用方通过 JBUI.scale 处理，本类仅负责计算尺寸和坐标，便于独立测试。
 */
internal class JsonDiffToolToolbarLayout(
    private val horizontalGap: Int,
    private val verticalGap: Int,
) : LayoutManager {
    // 布局按固定子组件顺序计算，无需额外保存名称或约束对象。
    override fun addLayoutComponent(name: String?, component: Component) = Unit

    override fun removeLayoutComponent(component: Component) = Unit

    /** 向外层布局报告所需高度；尚未获得窗口宽度时先按单行计算。 */
    override fun preferredLayoutSize(parent: Container): Dimension {
        val commands = parent.getComponent(0).preferredSize
        val options = parent.getComponent(1).preferredSize
        val insets = parent.insets
        // 内容可用宽度需要减去边框和内边距，不能直接使用窗口总宽度。
        val width = commands.width + horizontalGap + options.width
        val availableWidth = parent.width - insets.left - insets.right
        val height = if (parent.width > 0 && availableWidth < width) {
            commands.height + verticalGap + options.height
        } else {
            maxOf(commands.height, options.height)
        }
        return Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom)
    }

    /** 最小尺寸按双行估算：宽度取较宽的一组，高度包含两组及行间距。 */
    override fun minimumLayoutSize(parent: Container): Dimension {
        val commands = parent.getComponent(0).minimumSize
        val options = parent.getComponent(1).minimumSize
        val insets = parent.insets
        return Dimension(
            maxOf(commands.width, options.width) + insets.left + insets.right,
            commands.height + verticalGap + options.height + insets.top + insets.bottom,
        )
    }

    /** 实际放置两个组件；宽度再小时限制组件边界，避免绘制越过容器边缘。 */
    override fun layoutContainer(parent: Container) {
        val commands = parent.getComponent(0)
        val options = parent.getComponent(1)
        val commandSize = commands.preferredSize
        val optionSize = options.preferredSize
        val insets = parent.insets
        val width = (parent.width - insets.left - insets.right).coerceAtLeast(0)
        val stacked = width < commandSize.width + horizontalGap + optionSize.width
        val rowHeight = maxOf(commandSize.height, optionSize.height)
        // 操作区始终靠左；单行模式按较高组件的高度对齐中心。
        commands.setBounds(
            insets.left,
            insets.top + if (stacked) 0 else (rowHeight - commandSize.height) / 2,
            minOf(commandSize.width, width), commandSize.height,
        )
        // 选项区在单行时靠右，双行时回到左侧并位于操作区下方。
        options.setBounds(
            insets.left + if (stacked) 0 else width - optionSize.width,
            insets.top + if (stacked) commandSize.height + verticalGap else (rowHeight - optionSize.height) / 2,
            minOf(optionSize.width, width), optionSize.height,
        )
    }
}
