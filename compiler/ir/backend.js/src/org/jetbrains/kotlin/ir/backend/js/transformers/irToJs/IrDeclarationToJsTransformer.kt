/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import com.sun.tools.javac.util.Assert
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrDeclarationToJsTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: JsGenerationContext): JsStatement {
        require(!declaration.descriptor.isExpect)
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitConstructor(declaration: IrConstructor, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitClass(declaration: IrClass, context: JsGenerationContext): JsStatement {
        return JsClassGenerator(
            declaration,
            context.newDeclaration()
        ).generate()
    }

    override fun visitField(declaration: IrField, context: JsGenerationContext): JsStatement {
        val fieldName = context.getNameForField(declaration)

        if (declaration.isExternal) return JsEmpty

        if (declaration.initializer != null) {
            val initializer = declaration.initializer!!.accept(IrElementToJsExpressionTransformer(), context)
            context.staticContext.initializerBlock.statements += jsAssignment(fieldName.makeRef(), initializer).makeStmt()
        }

        return JsVars(JsVars.JsVar(fieldName))
    }

    override fun visitVariable(declaration: IrVariable, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrElementToJsStatementTransformer(), context)
    }

    override fun visitScript(irScript: IrScript, context: JsGenerationContext): JsStatement {
        val declarations: MutableList<JsStatement> = mutableListOf()
        val transformer = IrDeclarationToJsTransformer()
        for (d in irScript.declarations) {
            declarations += d.accept(transformer, context)
        }
        declarations += createEvaluateFunction(irScript.statements, context)

        return JsBlock(declarations)
    }

    companion object {
        var evalFunc: JsFunction? = null
        private var evaluateScriptFunctionCreated: Boolean = false

        fun getEvaluateScriptFunction(): JsFunction {
            Assert.check(evaluateScriptFunctionCreated)
            return evalFunc!!
        }
    }

    private fun createEvaluateFunction(statementssss: MutableList<IrStatement>, context: JsGenerationContext): JsStatement {
        val statements: MutableList<JsStatement> = mutableListOf()

        val tr = IrElementToJsStatementTransformer()
        for (s in statementssss) {
            statements += s.accept(tr, context)
        }

        evalFunc = JsFunction(
            emptyScope,
            JsBlock(statements),
            "Evaluate script function"
        )
            .also {
                it.name = JsName("evaluateScript", false)

                if (it.body.statements.isNotEmpty()) {
                    val returnExpression = JsReturn((it.body.statements.last() as JsExpressionStatement).expression)
                    it.body.statements[it.body.statements.size - 1] = returnExpression
                }
            }

        evaluateScriptFunctionCreated = true
        return evalFunc!!.makeStmt()
    }
}
