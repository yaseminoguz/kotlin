/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases;

import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.*
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.psi.*

internal class NestedClassMoveRefactoringCase : RandomMoveRefactoringCase {

    private fun KtDeclaration.isSourceCompatible() = this is KtClass && this !is KtEnumEntry && this.parent is KtClassBody

    private fun KtDeclaration.isTargetCompatible() = this is KtClass && this !is KtEnumEntry

    override fun tryCreateAndRun(project: Project): RandomMoveRefactoringResult {

        val scope = KotlinSourceFilterScope.projectSources(ProjectScope.getContentScope(project), project)
        val ktFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope).toList()

        val klassElements = ktFiles
            .toRandomSequence()
            .flatMap { PsiTreeUtil.collectElementsOfType(it.toPsiFile(project), KtClass::class.java).toList().toRandomSequence() }
            .take(1000)

        var targetElement: KtNamedDeclaration
        var sourceElement: KtNamedDeclaration

        val handler by lazy { MoveKotlinDeclarationsHandler() }

        while (true) {
            targetElement = klassElements
                .firstOrNull { it.isTargetCompatible() }
                ?: return RandomMoveRefactoringResult.Failed.Instance
            sourceElement = klassElements
                .firstOrNull { it !== targetElement && it.isSourceCompatible() }
                ?: return RandomMoveRefactoringResult.Failed.Instance

            val sourceAsArray = arrayOf(sourceElement)
            if (handler.isValidTarget(targetElement, sourceAsArray) && handler.canMove(sourceAsArray, targetElement)) {
                break
            }
        }

        val targetMoveElement = KotlinMoveTargetForExistingElement(targetElement as KtClassOrObject)
        val delegate = MoveDeclarationsDelegate.NestedClass()
        val descriptor = MoveDeclarationsDescriptor(
            project,
            MoveSource(sourceElement),
            targetMoveElement,
            delegate,
            searchInCommentsAndStrings = false,
            searchInNonCode = false,
            deleteSourceFiles = false,
            moveCallback = null,
            openInEditor = false
        )

        MoveKotlinDeclarationsProcessor(descriptor, Mover.Default).run()

        return RandomMoveRefactoringResult.Success("Source: ${sourceElement.name}\nTarget: ${targetElement.name}")
    }
}
