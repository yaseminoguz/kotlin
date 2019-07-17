/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.DFS

private fun sortDependencies(dependencies: Collection<IrModuleFragment>): Collection<IrModuleFragment> {
    val mapping = dependencies.map { it.descriptor to it }.toMap()

    return DFS.topologicalOrder(dependencies) { m ->
        val descriptor = m.descriptor
        descriptor.allDependencyModules.filter { it != descriptor }.map { mapping[it] }
    }.reversed()
}

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    allDependencies: List<KotlinLibrary>,
    friendDependencies: List<KotlinLibrary>,
    mainArguments: List<String>?,
    exportedDeclarations: Set<FqName> = emptySet()
): String {
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(project, files, configuration, allDependencies, friendDependencies)

    val moduleDescriptor = moduleFragment.descriptor

    val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(moduleFragment)

    val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, exportedDeclarations, configuration)

    // Load declarations referenced during `context` initialization
    dependencyModules.forEach {
        ExternalDependenciesGenerator(
            it.descriptor,
            symbolTable,
            irBuiltIns,
            deserializer = deserializer
        ).generateUnboundSymbolsAsDependencies()
    }

    // Since modules should be initialized in the correct topological order we sort them
    val irFiles = sortDependencies(dependencyModules).flatMap { it.files } + moduleFragment.files

    moduleFragment.files.clear()
    moduleFragment.files += irFiles

    // Create stubs
    ExternalDependenciesGenerator(
        moduleDescriptor = moduleDescriptor,
        symbolTable = symbolTable,
        irBuiltIns = irBuiltIns
    ).generateUnboundSymbolsAsDependencies()
    moduleFragment.patchDeclarationParents()

    jsPhases.invokeToplevel(phaseConfig, context, moduleFragment)

    val jsProgram =
        moduleFragment.accept(IrModuleToJsTransformer(context, mainFunction, mainArguments), null)
    return jsProgram.toString()
}

fun compileForRepl(configuration: CompilerConfiguration, psi2irContext: GeneratorContext, moduleFragment: IrModuleFragment): String {
    configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
    val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(moduleFragment)

    val context = JsIrBackendContext(
        moduleFragment.descriptor,
        psi2irContext.irBuiltIns,
        psi2irContext.symbolTable,
        moduleFragment,
        emptySet(),
        configuration
    )

    ExternalDependenciesGenerator(
        moduleDescriptor = moduleFragment.descriptor,
        symbolTable = psi2irContext.symbolTable,
        irBuiltIns = psi2irContext.irBuiltIns
    ).generateUnboundSymbolsAsDependencies()

    jsPhases.invokeToplevel(PhaseConfig(jsPhases), context, moduleFragment)

    val jsProgram =
        moduleFragment.accept(IrModuleToJsTransformer(context, mainFunction, emptyList()).also { it.generateScriptModule = true }, null)
    return jsProgram.toString()
}
