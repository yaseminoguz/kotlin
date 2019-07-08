/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.actions.DebuggerActions
import com.intellij.debugger.actions.GotoFrameSourceAction
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerContextListener
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.impl.DebuggerStateManager
import com.intellij.debugger.ui.impl.DebuggerTreePanel
import com.intellij.debugger.ui.impl.watch.DebuggerTree
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.ui.tree.StackFrameDescriptor
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPopupMenu
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.Alarm
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.frame.XWatchesViewImpl
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode
import com.sun.jdi.*
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.evaluate.createExecutionContext
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JTree

/**
 * Actually added into ui in [CoroutinesDebugConfigurationExtension.registerCoroutinesPanel]
 */
class CoroutinesPanel(project: Project, stateManager: DebuggerStateManager) : DebuggerTreePanel(project, stateManager) {
    private val myUpdateLabelsAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)

    init {
        val disposable = installAction(getCoroutinesTree(), DebuggerActions.EDIT_FRAME_SOURCE)
        registerDisposable(disposable)
        add(ScrollPaneFactory.createScrollPane(getCoroutinesTree()), BorderLayout.CENTER)
        stateManager.addListener(object : DebuggerContextListener {
            override fun changeEvent(newContext: DebuggerContextImpl, event: DebuggerSession.Event) {
                if (DebuggerSession.Event.ATTACHED == event || DebuggerSession.Event.RESUME == event) {
                    startLabelsUpdate()
                } else if (DebuggerSession.Event.PAUSE == event
                    || DebuggerSession.Event.DETACHED == event
                    || DebuggerSession.Event.DISPOSE == event
                ) {
                    myUpdateLabelsAlarm.cancelAllRequests()
                }
                if (DebuggerSession.Event.DETACHED == event || DebuggerSession.Event.DISPOSE == event) {
                    stateManager.removeListener(this)
                }
            }
        })
        startLabelsUpdate()
    }

    @Suppress("SameParameterValue")
    private fun installAction(tree: JTree, actionName: String): () -> Unit {
        val psiFacade = JavaPsiFacade.getInstance(project)

        val listener = object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                val location = tree.getPathForLocation(e.x, e.y)
                    ?.lastPathComponent as? DebuggerTreeNodeImpl ?: return false

                val dataContext = DataManager.getInstance().getDataContext(tree)
                val context = DebuggerManagerEx.getInstanceEx(project).context
                val view = (context.debuggerSession!!.xDebugSession as XDebugSessionImpl)
                    .sessionTab!!.watchesView as XWatchesViewImpl

                when (val descriptor = location.userObject) {
                    is CoroutinesDebuggerTree.SuspendStackFrameDescriptor -> {
                        val frame = descriptor.frame
                        val psiClass = psiFacade.findClass(
                            frame.className.substringBefore("$"),
                            GlobalSearchScope.everythingScope(project)
                        )
                        val classFile = psiClass?.containingFile?.virtualFile
                        val pos = XDebuggerUtil.getInstance().createPosition(classFile, frame.lineNumber) ?: return false
                        context.debugProcess?.managerThread?.schedule(object : DebuggerCommandImpl() {
                            override fun action() {
                                view.buildTreeAndRestoreState(pos, context, descriptor)
                            }
                        })
                        return true
                    }
                    is StackFrameDescriptor -> {
                        GotoFrameSourceAction.doAction(dataContext)
                        return true
                    }
                    else -> return true
                }

            }
        }
        listener.installOn(tree)

        val disposable = { listener.uninstall(tree) }
        DebuggerUIUtil.registerActionOnComponent(actionName, tree, disposable)

        return disposable
    }

    fun XWatchesViewImpl.buildTreeAndRestoreState(
        pos: XSourcePosition,
        context: DebuggerContextImpl,
        descriptor: CoroutinesDebuggerTree.SuspendStackFrameDescriptor
    ) {
        tree.sourcePosition = pos
        createNewRootNode(context, descriptor)
    }

    private fun XWatchesViewImpl.createNewRootNode(
        context: DebuggerContextImpl,
        descriptor: CoroutinesDebuggerTree.SuspendStackFrameDescriptor
    ): XDebuggerTreeNode {
        return object : XValueContainerNode<XValueContainer>(tree, null, false, object : XValueContainer() {}) {}.apply {
            this@createNewRootNode.tree.setRoot(this, false)
            val execContext = context.createExecutionContext() ?: return@apply
            val debugMetadataKtType = execContext
                .findClass("kotlin.coroutines.jvm.internal.DebugMetadataKt") as ClassType
            val continuation = getContinuation(descriptor.state, descriptor.frame, execContext)
            val vars = getSpilledVariables(
                continuation ?: return@apply,
                debugMetadataKtType, execContext
            )
            val children = XValueChildrenList()
            vars?.forEach {
                children.add(it)
            }
            this.addChildren(children, true)
        }
    }

    /**
     * Find continuation for the [frame]
     * Gets current CoroutineInfo.lastObservedFrame and finds next frames in it until null or needed frame is found
     * @return null if matching continuation is not found or is not BaseContinuationImpl
     */
    private fun getContinuation(state: CoroutineState, frame: StackTraceElement, context: ExecutionContext): ObjectReference? {
        var continuation: ObjectReference? = state.frame ?: return null
        val baseType = "kotlin.coroutines.jvm.internal.BaseContinuationImpl"
        val getTrace = (continuation!!.type() as ClassType).concreteMethodByName(
            "getStackTraceElement",
            "()Ljava/lang/StackTraceElement;"
        )
        val stackTraceType = context.findClass("java.lang.StackTraceElement") as ClassType
        val getClassName = stackTraceType.concreteMethodByName("getClassName", "()Ljava/lang/String;")
        val getLineNumber = stackTraceType.concreteMethodByName("getLineNumber", "()I")
        val className = {
            val trace = context.invokeMethod(continuation!!, getTrace, emptyList()) as ObjectReference
            (context.invokeMethod(trace, getClassName, emptyList()) as StringReference).value()
        }
        val lineNumber = {
            val trace = context.invokeMethod(continuation!!, getTrace, emptyList()) as ObjectReference
            (context.invokeMethod(trace, getLineNumber, emptyList()) as IntegerValue).value()
        }

        while (continuation != null &&
            continuation.type().isSubtype(baseType)
            && (frame.className != className() || frame.lineNumber != lineNumber()) // continuation frame equals to the current
        ) {
            continuation = state.getNextFrame(continuation, context)
        }
        return if (continuation != null && continuation.type().isSubtype(baseType)) continuation else null
    }

    private fun getSpilledVariables(
        continuation: ObjectReference,
        debugMetadataKtType: ClassType,
        context: ExecutionContext
    ): List<XNamedValue>? {
        val getSpilledVariableFieldMappingMethod = debugMetadataKtType.methodsByName(
            "getSpilledVariableFieldMapping",
            "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)[Ljava/lang/String;"
        ).firstOrNull() ?: return null

        val args = listOf(continuation)

        val rawSpilledVariables = context.invokeMethod(
            debugMetadataKtType,
            getSpilledVariableFieldMappingMethod, args
        ) as? ArrayReference ?: return null

        val length = rawSpilledVariables.length() / 2
        val spilledVariables = ArrayList<XNamedValue>(length)

        for (index in 0 until length) {
            val fieldName = (rawSpilledVariables.getValue(2 * index) as? StringReference)?.value() ?: continue
            val variableName = (rawSpilledVariables.getValue(2 * index + 1) as? StringReference)?.value() ?: continue
            val field = continuation.referenceType().fieldByName(fieldName) ?: continue

            val valueDescriptor = object : ValueDescriptorImpl(context.project) {
                override fun calcValueName() = variableName
                override fun calcValue(evaluationContext: EvaluationContextImpl?) = continuation.getValue(field)
                override fun getDescriptorEvaluation(context: DebuggerContext?) =
                    throw EvaluateException("Spilled variable evaluation is not supported")
            }

            spilledVariables += JavaValue.create(
                null,
                valueDescriptor,
                context.evaluationContext,
                context.debugProcess.xdebugProcess!!.nodeManager,
                false
            )
        }

        return spilledVariables
    }


    private fun startLabelsUpdate() {
        if (myUpdateLabelsAlarm.isDisposed) return
        myUpdateLabelsAlarm.cancelAllRequests()
        myUpdateLabelsAlarm.addRequest(object : Runnable {
            override fun run() {
                var updateScheduled = false
                try {
                    if (isUpdateEnabled) {
                        val tree = getCoroutinesTree()
                        val root = tree.model.root as DebuggerTreeNodeImpl
                        val process = context.debugProcess
                        if (process != null) {
                            process.managerThread.schedule(object : DebuggerCommandImpl() {
                                override fun action() {
                                    try {
                                        updateNodeLabels(root)
                                    } finally {
                                        reschedule()
                                    }
                                }

                                override fun commandCancelled() {
                                    reschedule()
                                }
                            })
                            updateScheduled = true
                        }
                    }
                } finally {
                    if (!updateScheduled) {
                        reschedule()
                    }
                }
            }

            private fun reschedule() {
                val session = context.debuggerSession
                if (session != null && session.isAttached && !session.isPaused && !myUpdateLabelsAlarm.isDisposed) {
                    myUpdateLabelsAlarm.addRequest(this, LABELS_UPDATE_DELAY_MS, ModalityState.NON_MODAL)
                }
            }

        }, LABELS_UPDATE_DELAY_MS, ModalityState.NON_MODAL)
    }

    override fun dispose() {
        Disposer.dispose(myUpdateLabelsAlarm)
        super.dispose()
    }

    private fun updateNodeLabels(from: DebuggerTreeNodeImpl) {
        val children = from.children()
        try {
            while (children.hasMoreElements()) {
                val child = children.nextElement() as DebuggerTreeNodeImpl
                child.descriptor.updateRepresentation(
                    null
                ) { child.labelChanged() }
                updateNodeLabels(child)
            }
        } catch (ignored: NoSuchElementException) { // children have changed - just skip
        }

    }

    override fun createTreeView(): DebuggerTree {
        return CoroutinesDebuggerTree(project)
    }

    // TODO
    override fun createPopupMenu(): ActionPopupMenu {
        val group = ActionManager.getInstance().getAction(DebuggerActions.THREADS_PANEL_POPUP) as DefaultActionGroup
        return ActionManager.getInstance().createActionPopupMenu(DebuggerActions.THREADS_PANEL_POPUP, group)
    }

    override fun getData(dataId: String): Any? {
        return if (PlatformDataKeys.HELP_ID.`is`(dataId)) {
            HELP_ID
        } else super.getData(dataId)
    }

    fun getCoroutinesTree(): CoroutinesDebuggerTree = tree as CoroutinesDebuggerTree

    companion object {
        @NonNls
        private val HELP_ID = "debugging.debugCoroutines"
        private const val LABELS_UPDATE_DELAY_MS = 200
    }

}