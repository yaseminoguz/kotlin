/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.execution.filters.ExceptionFilters
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.impl.RunnerContentUi
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.text.DateFormatUtil
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import com.sun.jdi.*
import com.sun.tools.jdi.StringReferenceImpl
import javaslang.control.Either
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.evaluate.createExecutionContext

class CoroutineDumpAction : AnAction(), AnAction.TransparentUpdate {
    private val logger = Logger.getInstance(this::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val session = context.debuggerSession
        if (session != null && session.isAttached) {
            val process = context.debugProcess ?: return
            process.managerThread.schedule(object : DebuggerCommandImpl() {
                override fun action() {
                    val execContext = context.createExecutionContext() ?: return
                    val states = buildCoroutineStates(execContext)
                    if (states.isLeft) {
                        logger.error(states.left)
                        XDebuggerManagerImpl.NOTIFICATION_GROUP
                            .createNotification(
                                "Coroutine dump failed. See log",
                                MessageType.ERROR
                            ).notify(project)
                        return
                    }
                    val f = fun() {
                        addCoroutineDump(
                            project,
                            states.get(),
                            session.xDebugSession?.ui ?: return,
                            session.searchScope
                        )
                    }
                    ApplicationManager.getApplication().invokeLater(f, ModalityState.NON_MODAL)
                }
            })
        }
    }

    companion object {
        private const val DEBUG_PACKAGE = "kotlinx.coroutines.debug"
        /**
         * Invokes DebugProbes from debugged process's classpath and returns states of coroutines
         * Should be invoked on debugger manager thread
         */
        fun buildCoroutineStates(context: ExecutionContext): Either<Throwable, List<CoroutineState>> {
            try {
                // kotlinx.coroutines.debug.DebugProbes instance and methods
                val debugProbes = context.findClass("$DEBUG_PACKAGE.DebugProbes") as ClassType
                val probesImplType = context.findClass("$DEBUG_PACKAGE.internal.DebugProbesImpl") as ClassType
                val debugProbesImpl = with(probesImplType) { getValue(fieldByName("INSTANCE")) as ObjectReference }
                val enhanceStackTraceWithThreadDump =
                    probesImplType.methodsByName("enhanceStackTraceWithThreadDump").single()
                val dumpMethod = debugProbes.concreteMethodByName("dumpCoroutinesInfo", "()Ljava/util/List;")
                val instance = with(debugProbes) { getValue(fieldByName("INSTANCE")) as ObjectReference }

                // CoroutineInfo
                val info = context.findClass("$DEBUG_PACKAGE.CoroutineInfo") as ClassType
                val getState = info.methodsByName("getState").single()
                val getContext = info.methodsByName("getContext").single()
                val idField = info.fieldByName("sequenceNumber")
                val lastObservedStackTrace = info.methodsByName("lastObservedStackTrace").single()
                val coroutineContext = context.findClass("kotlin.coroutines.CoroutineContext") as InterfaceType
                val getContextElement = coroutineContext.methodsByName("get").single()
                val coroutineName = context.findClass("kotlinx.coroutines.CoroutineName") as ClassType
                val getName = coroutineName.methodsByName("getName").single()
                val nameKey = coroutineName.getValue(coroutineName.fieldByName("Key")) as ObjectReference
                val toString = (context.findClass("java.lang.Object") as ClassType)
                    .methodsByName("toString").single()

                val threadRef = info.fieldByName("lastObservedThread")
                val continuation = info.fieldByName("lastObservedFrame")

                // get dump
                val infoList = context.invokeMethod(instance, dumpMethod, emptyList()) as ObjectReference

                // Methods for list
                val listType = context.findClass("java.util.List") as InterfaceType
                val getSize = listType.methodsByName("size").single()
                val getElement = listType.methodsByName("get").single()
                val size = (context.invokeMethod(infoList, getSize, emptyList()) as IntegerValue).value()
                val element = context.findClass("java.lang.StackTraceElement") as ClassType

                return Either.right(List(size) {
                    val index = context.vm.mirrorOf(it)
                    val elem = context.invokeMethod(infoList, getElement, listOf(index)) as ObjectReference
                    val name = getName(context, elem, getContext, getContextElement, nameKey, getName, idField)
                    val state = getState(context, elem, getState, toString)
                    val thread = getLastObservedThread(elem, threadRef)
                    CoroutineState(
                        name, state, thread, getStackTrace(
                            elem,
                            lastObservedStackTrace,
                            getSize,
                            getElement,
                            debugProbesImpl,
                            enhanceStackTraceWithThreadDump,
                            element,
                            context
                        ), elem.getValue(continuation) as? ObjectReference
                    )
                })
            } catch (e: Throwable) {
                return Either.left(e)
            }
        }

        private fun getName(
            context: ExecutionContext, // Execution context to invoke methods
            info: ObjectReference, // CoroutineInfo instance
            getContext: Method, // CoroutineInfo.getContext()
            getContextElement: Method, // CoroutineContext.get(Key)
            nameKey: ObjectReference, // CoroutineName companion object
            getName: Method, // CoroutineName.getName()
            idField: Field // CoroutineId.idField()
        ): String {
            // equals to `coroutineInfo.context.get(CoroutineName).name`
            val coroutineContextInst = context.invokeMethod(info, getContext, emptyList()) as ObjectReference
            val coroutineName = context.invokeMethod(
                coroutineContextInst,
                getContextElement, listOf(nameKey)
            ) as? ObjectReference
            // If the coroutine doesn't have a given name, CoroutineContext.get(CoroutineName) returns null
            val name = if (coroutineName != null) (context.invokeMethod(
                coroutineName,
                getName, emptyList()
            ) as StringReferenceImpl).value() else "coroutine"
            val id = (info.getValue(idField) as LongValue).value()
            return "$name#$id"
        }

        private fun getState(
            context: ExecutionContext, // Execution context to invoke methods
            info: ObjectReference, // CoroutineInfo instance
            getState: Method, // CoroutineInfo.state field
            toString: Method // CoroutineInfo.State.toString()
        ): String {
            //  equals to stringState = coroutineInfo.state.toString()
            val state = context.invokeMethod(info, getState, emptyList()) as ObjectReference
            return (context.invokeMethod(state, toString, emptyList()) as StringReferenceImpl).value()
        }

        private fun getLastObservedThread(
            info: ObjectReference, // CoroutineInfo instance
            threadRef: Field // reference to lastObservedThread
        ): ThreadReference? = info.getValue(threadRef) as ThreadReference?

        /**
         * Returns list of stackTraceElements for the given CoroutineInfo's [ObjectReference]
         */
        private fun getStackTrace(
            info: ObjectReference,
            lastObservedStackTrace: Method,
            getSize: Method,
            getElement: Method,
            debugProbesImpl: ObjectReference,
            enhanceStackTraceWithThreadDump: Method,
            element: ClassType,
            context: ExecutionContext
        ): List<StackTraceElement> {
            val frameList = context.invokeMethod(info, lastObservedStackTrace, emptyList()) as ObjectReference
            val mergedFrameList = context.invokeMethod(
                debugProbesImpl,
                enhanceStackTraceWithThreadDump, listOf(info, frameList)
            ) as ObjectReference
            val size = (context.invokeMethod(mergedFrameList, getSize, emptyList()) as IntegerValue).value()
            val methodName = element.fieldByName("methodName")
            val className = element.fieldByName("declaringClass")
            val fileName = element.fieldByName("fileName")
            val line = element.fieldByName("lineNumber")

            val list = ArrayList<StackTraceElement>()
            for (it in size - 1 downTo 0) {
                val frame = context.invokeMethod(
                    mergedFrameList, getElement,
                    listOf(context.vm.virtualMachine.mirrorOf(it))
                ) as ObjectReference
                val clazz = (frame.getValue(className) as StringReference).value()

                if (clazz.contains(DEBUG_PACKAGE)) break // cut off debug intrinsic stacktrace
                list.add(
                    0, // add in the beginning
                    StackTraceElement(
                        clazz,
                        (frame.getValue(methodName) as StringReference).value(),
                        (frame.getValue(fileName) as StringReference?)?.value(),
                        (frame.getValue(line) as IntegerValue).value()
                    )
                )
            }
            return list
        }
    }

    /**
     * Analog of [DebuggerUtilsEx.addThreadDump].
     */
    fun addCoroutineDump(project: Project, coroutines: List<CoroutineState>, ui: RunnerLayoutUi, searchScope: GlobalSearchScope) {
        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        consoleBuilder.filters(ExceptionFilters.getFilters(searchScope))
        val consoleView = consoleBuilder.console
        val toolbarActions = DefaultActionGroup()
        consoleView.allowHeavyFilters()
        val panel = CoroutineDumpPanel(project, consoleView, toolbarActions, coroutines)

        val id = "DumpKt " + DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis())
        val content = ui.createContent(id, panel, id, null, null).apply {
            putUserData(RunnerContentUi.LIGHTWEIGHT_CONTENT_MARKER, true)
            isCloseable = true
            description = "Coroutine Dump"
        }
        ui.addContent(content)
        ui.selectAndFocus(content, true, true)
        Disposer.register(content, consoleView)
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project
        if (project == null) {
            presentation.isEnabled = false
            return
        }
        // cannot be called when no SuspendContext
        if (DebuggerManagerEx.getInstanceEx(project).context.suspendContext == null) {
            presentation.isEnabled = false
            return
        }
        val debuggerSession = DebuggerManagerEx.getInstanceEx(project).context.debuggerSession
        presentation.isEnabled = debuggerSession != null && debuggerSession.isAttached
    }
}