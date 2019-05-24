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

class RuntimeChecksInsertion(val context: JsIrBackendContext) : FileLoweringPass {
    var count: Int = 0

    fun IrAnnotationContainer.prohibitsRuntimeTypeChecks(): Boolean =
        when {
            hasAnnotation(context.intrinsics.doNotIntrinsifyAnnotationSymbol) -> true
            hasAnnotation(context.intrinsics.skipRuntimeTypeChecksAnnotationSymbol) -> true
            else -> false
        }

    override fun lower(irFile: IrFile) {
        if (irFile.prohibitsRuntimeTypeChecks()) return

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                if (declaration.prohibitsRuntimeTypeChecks())
                    return declaration
                return super.visitDeclaration(declaration)
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                if (declaration.parentAsClass.isInline)
                    return declaration
                return super.visitFunction(declaration)
            }

            override fun visitExpression(expression: IrExpression): IrExpression {
                return insertRuntimeChecks(super.visitExpression(expression))
            }
        })

        irFile.patchDeclarationParents()
    }

    private fun insertRuntimeChecks(expression: IrExpression): IrExpression {
        val type = expression.type
        val typeSymbol = type.classifierOrNull ?: return expression

        val typeClass = typeSymbol.owner as? IrClass ?: return expression
        val typeName = typeClass.name.asString()
        if (typeName.startsWith("KMutableProperty") ||
            typeName.startsWith("KProperty") ||
            typeName.startsWith("SuspendFunction") ||
            typeName.startsWith("KSuspendFunction")
        )
            return expression

        if (type.isNothing())
            return expression

        if (typeSymbol == context.continuationClass)
            return expression

        if (typeClass.isEffectivelyExternal())
            return expression

        if (expression is IrDynamicExpression)
            return expression

        // These are not really expressions. They can't be used in any context.
        if (expression is IrEnumConstructorCall ||
            expression is IrDelegatingConstructorCall ||
            expression is IrInstanceInitializerCall
        )
            return expression

        // Some intrinsics rely on arguments being string constants:
        //    - kotlin.js.js
        //    - jsGetJSField
        //    - jsSetJSField
        if (expression is IrConst<*> && expression.kind == IrConstKind.String)
            return expression

        // Null constants used in EQEQ functions can change its semantics
        if (expression.isNullConst())
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

