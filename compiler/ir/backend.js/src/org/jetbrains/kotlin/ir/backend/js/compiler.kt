/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.webWorkers.WORKER_MAIN_FUNCTION_NAME
import org.jetbrains.kotlin.ir.backend.js.webWorkers.moveWorkersToSeparateFiles
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.name
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    immediateDependencies: List<KlibModuleRef>,
    allDependencies: List<KlibModuleRef>,
    friendDependencies: List<KlibModuleRef>,
    mainArguments: List<String>?,
    directory: String
): String {
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(project, files, configuration, immediateDependencies, allDependencies, friendDependencies)

    val moduleDescriptor = moduleFragment.descriptor

    val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(moduleFragment)

    val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, configuration)

    // Load declarations referenced during `context` initialization
    dependencyModules.forEach {
        ExternalDependenciesGenerator(
            it.descriptor,
            symbolTable,
            irBuiltIns,
            deserializer = deserializer
        ).generateUnboundSymbolsAsDependencies()
    }

    // TODO: check the order
    val irFiles = dependencyModules.flatMap { it.files } + moduleFragment.files

    moduleFragment.files.clear()
    moduleFragment.files += irFiles

    // Create stubs
    ExternalDependenciesGenerator(
        moduleDescriptor = moduleDescriptor,
        symbolTable = symbolTable,
        irBuiltIns = irBuiltIns
    ).generateUnboundSymbolsAsDependencies()
    moduleFragment.patchDeclarationParents()

    val workerFiles = moduleFragment.files.flatMap { moveWorkersToSeparateFiles(it, context) }
    moduleFragment.files += workerFiles

    jsPhases.invokeToplevel(phaseConfig, context, moduleFragment)

    for (workerFile in workerFiles) {
        // TODO: Split into several files
        val output = File(directory + File.separator + workerFile.name)
        val workerMainFunction = workerFile.declarations.single { it.name.asString() == WORKER_MAIN_FUNCTION_NAME } as IrSimpleFunction
        val content = moduleFragment.accept(IrModuleToJsTransformer(context, workerMainFunction, emptyList()), null)
        output.parentFile.mkdirs()
        output.writeText(content.toString())
    }

    val jsProgram =
        moduleFragment.accept(IrModuleToJsTransformer(context, mainFunction, mainArguments), null)
    return jsProgram.toString()
}