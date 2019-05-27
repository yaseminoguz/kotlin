/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class RuntimeChecksInsertion(val context: JsIrBackendContext) : FileLoweringPass {
    fun IrAnnotationContainer.prohibitsRuntimeTypeChecks(): Boolean =
        hasAnnotation(context.intrinsics.doNotIntrinsifyAnnotationSymbol) ||
                hasAnnotation(context.intrinsics.skipRuntimeTypeChecksAnnotationSymbol)

    override fun lower(irFile: IrFile) {
        if (irFile.prohibitsRuntimeTypeChecks()) return

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                if (declaration.prohibitsRuntimeTypeChecks())
                    return declaration
                return super.visitDeclaration(declaration)
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                // TODO: Fix types in inline class constructors
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

        // Pointless checks
        if (type.isNothing() || type.isUnit())
            return expression

        // "Primitive" arrays (BooleanArray, IntArray, etc.) are extensively unsafe-casted to Array in stdlib
        if (type.isArray())
            return expression

        // TODO: Investigate problems with continuation classes
        if (typeSymbol == context.continuationClass)
            return expression

        if (typeClass.isEffectivelyExternal() && typeClass.kind != ClassKind.CLASS)
            return expression

        if (expression is IrDynamicExpression)
            return expression

        // These are not really expressions. Their return values are not used.
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

        // TODO: Implement type checks for primitive companions
        if (typeClass.isCompanion) return expression

        return JsIrBuilder.buildTypeOperator(type, IrTypeOperator.CAST, expression, type, typeSymbol)
    }
}