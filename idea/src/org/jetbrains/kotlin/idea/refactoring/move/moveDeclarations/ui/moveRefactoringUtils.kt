/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

internal fun getTargetPackageFqName(targetContainer: PsiElement): FqName? {
    if (targetContainer is PsiDirectory) {
        val targetPackage = targetContainer.getPackage()
        return if (targetPackage != null) FqName(targetPackage.qualifiedName) else null
    }
    return if (targetContainer is KtFile) targetContainer.packageFqName else null
}
