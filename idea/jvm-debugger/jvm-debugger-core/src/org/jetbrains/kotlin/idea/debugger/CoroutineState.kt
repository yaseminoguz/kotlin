/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.sun.jdi.ClassType
import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

/**
 * Represents state of a coroutine.
 * @see `kotlinx.coroutines.debug.CoroutineInfo`
 */
class CoroutineState(
    val name: String,
    val state: String,
    val thread: ThreadReference? = null,
    val stackTrace: List<StackTraceElement>,
    val frame: ObjectReference?
) {
    val isSuspended: Boolean = state == "SUSPENDED"
    val isEmptyStackTrace: Boolean by lazy { stackTrace.isEmpty() }
    val stringStackTrace: String by lazy {
        buildString {
            stackTrace.forEach {
                appendln(it)
            }
        }
    }

    /**
     * Finds previous Continuation for this Continuation (completion field in BaseContinuationImpl)
     * @return null if given ObjectReference is not a BaseContinuationImpl instance or completion is null
     */
    fun getNextFrame(continuation: ObjectReference, context: ExecutionContext): ObjectReference? {
        val type = continuation.type() as ClassType
        if (!type.isSubtype("kotlin.coroutines.jvm.internal.BaseContinuationImpl")) return null
        val next = type.concreteMethodByName("getCompletion", "()Lkotlin/coroutines/Continuation;")
        return context.invokeMethod(continuation, next, emptyList()) as? ObjectReference
    }
}