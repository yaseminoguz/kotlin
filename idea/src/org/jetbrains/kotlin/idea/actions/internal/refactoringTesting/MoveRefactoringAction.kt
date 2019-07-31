/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.ide.actions.OpenProjectFileChooserDescriptor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
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
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases.FileSystemChangesTracker
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases.MoveRefactoringCase
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val WINDOW_TITLE: String = "Move refactoring testing"

class MoveRefactoringAction : AnAction() {

    private val nestedRefactoring = MoveRefactoringCase()

    private var iteration = 0

    private var fails = 0

    private fun randomMoveAndCheck(
        project: Project,
        projectRoot: VirtualFile,
        indicator: ProgressIndicator,
        actionRunner: CompilationStatusTracker,
        fileTracker: FileSystemChangesTracker,
        resultsFile: File
    ) {

        iteration++
        try {
            indicator.text = "$WINDOW_TITLE [Try $iteration with $fails fails]"

            indicator.text2 = "Update indices..."
            indicator.fraction = 0.0

            DumbService.getInstance(project).waitForSmartMode()

            indicator.text2 = "Perform refactoring ..."
            indicator.fraction = 0.1

            fileTracker.reset()

            var refactoringResult: RandomMoveRefactoringResult = RandomMoveRefactoringResult.Failed
            edtExecute {
                refactoringResult = nestedRefactoring.tryCreateAndRun(project)
                indicator.text2 = "Saving files..."
                indicator.fraction = 0.3
                FileDocumentManager.getInstance().saveAllDocuments()
                VfsUtil.markDirtyAndRefresh(false, true, true, projectRoot)
            }

            when (val localRefactoringResult = refactoringResult) {
                is RandomMoveRefactoringResult.Success -> {
                    indicator.text2 = "Compiling project..."
                    indicator.fraction = 0.7
                    if (!actionRunner.checkByBuild()) {
                        fails++
                        resultsFile.appendText("${localRefactoringResult.caseData}\n\n")
                    }
                }
                is RandomMoveRefactoringResult.ExceptionCaused -> {
                    fails++
                    resultsFile.appendText("${localRefactoringResult.caseData}\nWith exception\n${localRefactoringResult.message}\n\n")
                }
                is RandomMoveRefactoringResult.Failed -> { }
            }

        } finally {
            indicator.text2 = "Reset files..."
            indicator.fraction = 0.9

            fileTracker.createdFiles.toList().map {
                if (it.exists()) it.delete(null)
            }

            gitReset(project, projectRoot)
        }

        indicator.text2 = "Done"
        indicator.fraction = 1.0
    }

    private fun createFileIfNotExist(targetPath: String): File? {

        val stamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd-HH-mm-ss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
            .toString()

        val resultsFile = File(targetPath, "REFACTORING_TEST_RESULT-$stamp.txt")
        return try {
            resultsFile.createNewFile()
            resultsFile
        } catch (e: IOException) {
            null
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val projectRoot = project?.guessProjectDir()

        if (projectRoot === null) return

        val targetPath = OpenProjectFileChooserDescriptor(true).let { descriptor ->
            descriptor.title = "Select test result directory path"
            FileChooser.chooseFiles(descriptor, project, null).firstOrNull()?.path
        }
        if (targetPath === null) {
            return
        }

        val resultsFile = createFileIfNotExist(targetPath)
        if (resultsFile === null) {
            Messages.showMessageDialog(project, "Cannot get or create results file", WINDOW_TITLE, null)
            return
        }

        ProgressManager.getInstance().run(object : Task.Modal(project, WINDOW_TITLE, true) {
            override fun run(indicator: ProgressIndicator) {
                while (!indicator.isCanceled) {
                    iteration++
                    randomMoveAndCheck(
                        project,
                        projectRoot,
                        indicator,
                        CompilationStatusTracker(project),
                        FileSystemChangesTracker(project),
                        resultsFile
                    )
                }
            }
        })
    }
}
