/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

object ThrowNPE : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        return IrIntrinsicFunction.createWithResult(expression, signature, context) {
            // Note that to support the new behavior of null checks in 1.4 (KT-22275) we could simply change this to "throwJavaNpe".
            // However, it would be preferable to refactor the null check generation in the IR backend in a way that would allow us
            // to call "Intrinsics.checkNotNull" instead, as in the old backend. That way, the bytecode will be shorter and we'll
            // avoid dependency on another method from stdlib (which will allow us to refactor it if needed in the future).
            it.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "throwNpe", "()V", false)
            Type.VOID_TYPE
        }
    }
}
