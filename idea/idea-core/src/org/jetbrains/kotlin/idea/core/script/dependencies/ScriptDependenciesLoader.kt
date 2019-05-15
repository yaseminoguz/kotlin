/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.*
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.resultOrNull
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.jvm.compat.mapToLegacyReports

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
abstract class ScriptDependenciesLoader(protected val project: Project) {

    abstract fun isApplicable(file: VirtualFile): Boolean
    abstract fun loadDependencies(file: VirtualFile)

    protected abstract fun shouldShowNotification(): Boolean

    protected var shouldNotifyRootsChanged = false

    protected val contentLoader = ScriptContentLoader(project)
    protected val cache: ScriptDependenciesCache = ServiceManager.getService(project, ScriptDependenciesCache::class.java)

    private val reporter: ScriptReportSink = ServiceManager.getService(project, ScriptReportSink::class.java)

    @Deprecated("migrating to new configuration refinement", level = DeprecationLevel.ERROR)
    protected fun processResult(result: DependenciesResolver.ResolveResult, file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        debug(file) { "dependencies from ${this.javaClass} received = $result" }

        if (cache[file] == null) {
            saveDependencies(result, file, scriptDef)
            attachReportsIfChanged(result, file)
            return
        }

        val newDependencies = result.dependencies?.adjustByDefinition(scriptDef)
        if (cache[file] != newDependencies) {
            if (shouldShowNotification() && !ApplicationManager.getApplication().isUnitTestMode) {
                debug(file) {
                    "dependencies changed, notification is shown: old = ${cache[file]}, new = $newDependencies"
                }
                file.addScriptDependenciesNotificationPanel(result, project) {
                    saveDependencies(it, file, scriptDef)
                    attachReportsIfChanged(it, file)
                    submitMakeRootsChange()
                }
            } else {
                debug(file) {
                    "dependencies changed, new dependencies are applied automatically: old = ${cache[file]}, new = $newDependencies"
                }
                saveDependencies(result, file, scriptDef)
                attachReportsIfChanged(result, file)
            }
        } else {
            attachReportsIfChanged(result, file)

            if (shouldShowNotification()) {
                file.removeScriptDependenciesNotificationPanel(project)
            }
        }
    }

    protected fun processRefinedConfiguration(result: ResultWithDiagnostics<RefinementResults>, file: VirtualFile) {
        debug(file) { "refined script compilation configuration from ${this.javaClass} received = $result" }

        val refinementResult = result.resultOrNull()

        val oldResult = cache.getRefinementResults(file)

        if (oldResult == null) {
            refinementResult?.let {
                saveRefinementResults(it, file)
                attachReportsIfChanged(result, file)
            }
            return
        }

        if (oldResult != refinementResult) {
            if (shouldShowNotification() && !ApplicationManager.getApplication().isUnitTestMode) {
                debug(file) {
                    "dependencies changed, notification is shown: old = $oldResult, new = $refinementResult"
                }
                file.addScriptDependenciesNotificationPanel(refinementResult, project) {
                    saveRefinementResults(it, file)
                    attachReportsIfChanged(result, file)
                    submitMakeRootsChange()
                }
            } else {
                debug(file) {
                    "dependencies changed, new dependencies are applied automatically: old = $oldResult, new = $refinementResult"
                }
                saveRefinementResults(refinementResult, file)
                attachReportsIfChanged(result, file)
            }
        } else {
            attachReportsIfChanged(result, file)

            if (shouldShowNotification()) {
                file.removeScriptDependenciesNotificationPanel(project)
            }
        }
    }

    @Deprecated("migrating to new configuration refinement")
    private fun attachReportsIfChanged(result: DependenciesResolver.ResolveResult, file: VirtualFile) {
        if (file.getUserData(IdeScriptReportSink.Reports) != result.reports.takeIf { it.isNotEmpty() }) {
            reporter.attachReports(file, result.reports)
        }
    }

    private fun attachReportsIfChanged(result: ResultWithDiagnostics<*>, file: VirtualFile) {
        if (file.getUserData(IdeScriptReportSink.Reports) != result.reports.takeIf { it.isNotEmpty() }) {
            reporter.attachReports(file, result.reports.mapToLegacyReports())
        }
    }

    @Deprecated("migrating to new configuration refinement")
    private fun saveDependencies(result: DependenciesResolver.ResolveResult, file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        if (shouldShowNotification()) {
            file.removeScriptDependenciesNotificationPanel(project)
        }

        val dependencies = result.dependencies?.adjustByDefinition(scriptDef) ?: return
        saveToCache(file, dependencies)
    }

    private fun saveRefinementResults(refinementResult: RefinementResults?, file: VirtualFile) {
        if (shouldShowNotification()) {
            file.removeScriptDependenciesNotificationPanel(project)
        }
        if (refinementResult != null) {
            saveToCache(file, refinementResult)
        }
    }

    @Deprecated("migrating to new configuration refinement")
    protected fun saveToCache(file: VirtualFile, dependencies: ScriptDependencies) {
        val rootsChanged = cache.hasNotCachedRoots(dependencies)
        if (cache.save(file, dependencies)) {
            debug(file) {
                "dependencies are saved to file attributes: dependencies = $dependencies"
            }
            file.scriptDependencies = dependencies
        }

        if (rootsChanged) {
            shouldNotifyRootsChanged = true
        }
    }

    protected fun saveToCache(file: VirtualFile, refinementResults: RefinementResults, skipSaveToAttributes: Boolean = false) {
        val rootsChanged = cache.hasNotCachedRoots(refinementResults)
        if (cache.save(file, refinementResults) && !skipSaveToAttributes) {
            debug(file) {
                "refined configuration is saved to file attributes: $refinementResults"
            }
            if (refinementResults is RefinementResults.FromLegacy)
                file.scriptDependencies = refinementResults.scriptDependencies
            else
                file.scriptCompilationConfiguration = refinementResults.compilationConfiguration
        }

        if (rootsChanged) {
            shouldNotifyRootsChanged = true
        }
    }


    open fun notifyRootsChanged(): Boolean = submitMakeRootsChange()

    protected fun submitMakeRootsChange(): Boolean {
        if (!shouldNotifyRootsChanged) return false

        val doNotifyRootsChanged = Runnable {
            runWriteAction {
                if (project.isDisposed) return@runWriteAction

                debug(null) { "root change event for ${this.javaClass}" }

                shouldNotifyRootsChanged = false
                ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
            }
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            TransactionGuard.submitTransaction(project, doNotifyRootsChanged)
        } else {
            TransactionGuard.getInstance().submitTransactionLater(project, doNotifyRootsChanged)
        }

        return true
    }

    companion object {
        private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.script")

        internal fun debug(file: VirtualFile? = null, message: () -> String) {
            if (LOG.isDebugEnabled) {
                LOG.debug("[KOTLIN SCRIPT] " + (file?.let { "file = ${file.path}, " } ?: "") + message())
            }
        }
    }
}
