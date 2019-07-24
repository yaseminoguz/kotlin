/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases.NestedClassMoveRefactoringCase

class MoveRefactoringAction : AnAction() {

    private val nestedRefactoring = NestedClassMoveRefactoringCase()

    private fun randomMoveAndCheck(
        project: Project,
        projectRoot: VirtualFile,
        indicator: ProgressIndicator,
        actionRunner: RunActionWithErrorCountCheck
    ): Boolean {

        try {
            var applied = false
            var caseText: String? = null

            indicator.text2 = "Update indices..."
            indicator.fraction = 0.0

            DumbService.getInstance(project).waitForSmartMode()

            indicator.text2 = "Perform refactoring..."
            indicator.fraction = 0.1

            edtExecute {
                try {
                    when (val refactoringResult = nestedRefactoring.tryCreateAndRun(project)) {
                        is RandomMoveRefactoringResult.Success -> {
                            applied = true
                            caseText = refactoringResult.caseData
                        }
                        is RandomMoveRefactoringResult.Failed -> applied = false
                    }
                } catch (e: Exception) {
                }
            }

            if (!applied) return false

            indicator.text2 = "Saving files..."
            indicator.fraction = 0.3


            edtExecute {
                FileDocumentManager.getInstance().saveAllDocuments()
            }

            VfsUtil.markDirtyAndRefresh(false, true, true, projectRoot)

            indicator.text2 = "Compiling project..."
            indicator.fraction = 0.7

            if (!actionRunner.checkByBuild()) {
                edtExecute {
                    Messages.showMessageDialog(project, caseText, "Found invalid refactoring", null)
                }
            }
        } finally {
            indicator.text2 = "Reset files..."
            indicator.fraction = 0.9
            gitReset(project, projectRoot)
        }

        indicator.text2 = "Done"
        indicator.fraction = 1.0

        return true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val projectRoot = project?.guessProjectDir()

        if (projectRoot === null) return

        ProgressManager.getInstance().run(object : Task.Modal(project, "Move refactoring testing", true) {
            override fun run(indicator: ProgressIndicator) {
                while (randomMoveAndCheck(project, projectRoot, indicator, RunActionWithErrorCountCheck(project))) {
                    if (indicator.isCanceled) return
                }

                edtExecute {
                    Messages.showMessageDialog(project, "Cannot apply any refactoring!", "Move refactoring test", null)
                }
            }
        })
    }
}
