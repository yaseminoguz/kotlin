/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import java.net.URL
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapToDiagnostics
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.refineWith
import kotlin.script.experimental.jvm.impl.toDependencies

internal fun VirtualFile.loadAnnotations(
    acceptedAnnotations: List<KClass<out Annotation>>,
    project: Project,
    classLoader: ClassLoader
): List<Annotation> =
// TODO_R: report error on failure to load annotation class
    ApplicationManager.getApplication().runReadAction<List<Annotation>> {
        this.getAnnotationEntries(project).construct(classLoader, acceptedAnnotations, project)
    }

internal fun VirtualFile.getAnnotationEntries(project: Project): Iterable<KtAnnotationEntry> {
    val psiFile: PsiFile = PsiManager.getInstance(project).findFile(this)
        ?: throw IllegalArgumentException("Unable to load PSI from $canonicalPath")
    return (psiFile as? KtFile)?.annotationEntries
        ?: throw IllegalArgumentException("Unable to extract kotlin annotations from $name ($fileType)")
}

/**
 * The implementation of the SourceCode for a script located in a virtual file
 */
open class VirtualFileScriptSource(val file: VirtualFile, private val preloadedText: String? = null) :
    ExternalSourceCode {
    override val externalLocation: URL get() = URL(file.url)
    override val text: String by lazy { preloadedText ?: file.inputStream.bufferedReader().readText() }
    override val name: String? get() = file.name
    override val locationId: String? get() = file.path

    override fun equals(other: Any?): Boolean =
        this === other || (other as? VirtualFileScriptSource)?.let { file == it.file } == true

    override fun hashCode(): Int = file.hashCode()
}

/**
 * The implementation of the SourceCode for a script located in a KtFile
 */
open class KtFileScriptSource(val ktFile: KtFile, private val preloadedText: String? = null) :
    ExternalSourceCode {
    val virtualFile: VirtualFile? get() = ktFile.virtualFile ?: ktFile.originalFile.virtualFile
    override val externalLocation: URL get() = URL(virtualFile?.url ?: "file:unknown")
    override val text: String by lazy { preloadedText ?: ktFile.text }
    override val name: String? get() = ktFile.name
    override val locationId: String? get() = virtualFile?.path

    override fun equals(other: Any?): Boolean =
        this === other || (other as? KtFileScriptSource)?.let { ktFile == it.ktFile } == true

    override fun hashCode(): Int = ktFile.hashCode()
}

class ScriptLightVirtualFile(name: String, private val _path: String?, text: String) :
    LightVirtualFile(
        name,
        KotlinLanguage.INSTANCE,
        StringUtil.convertLineSeparators(text)
    ) {

    init {
        charset = CharsetToolkit.UTF8_CHARSET
    }

    override fun getPath(): String = _path ?: super.getPath()
    override fun getCanonicalPath(): String? = path
}

abstract class RefinementResults(val script: KtFileScriptSource) {
    abstract val compilationConfiguration: ScriptCompilationConfiguration?
    abstract val scriptDependencies: ScriptDependencies?

    class FromRefinement(
        script: KtFileScriptSource,
        override val compilationConfiguration: ScriptCompilationConfiguration?
    ) : RefinementResults(script) {
        override val scriptDependencies: ScriptDependencies?
            get() = compilationConfiguration?.let {
                val newClasspath = it[ScriptCompilationConfiguration.dependencies]
                    ?.flatMap { (it as JvmDependency).classpath } ?: emptyList()
                it.toDependencies(newClasspath)
            }
    }

    class FromLegacy(
        script: KtFileScriptSource,
        override val scriptDependencies: ScriptDependencies?
    ) : RefinementResults(script) {
        override val compilationConfiguration: ScriptCompilationConfiguration?
            get() = scriptDependencies?.let {
                TODO("drop or implement")
            }
    }
}

fun refineScriptCompilationConfiguration(
    script: SourceCode,
    definition: ScriptDefinition,
    project: Project,
    contextClass: KClass<*>
): ResultWithDiagnostics<RefinementResults> {
    val ktFileSource = script.toKtFileSource(definition, project)
    val legacyDefinition = definition.asLegacyOrNull<KotlinScriptDefinition>()
    if (legacyDefinition == null) {
        val compilationConfiguration = definition.compilationConfiguration
        val collectedData =
            getScriptCollectedData(ktFileSource.ktFile, compilationConfiguration, project, contextClass)

        return compilationConfiguration.refineWith(
            compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.handler, collectedData, script
        ).onSuccess {
            it.refineWith(
                compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationBeforeCompiling]?.handler, collectedData, script
            )
        }.onSuccess {
            RefinementResults.FromRefinement(ktFileSource, it).asSuccess()
        }
    } else {
        val file = script.getVirtualFile(definition)
        val scriptContents =
            makeScriptContents(file, legacyDefinition, project, contextClass.java.classLoader)
        val environment = (legacyDefinition as? KotlinScriptDefinitionFromAnnotatedTemplate)?.environment.orEmpty()

        val result: DependenciesResolver.ResolveResult = try {
            legacyDefinition.dependencyResolver.resolve(scriptContents, environment)
        } catch (e: Throwable) {
            return makeFailureResult(e.asDiagnostics())
        }
        return if (result is DependenciesResolver.ResolveResult.Failure) makeFailureResult(
            result.reports.mapToDiagnostics()
        )
        else RefinementResults.FromLegacy(ktFileSource, result.dependencies).asSuccess(result.reports.mapToDiagnostics())
    }
}

internal fun makeScriptContents(
    file: VirtualFile,
    legacyDefinition: KotlinScriptDefinition,
    project: Project,
    classLoader: ClassLoader
): ScriptContentLoader.BasicScriptContents =
    ScriptContentLoader.BasicScriptContents(
        file,
        getAnnotations = {
            file.loadAnnotations(legacyDefinition.acceptedAnnotations, project, classLoader)
        })

fun SourceCode.getVirtualFile(definition: ScriptDefinition): VirtualFile {
    if (this is VirtualFileScriptSource) return file
    if (this is KtFileScriptSource) {
        val vFile = virtualFile
        if (vFile != null) return vFile
    }
    if (this is FileScriptSource) {
        val vFile = LocalFileSystem.getInstance().findFileByIoFile(file)
        if (vFile != null) return vFile
    }
    val scriptName = name ?: "script.${definition.fileExtension}"
    val scriptPath = when (this) {
        is FileScriptSource -> file.path
        is ExternalSourceCode -> externalLocation.toString()
        else -> null
    }
    val scriptText = definition.asLegacyOrNull<KotlinScriptDefinition>()?.let { text }
        ?: getMergedScriptText(this, definition.compilationConfiguration)

    return ScriptLightVirtualFile(scriptName, scriptPath, scriptText)
}

fun SourceCode.getKtFile(definition: ScriptDefinition, project: Project): KtFile =
    if (this is KtFileScriptSource) ktFile
    else {
        val file = getVirtualFile(definition)
        ApplicationManager.getApplication().runReadAction<KtFile> {
            val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file)
                ?: throw IllegalArgumentException("Unable to load PSI from ${file.path}")
            (psiFile as? KtFile)
                ?: throw IllegalArgumentException("Not a kotlin file ${file.path} (${file.fileType})")
        }
    }

fun SourceCode.toKtFileSource(definition: ScriptDefinition, project: Project): KtFileScriptSource =
    if (this is KtFileScriptSource) this
    else {
        KtFileScriptSource(this.getKtFile(definition, project))
    }

fun getScriptCollectedData(
    scriptFile: KtFile,
    compilationConfiguration: ScriptCompilationConfiguration,
    project: Project,
    contextClass: KClass<*>
): ScriptCollectedData {
    val hostConfiguration =
        compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]!!
    val acceptedAnnotations =
        compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.annotations?.mapNotNull {
            getScriptingClass(it, contextClass, hostConfiguration) as? KClass<Annotation> // TODO errors
        }.orEmpty()
    val annotations = scriptFile.annotationEntries.construct(contextClass::class.java.classLoader, acceptedAnnotations, project)
    return ScriptCollectedData(
        mapOf(
            ScriptCollectedData.foundAnnotations to annotations
        )
    )
}

private fun Iterable<KtAnnotationEntry>.construct(
    classLoader: ClassLoader, acceptedAnnotations: List<KClass<out Annotation>>, project: Project
): List<Annotation> =
    mapNotNull { psiAnn ->
        // TODO: consider advanced matching using semantic similar to actual resolving
        acceptedAnnotations.find { ann ->
            psiAnn.typeName.let { it == ann.simpleName || it == ann.qualifiedName }
        }?.let {
            @Suppress("UNCHECKED_CAST")
            (constructAnnotation(
                psiAnn,
                classLoader.loadClass(it.qualifiedName).kotlin as KClass<out Annotation>,
                project
            ))
        }
    }