/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName

class RuntimeChecksInsertion(val context: JsIrBackendContext) : FileLoweringPass {
    var count: Int = 0

    override fun lower(irFile: IrFile) {
        if (irFile.name.contains("typeCheckUtils.kt"))
            return

        if (irFile.name.contains("coroutineInternalJS.kt"))
            return

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                val parent = declaration.parent
                if (declaration.hasAnnotation(context.intrinsics.doNotIntrinsifyAnnotationSymbol))
                    return declaration
                if (declaration.symbol == context.intrinsics.typeCheckIntrinsic)
                    return declaration
                if (declaration is IrConstructor && parent is IrClass && parent.isInline)
                    return declaration

                return super.visitFunction(declaration)
            }

            override fun visitExpression(expression: IrExpression): IrExpression {
                // Null constants used in EQEQ functions can change its semantics
                if (expression.isNullConst())
                    return expression

                if (expression is IrCall) {
                    val function: IrFunction = expression.symbol.owner
                    val fqName = function.fqNameWhenAvailable

                    // String literal of 'js' function cannot be separated
                    if (fqName == FqName("kotlin.js.js")) {
                        return expression
                    }
                }

                val newExpression = super.visitExpression(expression)
                return insertRuntimeChecks(newExpression)
            }
        })

        irFile.patchDeclarationParents()
    }

    private fun insertRuntimeChecks(expression: IrExpression): IrExpression {
        val type = expression.type
        val typeSymbol = type.classifierOrNull ?: return expression

        val typeClass = typeSymbol.owner as? IrClass ?: return expression

        if (typeClass.name.asString().startsWith("KMutableProperty"))
            return expression

        if (typeClass.name.asString().startsWith("KProperty"))
            return expression

        if (typeClass.name.asString().startsWith("SuspendFunction"))
            return expression

        if (typeClass.name.asString().startsWith("KSuspendFunction"))
            return expression

        if (type.isNothing())
            return expression

        if (typeSymbol == context.continuationClass)
            return expression

        if (typeClass.isEffectivelyExternal())
            return expression

        if (expression is IrDynamicExpression)
            return expression

        // Enum constructor calls pretend to return Unit.
        if (expression is IrEnumConstructorCall ||
            expression is IrDelegatingConstructorCall ||
            expression is IrInstanceInitializerCall
        )
            return expression

        // For primitive companions
        if (typeClass.isCompanion) return expression

        if (expression.canHaveSideEffects()) {
            val tmp = JsIrBuilder.buildVar(type, null, name = "tc$$count", initializer = expression)
            count++

            val condition = JsIrBuilder.buildTypeOperator(
                type = context.irBuiltIns.booleanType,
                argument = JsIrBuilder.buildGetValue(tmp.symbol),
                operator = IrTypeOperator.INSTANCEOF,
                symbol = typeSymbol,
                toType = type
            )
            val typeCheck = JsIrBuilder.buildCall(context.intrinsics.typeCheckIntrinsic).apply {
                putValueArgument(0, condition)
            }
            val getTmp = JsIrBuilder.buildGetValue(tmp.symbol)

            return IrCompositeImpl(
                expression.startOffset,
                expression.endOffset,
                origin = null,
                statements = listOf(tmp, typeCheck, getTmp),
                type = expression.type
            )
        } else {
            val condition = JsIrBuilder.buildTypeOperator(
                type = context.irBuiltIns.booleanType,
                argument = expression.deepCopyWithVariables(),
                operator = IrTypeOperator.INSTANCEOF,
                symbol = typeSymbol,
                toType = type
            )
            val typeCheck = JsIrBuilder.buildCall(context.intrinsics.typeCheckIntrinsic).apply {
                putValueArgument(0, condition)
            }
            return IrCompositeImpl(
                expression.startOffset,
                expression.endOffset,
                origin = null,
                statements = listOf(typeCheck, expression),
                type = expression.type
            )
        }
    }
}

private fun IrExpression.canHaveSideEffects(): Boolean =
    when (this) {
        is IrGetValue -> false
        else -> true
    }

