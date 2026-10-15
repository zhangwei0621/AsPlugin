package com.github.zhangwei0621.asplugin.jsondifftool.logic

import com.github.zhangwei0621.asplugin.jsondifftool.util.JsonCaretMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm

/**
 * 管理两侧 JSON 文档的交互流程，不直接操作 Swing 控件。
 *
 * 输入变化后先防抖，再到后台解析；结果回到 EDT 后才更新文档并通知 UI。
 * UI 在创建控制器时传入当前选项、已解析的操作文案和状态回调。
 * 面板通过 Disposer 管理本控制器，文档监听器和 Alarm 随控制器一起释放。
 *
 * @param options 在 EDT 上读取当前选项，避免后台线程访问复选框。
 * @param commandNames 用于撤销菜单的操作名称，由 UI 层从资源文件获取。
 * @param onPending 在 EDT 上通知 UI 显示校验中状态。
 * @param onResult 在 EDT 上返回左右结果，文档格式化已在回调前完成。
 */
internal class JsonDiffToolController(
    private val project: Project,
    private val left: Document,
    private val right: Document,
    private val options: () -> Options,
    private val commandNames: CommandNames,
    private val onPending: () -> Unit,
    private val onResult: (JsonFormatter.Result, JsonFormatter.Result) -> Unit,
) : Disposable {
    /** 一次处理所使用的选项快照；后台解析期间不再读取界面状态。 */
    data class Options(val autoFormat: Boolean, val sortKeys: Boolean)

    /** 各种文档修改对应的撤销操作名称，逻辑层无需依赖文案资源。 */
    data class CommandNames(val format: String, val swap: String, val clear: String)

    // 定时请求在 EDT 上触发，读取文档和选项后才交给后台线程处理。
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    // 控制器自己修改文档时抑制监听回调，避免格式化再次触发格式化。
    private var applyingChanges = false

    // 每次请求递增；即使旧后台任务已经启动，也不能覆盖较新的输入。
    private var revision = 0
    @Volatile private var disposed = false

    init {
        // 两侧共享一个监听器。撤销或重做仍进行校验，但不立即重新格式化，
        // 否则用户刚恢复的文本可能再次被自动修改。
        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (!applyingChanges) {
                    scheduleUpdate(skipFormat = UndoManager.getInstance(project).isUndoOrRedoInProgress)
                }
            }
        }
        left.addDocumentListener(listener, this)
        right.addDocumentListener(listener, this)
    }

    /** 手动格式化或切换排序选项：忽略自动格式化开关，并立即开始处理。 */
    fun format() = scheduleUpdate(forceFormat = true)

    /** 根据当前选项重新校验；仅在开启自动格式化时改写合法输入。 */
    fun refresh() = scheduleUpdate()

    /** 用同一个写入命令交换两侧内容，方便用户一次撤销整个交换操作。 */
    fun swap() {
        val leftText = left.text
        val rightText = right.text
        changeDocuments(commandNames.swap) {
            left.setText(rightText)
            right.setText(leftText)
        }
        scheduleUpdate()
    }

    /** 清空两侧文档，并重新计算空输入状态；清空本身也支持撤销。 */
    fun clear() {
        changeDocuments(commandNames.clear) {
            left.setText("")
            right.setText("")
        }
        scheduleUpdate()
    }

    /**
     * 合并连续输入产生的请求，并保证只有最新请求可以应用结果。
     *
     * @param forceFormat 为 true 时跳过 600 毫秒防抖，强制格式化合法的一侧或两侧。
     * @param skipFormat 撤销或重做时只校验，保留用户恢复的文档排版。
     */
    private fun scheduleUpdate(forceFormat: Boolean = false, skipFormat: Boolean = false) {
        if (disposed || project.isDisposed) return
        val currentRevision = ++revision
        // 这里只取消尚未触发的定时请求；已运行的后台任务由 revision 检查拦截。
        alarm.cancelAllRequests()
        onPending()
        alarm.addRequest({
            // 在 EDT 上获取完整快照，后台线程仅处理普通字符串和不可变选项。
            val leftText = left.text
            val rightText = right.text
            val currentOptions = options()
            val shouldFormat = forceFormat || (currentOptions.autoFormat && !skipFormat)
            val application = ApplicationManager.getApplication()
            // 保留发起请求时的模态状态，让结果回调遵守 IDE 对话框的调度规则。
            val modalityState = ModalityState.current()
            application.executeOnPooledThread {
                val leftResult = JsonFormatter.format(leftText, currentOptions.sortKeys)
                val rightResult = JsonFormatter.format(rightText, currentOptions.sortKeys)
                application.invokeLater({
                    // 丢弃旧输入的后台结果，窗口关闭后不再更新文档或界面。
                    if (disposed || project.isDisposed || currentRevision != revision) return@invokeLater
                    if (shouldFormat) {
                        // 两侧一起进入写入命令；非法输入由 applyFormat 保留原文。
                        changeDocuments(commandNames.format) {
                            applyFormat(left, leftResult)
                            applyFormat(right, rightResult)
                        }
                    }
                    onResult(leftResult, rightResult)
                }, modalityState)
            }
        }, if (forceFormat) 0 else 600)
    }

    /**
     * 将合法格式化结果应用到文档，并恢复引用该文档的所有编辑器光标和选区。
     * 非法结果和未发生变化的文本都直接跳过，避免无效写入影响撤销历史。
     */
    private fun applyFormat(document: Document, result: JsonFormatter.Result) {
        if (result !is JsonFormatter.Result.Valid || document.text == result.text) return
        val source = document.text
        // setText 会改变偏移位置，必须在替换前根据旧文本计算所有目标位置。
        // Triple 依次保存光标对象、目标偏移和可选的选区起止偏移。
        val carets = EditorFactory.getInstance().getEditors(document, project).flatMap { editor ->
            editor.caretModel.allCarets.map { caret ->
                Triple(caret, JsonCaretMapper.mapOffset(source, result.text, caret.offset),
                    if (caret.hasSelection()) {
                        JsonCaretMapper.mapOffset(source, result.text, caret.selectionStart) to
                            JsonCaretMapper.mapOffset(source, result.text, caret.selectionEnd)
                    } else null)
            }
        }
        document.setText(result.text)
        carets.forEach { (caret, offset, selection) ->
            caret.moveToOffset(offset)
            if (selection != null) caret.setSelection(selection.first, selection.second)
        }
    }

    /**
     * 统一执行可撤销的文档修改，同时屏蔽修改过程中的递归监听。
     * finally 保证写入失败后也恢复监听，使后续输入仍能正常触发校验。
     */
    private fun changeDocuments(name: String, change: () -> Unit) {
        applyingChanges = true
        try {
            WriteCommandAction.writeCommandAction(project).withName(name).run<RuntimeException>(change)
        } finally {
            applyingChanges = false
        }
    }

    /** 标记控制器失效；Disposer 另外负责释放子级 Alarm 和文档监听器。 */
    override fun dispose() {
        disposed = true
        revision++
    }
}
