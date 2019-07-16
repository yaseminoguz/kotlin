/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val removeReceiverLowering = makeIrModulePhase(
    ::RemoveReceiverLowering,
    name = "RemoveReceiver",
    description = "Remove receivers"
)

private class RemoveReceiverLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.declarations.transformFlat {
            if (it is IrScript) {
                lower(it)
            } else null
        }
    }

    fun lower(script: IrScript): List<IrScript> {
        val transformer: IrElementTransformerVoid = object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (script.declarations.any { it == expression.symbol.owner || it is IrProperty && it.getter === expression.symbol.owner }) {
                    expression.dispatchReceiver = null
                } else {
                    println()
                }
                return super.visitCall(expression)
            }

            override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
                if (script.declarations.any { it.descriptor == expression.symbol.owner.descriptor }) {
                    expression.receiver = null
                } else {
                    println()
                }
                return super.visitFieldAccess(expression)
            }
        }

        script.transformChildrenVoid(transformer)

        script.declarations.forEach {
            when (it) {
                is IrSimpleFunction -> it.dispatchReceiverParameter = null
                is IrProperty -> {
                    it.getter?.dispatchReceiverParameter = null
                    it.setter?.dispatchReceiverParameter = null
                }
            }
        }

        return listOf(script)
    }
}
