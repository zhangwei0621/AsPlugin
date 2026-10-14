package com.github.zhangwei0621.asplugin.jsondiff.ui

import com.github.zhangwei0621.asplugin.jsondiff.logic.JsonDiffController
import com.github.zhangwei0621.asplugin.jsondiff.logic.JsonFormatter
import com.github.zhangwei0621.asplugin.jsondiff.util.MyBundle
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffEditorTitleCustomizer
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.simple.SimpleDiffTool
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.diff.util.DiffUserDataKeysEx
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * JsonDiff 的界面容器，只负责布局、交互绑定和校验状态展示。
 *
 * BorderLayout 分为顶部操作栏、中间原生 Diff 编辑器、底部双栏状态区。
 * 文档监听、后台解析和可撤销的写入操作交给 JsonDiffController。
 * 本面板作为 Disposable 根节点，统一管理控制器与 Diff 编辑器的生命周期。
 */
internal class JsonDiffPanel(project: Project) : JPanel(BorderLayout()), Disposable {
    // 指定 JSON 文件类型，原生编辑器可使用对应的语法高亮和编辑支持。
    private val fileType = FileTypeManager.getInstance().getFileTypeByExtension("json")
    private val left = DiffContentFactory.getInstance().createEditable(project, "", fileType)
    private val right = DiffContentFactory.getInstance().createEditable(project, "", fileType)
    private val diffPanel = DiffManager.getInstance().createRequestPanel(project, this, null)

    // 初始默认开启自动格式化，关闭排序；所有可见文案统一从资源文件读取。
    private val autoFormat = JBCheckBox(MyBundle["jsondiff.autoformat"], true)
    private val sortKeys = JBCheckBox(MyBundle["jsondiff.sort.keys"], false)
    private val leftStatus = JBLabel(MyBundle["jsondiff.status.empty"])
    private val rightStatus = JBLabel(MyBundle["jsondiff.status.empty"])
    private val leftStats = JBLabel()
    private val rightStats = JBLabel()
    // 空状态也保留图标占位，校验状态切换时文字位置和状态栏高度不会跳动。
    private val neutralStatusIcon = EmptyIcon.create(JBUI.scale(16))

    // UI 提供选项和操作名称；控制器回调已返回 EDT，可以直接更新 Swing 控件。
    private val controller = JsonDiffController(
        project, left.document, right.document,
        options = { JsonDiffController.Options(autoFormat.isSelected, sortKeys.isSelected) },
        commandNames = JsonDiffController.CommandNames(
            MyBundle["jsondiff.format"], MyBundle["jsondiff.swap"], MyBundle["jsondiff.clear"],
        ),
        onPending = ::showPendingStatus,
        onResult = { leftResult, rightResult ->
            updateStatus(leftStatus, leftResult)
            updateStatus(rightStatus, rightResult)
            updateStatistics()
        },
    )

    init {
        // 窗口内容销毁时，Disposer 会递归释放控制器及其定时器、监听器。
        Disposer.register(this, controller)

        // 使用 IDE 原生 ActionToolbar，复用图标按钮、悬停提示和主题样式。
        val actions = DefaultActionGroup().apply {
            add(action("jsondiff.format", AllIcons.Actions.PrettyPrint, controller::format))
            add(action("jsondiff.swap", AllIcons.Actions.SwapPanels, controller::swap))
            addSeparator()
            add(action("jsondiff.clear", AllIcons.Actions.GC, controller::clear))
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("JsonDiff.Toolbar", actions, true)
        // Action 获取本面板的数据上下文，避免依赖当前焦点碰巧落在哪个窗口。
        toolbar.targetComponent = this
        autoFormat.toolTipText = MyBundle["jsondiff.autoformat.tooltip"]
        sortKeys.toolTipText = MyBundle["jsondiff.sort.keys.tooltip"]
        val options = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(autoFormat)
            add(Box.createHorizontalStrut(JBUI.scale(12)))
            add(sortKeys)
        }
        // 工具栏先加入操作区，再加入选项区；自定义布局依赖这两个子组件的顺序。
        add(JPanel(JsonDiffToolbarLayout(JBUI.scale(24), JBUI.scale(6))).apply {
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(8, 12),
            )
            add(toolbar.component)
            add(options)
            addComponentListener(object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) {
                    // 宽度改变可能触发换行，需要外层布局重新计算工具栏高度。
                    revalidate()
                }
            })
        }, BorderLayout.NORTH)
        add(diffPanel.component, BorderLayout.CENTER)

        // 两侧状态单元格等宽，中间用细线分隔，避免状态信息混在一起。
        add(JPanel(GridLayout(1, 2)).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
            add(statusCell(leftStatus, leftStats))
            add(statusCell(rightStatus, rightStats).apply {
                border = BorderFactory.createCompoundBorder(
                    JBUI.Borders.customLine(JBColor.border(), 0, 1, 0, 0), border,
                )
            })
        }, BorderLayout.SOUTH)
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                updateStatisticsVisibility()
            }
        })
        updateStatistics()

        // 固定使用原生双栏 Diff 工具；空白差异参与比较，关闭自动格式化后仍可检查排版。
        diffPanel.putContextHints(DiffUserDataKeys.PLACE, "JsonDiff")
        diffPanel.putContextHints(DiffUserDataKeysEx.FORCE_DIFF_TOOL, SimpleDiffTool.INSTANCE)
        diffPanel.putContextHints(DiffUserDataKeys.DO_NOT_IGNORE_WHITESPACES, true)
        val leftTitle = MyBundle["jsondiff.left.title"]
        val rightTitle = MyBundle["jsondiff.right.title"]
        diffPanel.setRequest(SimpleDiffRequest(
            MyBundle["jsondiff.title"], left, right,
            leftTitle, rightTitle,
        ).apply {
            // 标题交给原生 Diff 分栏布局管理，拖动分隔条后仍居中对应各自的编辑区域。
            putUserData(DiffUserDataKeysEx.EDITORS_TITLE_CUSTOMIZER, listOf(
                editorTitle(leftTitle), editorTitle(rightTitle),
            ))
        })

        // 自动格式化开关重新校验；排序开关需要立即改写文档，因此强制格式化。
        autoFormat.addActionListener { controller.refresh() }
        sortKeys.addActionListener { controller.format() }
    }

    /** 统一两侧标题的居中方式和内边距；窄窗口可通过悬停查看完整标题。 */
    private fun editorTitle(title: String) = DiffEditorTitleCustomizer {
        JBLabel(title, SwingConstants.CENTER).apply {
            border = JBUI.Borders.empty(4, 8)
            toolTipText = title
        }
    }

    /** 为工具栏生成可在 IDE 索引期间执行的动作，展示文案留在 UI 层解析。 */
    private fun action(key: String, icon: Icon, perform: () -> Unit) =
        object : DumbAwareAction(MyBundle[key], MyBundle[key], icon) {
            override fun actionPerformed(event: AnActionEvent) = perform()
        }

    /** 构建一侧状态区：校验状态占据剩余空间，行数和字符数靠右排列。 */
    private fun statusCell(status: JBLabel, statistics: JBLabel) = JPanel(BorderLayout(JBUI.scale(12), 0)).apply {
        border = JBUI.Borders.empty(7, 12)
        status.font = JBUI.Fonts.smallFont()
        status.foreground = UIUtil.getContextHelpForeground()
        status.iconTextGap = JBUI.scale(6)
        status.icon = neutralStatusIcon
        statistics.font = JBUI.Fonts.smallFont()
        statistics.foreground = UIUtil.getContextHelpForeground()
        add(status, BorderLayout.CENTER)
        add(statistics, BorderLayout.EAST)
    }

    /**
     * 从当前文档读取统计，格式化后再次调用可展示最新行数。
     * 字符数采用 IDE Document 的 UTF-16 文本长度，不是 UTF-8 字节数。
     * 空文档显示 0 行，避免底层默认的单个空行被当作有效输入。
     */
    private fun updateStatistics() {
        listOf(left.document to leftStats, right.document to rightStats).forEach { (document, label) ->
            label.text = MyBundle[
                "jsondiff.document.stats", if (document.textLength == 0) 0 else document.lineCount, document.textLength,
            ]
            label.toolTipText = label.text
        }
        updateStatisticsVisibility()
    }

    /**
     * 根据两侧状态和统计的实际字体宽度决定是否展示统计。
     * 两侧同步隐藏，窄窗口优先保证错误状态可读；40 是内边距与组件间距的余量。
     */
    private fun updateStatisticsVisibility() {
        val requiredWidth = maxOf(
            leftStatus.preferredSize.width + leftStats.preferredSize.width,
            rightStatus.preferredSize.width + rightStats.preferredSize.width,
        ) + JBUI.scale(40)
        val showStatistics = width / 2 >= requiredWidth
        leftStats.isVisible = showStatistics
        rightStats.isVisible = showStatistics
    }

    /** 输入变化时立即显示等待校验的状态，并清除上次错误颜色及错误路径提示。 */
    private fun showPendingStatus() {
        leftStatus.text = MyBundle["jsondiff.status.pending"]
        rightStatus.text = MyBundle["jsondiff.status.pending"]
        listOf(leftStatus, rightStatus).forEach { label ->
            label.icon = neutralStatusIcon
            label.foreground = UIUtil.getContextHelpForeground()
            label.toolTipText = label.text
        }
        updateStatistics()
    }

    /**
     * 将业务校验结果转换为文案、原生图标和主题颜色。
     * 状态栏保留简短文字，完整错误路径放入 tooltip，避免长路径挤压编辑区布局。
     */
    private fun updateStatus(label: JBLabel, result: JsonFormatter.Result) {
        label.text = when (result) {
            JsonFormatter.Result.Empty -> MyBundle["jsondiff.status.empty"]
            is JsonFormatter.Result.Valid -> MyBundle["jsondiff.status.valid"]
            is JsonFormatter.Result.Invalid -> MyBundle[
                if (result.duplicateKey) "jsondiff.status.duplicate" else "jsondiff.status.invalid",
            ]
        }
        label.icon = when (result) {
            is JsonFormatter.Result.Valid -> AllIcons.General.InspectionsOK
            is JsonFormatter.Result.Invalid -> AllIcons.General.Error
            JsonFormatter.Result.Empty -> neutralStatusIcon
        }
        label.foreground = if (result is JsonFormatter.Result.Invalid) {
            // 优先使用主题定义的错误色；缺少该颜色时分别提供浅色和深色主题的后备值。
            JBColor.namedColor("Label.errorForeground", JBColor(Color(0xB83B3B), Color(0xE89B9B)))
        } else UIUtil.getContextHelpForeground()
        label.toolTipText = if (result is JsonFormatter.Result.Invalid) {
            MyBundle[
                if (result.duplicateKey) "jsondiff.status.duplicate.details" else "jsondiff.status.invalid.details", result.path,
            ]
        } else label.text
    }

    /** 子组件由 Disposer 递归释放，面板本身没有额外需要主动关闭的资源。 */
    override fun dispose() = Unit
}
