package com.github.zhangwei0621.asplugin.jsondifftool.ui

import com.github.zhangwei0621.asplugin.jsondifftool.util.MyBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindowManager

/** Tools 菜单中的 JsonDiffTool 入口，激活当前项目已注册的工具窗口。 */
class OpenJsonDiffToolAction : DumbAwareAction() {
    init {
        templatePresentation.text = MyBundle["jsondifftool.title"]
        templatePresentation.icon = JsonDiffToolIcons.jsonDiffTool
    }

    /** 可用性判断只读取项目上下文，交给后台线程执行，避免占用 EDT。 */
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    /** 没有打开项目时隐藏入口，因为工具窗口及其文档都属于项目会话。 */
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    /** 使用与 plugin.xml 中 toolWindow.id 相同的标识激活窗口，无需手动创建新面板。 */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("JsonDiffTool")?.activate(null)
    }
}
