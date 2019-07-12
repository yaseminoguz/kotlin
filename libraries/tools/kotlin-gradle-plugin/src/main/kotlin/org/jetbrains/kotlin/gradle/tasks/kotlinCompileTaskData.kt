/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import java.io.File

internal open class BasicKotlinCompileTaskData(
    val compilation: AbstractKotlinCompilation<*>,
    val taskName: String,
    var destinationDirProvider: () -> File
)

internal open class IncrementalKotlinCompileTaskData(
    compilation: AbstractKotlinCompilation<*>,
    taskName: String,
    destinationDirProvider: () -> File,
    var useModuleDetectionProvider: () -> Boolean
) : BasicKotlinCompileTaskData(compilation, taskName, destinationDirProvider) {

    private val taskBuildDirectory: File
        get() = File(File(compilation.target.project.buildDir, KOTLIN_BUILD_DIR_NAME), taskName)

    val buildHistoryFile: File
        get() = File(taskBuildDirectory, "build-history.bin")

    var javaOutputDir: File? = null
}