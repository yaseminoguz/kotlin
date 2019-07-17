import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.repl.BasicReplStageHistory
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.ir.backend.js.compileForRepl
import org.jetbrains.kotlin.ir.backend.js.emptyLoggingContext
import org.jetbrains.kotlin.ir.backend.js.generateModuleFragment
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzer
//import org.jetbrains.kotlin.scripting.repl.JsReplCodeAnalyzer
import org.jetbrains.kotlin.scripting.resolve.ScriptLightVirtualFile
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.StringScriptSource

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class KJsReplCompiler(private val configuration: CompilerConfiguration) {

    fun generateJsByReplSnippet(
        snippet: String,
        snippetId: Int
    ): String {
        val disposer = Disposer.newDisposable()
        val environment =
            KotlinCoreEnvironment.createForProduction(
                disposer, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES
            )
        val analyzerEngine = JsReplCodeAnalyzer(environment)

        val history = BasicReplStageHistory<ScriptDescriptor>() //??

        setIdeaIoUseFallback()
//
//        // NOTE: converting between REPL entities from compiler and "new" scripting entities
//        // TODO: (big) move REPL API from compiler to the new scripting infrastructure and streamline ops
        val codeLine = makeReplCodeLine(snippetId, snippet)

        val sourceCode = StringScriptSource(snippet, "test.kts")
        val snippetKtFile =
            getScriptKtFile(sourceCode, snippet, environment.project)
                .valueOr { return "Error" }

        val sourceFiles = listOf(snippetKtFile)

        val analysisResult =
            analyzerEngine.analyzeReplLineWithImportedScripts(snippetKtFile, sourceFiles.drop(1), codeLine)

//        AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

        val scriptDescriptor = when (analysisResult) {
            is ReplCodeAnalyzer.ReplLineAnalysisResult.WithErrors -> return "Error"
            is ReplCodeAnalyzer.ReplLineAnalysisResult.Successful -> analysisResult.scriptDescriptor
            else -> return "Error"
        }

        val psi2ir = Psi2IrTranslator(environment.configuration.languageVersionSettings)
        val psi2irContext = psi2ir.createGeneratorContext(
            analyzerEngine.context.module,
            analyzerEngine.trace.bindingContext
        )

        val deserializer = JsIrLinker(
            psi2irContext.moduleDescriptor,
            emptyLoggingContext,
            psi2irContext.irBuiltIns,
            psi2irContext.symbolTable
        )

        //???
        val deserializedModuleFragments = analyzerEngine.dependencies.map {
            deserializer.deserializeIrModuleHeader(analyzerEngine.modulesStructure.getModuleDescriptor(it))!!
        }

        val irModuleFragment = psi2irContext.generateModuleFragment(sourceFiles, deserializer)

        return compileForRepl(
            environment.configuration,
            psi2irContext,
            irModuleFragment
        )
    }
}

fun getScriptKtFile(
    script: SourceCode,
    scriptText: String,
    project: Project
//    messageCollector: ScriptDiagnosticsMessageCollector
): ResultWithDiagnostics<KtFile> {
    val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val virtualFile = ScriptLightVirtualFile(
        script.name!!,
        (script as? FileBasedScriptSource)?.file?.path,
        scriptText
    )
    val ktFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
    return when {
        ktFile == null -> throw IllegalArgumentException()
        ktFile.declarations.firstIsInstanceOrNull<KtScript>() == null -> throw IllegalArgumentException()
        else -> ktFile.asSuccess()
    }
}

internal fun makeReplCodeLine(no: Int, code: String): ReplCodeLine =
    ReplCodeLine(no, 0, code)
