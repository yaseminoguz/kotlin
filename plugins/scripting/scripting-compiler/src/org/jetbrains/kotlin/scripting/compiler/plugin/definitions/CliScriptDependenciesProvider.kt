/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.jvm.compat.mapToLegacyReports

class CliScriptDependenciesProvider(private val project: Project) : ScriptDependenciesProvider {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, ScriptCompilationConfigurationWrapper?>()

    override fun getScriptRefinedCompilationConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? = cacheLock.read {
        calculateRefinedConfiguration(file)
    }

    private fun calculateRefinedConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? {
        val path = file.path
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = file.findScriptDefinition(project)
            if (scriptDef != null) {
                val result = refineScriptCompilationConfiguration(VirtualFileScriptSource(file), scriptDef, project)

                ServiceManager.getService(project, ScriptReportSink::class.java)?.attachReports(file, result.reports.mapToLegacyReports())

                if (result is ResultWithDiagnostics.Success<ScriptCompilationConfigurationWrapper>) {
                    log.info("[kts] new cached deps for $path: ${result.value.dependenciesClassPath.joinToString(File.pathSeparator)}")
                    cacheLock.write {
                        cache.put(path, result.value)
                    }
                    result.value
                } else null
            } else null
        }
    }
}

private val log = Logger.getInstance(ScriptDependenciesProvider::class.java)
