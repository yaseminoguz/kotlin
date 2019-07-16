/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol

interface IrScript : IrSymbolDeclaration<IrScriptSymbol>, IrDeclarationContainer, IrDeclaration, IrDeclarationParent {
    val statements: MutableList<IrStatement>
    var thisReceiver: IrValueParameter
}
