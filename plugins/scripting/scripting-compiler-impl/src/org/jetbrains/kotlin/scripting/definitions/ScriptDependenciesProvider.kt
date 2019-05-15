/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.scripting.resolve.RefinementResults
import kotlin.script.experimental.dependencies.ScriptDependencies

interface ScriptDependenciesProvider {

    @Deprecated("Migrating to configuration refinement")
    fun getScriptDependencies(file: VirtualFile): ScriptDependencies? = getRefinementResults(file)?.scriptDependencies

    @Deprecated("Migrating to configuration refinement")
    fun getScriptDependencies(file: PsiFile) = getScriptDependencies(file.virtualFile ?: file.originalFile.virtualFile)

    fun getRefinementResults(file: VirtualFile): RefinementResults? = null

    fun getRefinementResults(file: PsiFile) = getRefinementResults(file.virtualFile ?: file.originalFile.virtualFile)

    companion object {
        fun getInstance(project: Project): ScriptDependenciesProvider? =
            ServiceManager.getService(project, ScriptDependenciesProvider::class.java)
    }
}
