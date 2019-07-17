/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.frontend.js.di.createTopDownAnalyzerForJs
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.isBuiltIns
import org.jetbrains.kotlin.ir.backend.js.loadKlib
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzer
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.PackagesWithHeaderMetadata
import org.jetbrains.kotlin.utils.JsMetadataVersion

class JsReplCodeAnalyzer(val environment: KotlinCoreEnvironment) {

    private val replState = ReplCodeAnalyzer.ResettableAnalyzerState()
    val trace: BindingTraceContext = NoScopeRecordCliBindingTrace()

    lateinit var modulesStructure: ModulesStructure
    private fun createBuiltIns(dependencies: List<KotlinLibrary>, files: List<KtFile>): KotlinBuiltIns {
        val builtInsDep = dependencies.find { it.isBuiltIns }
        modulesStructure = ModulesStructure(environment.project, files, environment.configuration, dependencies, emptyList())
        val customBuiltInsModule = modulesStructure.getModuleDescriptor(builtInsDep!!)

        return customBuiltInsModule.builtIns
    }

    lateinit var context: MutableModuleContext
    lateinit var dependencies: List<KotlinLibrary>
    private fun createTopDownAnalyzerJS(files: Collection<KtFile>): LazyTopDownAnalyzer {
        val config = JsConfig(environment.project, environment.configuration)
        val runtimeKlib = loadKlib("/Users/Vitaliy.Tikhonov/kotlin/compiler/ir/serialization.js/build/reducedRuntime/klib")
        val moduleName = config.configuration[CommonConfigurationKeys.MODULE_NAME]!!

        dependencies = listOf(runtimeKlib)
        val builtIns = createBuiltIns(dependencies, files.toList())

        context = ContextForNewModule(
            ProjectContext(config.project, "TopDownAnalyzer for JS"),
            Name.special("<$moduleName>"),
            builtIns,
            platform = null
        )

        val languageVersionSettings = config.languageVersionSettings
        val lookupTracker = config.configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING
        val expectActualTracker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER) ?: ExpectActualTracker.DoNothing

        val additionalPackages = mutableListOf<PackageFragmentProvider>()

        val packageFragment = config.configuration[JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER]?.let { incrementalData ->
            val metadata = PackagesWithHeaderMetadata(
                incrementalData.headerMetadata,
                incrementalData.compiledPackageParts.values.map { it.metadata },
                JsMetadataVersion(*incrementalData.metadataVersion)
            )
            KotlinJavascriptSerializationUtil.readDescriptors(
                metadata, context.storageManager, context.module,
                CompilerDeserializationConfiguration(languageVersionSettings), lookupTracker
            )
        }

        val dependencies = mutableSetOf(context.module) + config.moduleDescriptors + builtIns.builtInsModule
        context.module.setDependencies(dependencies.toList(), config.friendModuleDescriptors.toSet())

        return createTopDownAnalyzerForJs(
            context, trace,
            FileBasedDeclarationProviderFactory(context.storageManager, files),
            languageVersionSettings,
            lookupTracker,
            expectActualTracker,
            additionalPackages + listOfNotNull(packageFragment)
        )
    }

    fun analyzeReplLineWithImportedScripts(psiFile: KtFile, importedScripts: List<KtFile>, codeLine: ReplCodeLine): ReplCodeAnalyzer.ReplLineAnalysisResult {
//        topDownAnalysisContext.scripts.clear()
        trace.clearDiagnostics()

        psiFile.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, codeLine.no)

        return doAnalyze(psiFile, importedScripts, codeLine)
    }

    fun doAnalyze(linePsi: KtFile, importedScripts: List<KtFile>, codeLine: ReplCodeLine): ReplCodeAnalyzer.ReplLineAnalysisResult {
//        scriptDeclarationFactory.setDelegateFactory(FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(linePsi)))
        replState.submitLine(linePsi, codeLine)

        val files = listOf(linePsi) + importedScripts
        val analyzer = createTopDownAnalyzerJS(files)
        val context = analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }
        return if (hasErrors) {
            replState.lineFailure(linePsi, codeLine)
            ReplCodeAnalyzer.ReplLineAnalysisResult.WithErrors(diagnostics)
        } else {
            val scriptDescriptor = context.scripts[linePsi.script]!!
            replState.lineSuccess(linePsi, codeLine, scriptDescriptor)
            ReplCodeAnalyzer.ReplLineAnalysisResult.Successful(scriptDescriptor, diagnostics)
        }
    }
}
