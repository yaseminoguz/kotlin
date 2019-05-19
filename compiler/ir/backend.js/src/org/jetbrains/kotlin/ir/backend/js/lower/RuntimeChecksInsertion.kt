/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
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
    private val calculator = JsIrArithBuilder(context)
    var count: Int = 0

    override fun lower(irFile: IrFile) {
        if (irFile.name.contains("typeCheckUtils.kt"))
            return

        if (irFile.name.contains("coroutineInternalJS.kt"))
            return

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(context.intrinsics.doNotIntrinsifyAnnotationSymbol))
                    return declaration

                if (declaration is IrSimpleFunction && declaration.isTailrec) {
                    // IrJsCodegenBoxTestGenerated.Coroutines.FeatureIntersection.Tailrec#testSum_1_3
                    // IrJsCodegenBoxTestGenerated.Diagnostics.Functions.TailRecursion#testSum
                    return declaration
                }


                // Some call stack problems
                if (declaration is IrConstructor)
                    return declaration

                return super.visitFunction(declaration)
            }

            override fun visitExpression(expression: IrExpression): IrExpression {
                if (expression is IrDynamicMemberExpression) {
                    return expression
                }

                if (expression is IrWhen || expression is IrBranch) {
                    return expression
                }

                if (expression is IrDynamicOperatorExpression) {
                    if (expression.operator.isAssignmentOperator) {
                        return expression
                    }
                    if (expression.operator == IrDynamicOperator.INVOKE) {
                        return expression
                    }
                }

                if (expression is IrCall) {
                    // EQEQ
                    if (expression.symbol.owner.getPackageFragment()?.fqName == FqName("kotlin.internal.ir")) {
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

        // Some boolean operators return numbers (^)
        if (type.isBoolean())
            return expression

        if (typeSymbol == context.continuationClass)
            return expression

        if (typeClass.isEffectivelyExternal())
            return expression

        if (type.isUnit()) return expression

        // Pointless. Check later
        if (type.isNullableAny()) return expression

        // For primitive companions
        if (typeClass.isCompanion) return expression

        // For IR intrinsics        return;

        val parentFragment = typeClass.parent as? IrPackageFragment
        if (parentFragment != null && parentFragment.fqName == FqName("kotlin.internal.ir"))
            return expression

        // For js function
        if (type.isString()) return expression

//        val typeCheck: IrExpression =
//            JsIrBuilder.buildString(context.irBuiltIns.stringType, "type_check")

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

