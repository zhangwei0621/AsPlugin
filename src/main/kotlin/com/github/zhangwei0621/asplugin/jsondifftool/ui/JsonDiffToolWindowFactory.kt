package com.github.zhangwei0621.asplugin.jsondifftool.ui

import com.github.zhangwei0621.asplugin.jsondifftool.util.MyBundle
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * plugin.xml 注册的工具窗口工厂，由 IDE 在创建 JsonDiffTool 内容时调用。
 * 实现 DumbAware，允许项目索引期间打开工具窗口。
 */
class JsonDiffToolWindowFactory : ToolWindowFactory, DumbAware {
    /** 为当前项目创建面板，并将其生命周期绑定到窗口内容。 */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JsonDiffToolPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, MyBundle["jsondifftool.title"], false)
        // 移除或销毁 Content 时，平台会通过 Disposer 释放面板及其全部子级资源。
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}
