/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.scriptCompilationConfiguration
import org.jetbrains.kotlin.idea.core.script.scriptDependencies
import org.jetbrains.kotlin.scripting.resolve.RefinementResults
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
class FromFileAttributeScriptDependenciesLoader(project: Project) : ScriptDependenciesLoader(project) {

    override fun isApplicable(file: VirtualFile): Boolean {
        return file.scriptDependencies != null
    }

    override fun loadDependencies(file: VirtualFile) {
        file.scriptCompilationConfiguration?.let {
            RefinementResults.FromRefinement(VirtualFileScriptSource(file), it).apply {
                debug(file) { "refined configuration from fileAttributes = $it" }
            }
        } ?: file.scriptDependencies?.let {
            RefinementResults.FromLegacy(VirtualFileScriptSource(file), it).apply {
                debug(file) { "dependencies from fileAttributes = $it" }
            }
        }?.let {
            saveToCache(file, it, skipSaveToAttributes = true)
        }
    }

    override fun shouldShowNotification(): Boolean = false
}