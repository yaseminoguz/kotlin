/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.File
import java.io.Serializable
import java.lang.Exception
import java.lang.reflect.InvocationTargetException
import java.util.*

interface CompressedCompilerArgument : Serializable {
    val argumentParts: List<Long>
}
data class CompressedCompilerArgumentImpl(override val argumentParts: List<Long>) : CompressedCompilerArgument {
    constructor(
        stringRepresentation: String,
        mapper: PathItemMapper
    ) : this(stringRepresentation.split(File.pathSeparator).map { mapper.getId(it) })
}

fun List<String>.toArgumentList(mapper: PathItemMapper): List<CompressedCompilerArgument> = this.map { CompressedCompilerArgumentImpl(it, mapper) }

fun List<CompressedCompilerArgument>.toStringList(mapper: PathItemMapper): List<String> =
    this.map { it.argumentParts.map { itemId -> mapper.getPathItem(itemId) }.joinToString(File.pathSeparator) }

interface PathItemMapper : Serializable {
    val idToPathItem: HashMap<Long, String>
    val pathItemToId: HashMap<String, Long>
}

class PathItemMapperImpl : PathItemMapper {
    var currentIndex: Long = 0
    override val idToPathItem = HashMap<Long, String>()
    override val pathItemToId = HashMap<String, Long>()
}

fun PathItemMapper.getPathItem(id: Long) = idToPathItem[id]

fun PathItemMapper.getId(pathItem: String): Long {
    return pathItemToId[pathItem] ?: let {
        (it as PathItemMapperImpl).currentIndex++
        pathItemToId[pathItem] = (it as PathItemMapperImpl).currentIndex
        idToPathItem[(it as PathItemMapperImpl).currentIndex] = pathItem
        return (it as PathItemMapperImpl).currentIndex
    }
}

typealias CompilerArgument = String


//fun List<String>.toArgumentList(mapper: PathItemMapper): List<String> = this
//fun List<CompilerArgument>.toStringList(mapper: PathItemMapper): List<String> = this

interface ArgsInfo : Serializable {
    val currentArguments: () -> List<CompilerArgument>
    val defaultArguments: () -> List<CompilerArgument>
    val dependencyClasspath: () -> List<CompilerArgument>
}

interface CompressedArgsInfo : Serializable {
    val currentArguments: List<CompressedCompilerArgument>
    val defaultArguments: List<CompressedCompilerArgument>
    val dependencyClasspath: List<CompressedCompilerArgument>

}

data class ArgsInfoImpl(
    override val currentArguments: () -> List<CompilerArgument>,
    override val defaultArguments: () -> List<CompilerArgument>,
    override val dependencyClasspath: () -> List<CompilerArgument>
) : ArgsInfo {

    constructor(argsInfo: CompressedArgsInfo, mapper: PathItemMapper) : this(
        { argsInfo.currentArguments.toStringList(mapper) },
        { argsInfo.defaultArguments.toStringList(mapper) },
        { argsInfo.dependencyClasspath.toStringList(mapper) }
    )
}


data class CompressedArgsInfoImpl(
    override val currentArguments: List<CompressedCompilerArgument>,
    override val defaultArguments: List<CompressedCompilerArgument>,
    override val dependencyClasspath: List<CompressedCompilerArgument>
) : CompressedArgsInfo {

    constructor(argsInfo: CompressedArgsInfo) : this(
        ArrayList(argsInfo.currentArguments),
        ArrayList(argsInfo.defaultArguments),
        ArrayList(argsInfo.dependencyClasspath)
    )
}

typealias CompilerArgumentsBySourceSet = Map<String, ArgsInfo>
typealias CompressedCompilerArgumentsBySourceSet = Map<String, CompressedArgsInfo>


/**
 * Creates deep copy in order to avoid holding links to Proxy objects created by gradle tooling api
 */
fun CompressedCompilerArgumentsBySourceSet.deepCopy(mapper: PathItemMapper): CompilerArgumentsBySourceSet {
    val result = HashMap<String, ArgsInfo>()
    this.forEach { key, value -> result[key] = ArgsInfoImpl(CompressedArgsInfoImpl(value), mapper) }
    return result
}

interface KotlinGradleModel : Serializable {
    val hasKotlinPlugin: Boolean
    val compilerArgumentsBySourceSet: CompressedCompilerArgumentsBySourceSet
    val coroutines: String?
    val platformPluginId: String?
    val implements: List<String>
    val kotlinTarget: String?
    val kotlinTaskProperties: KotlinTaskPropertiesBySourceSet
    val pathItemMapper: PathItemMapper
}

data class KotlinGradleModelImpl(
    override val hasKotlinPlugin: Boolean,
    override val compilerArgumentsBySourceSet: CompressedCompilerArgumentsBySourceSet,
    override val coroutines: String?,
    override val platformPluginId: String?,
    override val implements: List<String>,
    override val kotlinTarget: String? = null,
    override val kotlinTaskProperties: KotlinTaskPropertiesBySourceSet,
    override val pathItemMapper: PathItemMapper
) : KotlinGradleModel

abstract class AbstractKotlinGradleModelBuilder : ModelBuilderService {
    companion object {
        val kotlinCompileJvmTaskClasses = listOf(
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompile_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompileWithWorkers_Decorated"
        )

        val kotlinCompileTaskClasses = kotlinCompileJvmTaskClasses + listOf(
            "org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompileWithWorkers_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommonWithWorkers_Decorated"
        )
        val platformPluginIds = listOf("kotlin-platform-jvm", "kotlin-platform-js", "kotlin-platform-common")
        val pluginToPlatform = linkedMapOf(
            "kotlin" to "kotlin-platform-jvm",
            "kotlin2js" to "kotlin-platform-js"
        )
        val kotlinPluginIds = listOf("kotlin", "kotlin2js", "kotlin-android")
        val ABSTRACT_KOTLIN_COMPILE_CLASS = "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile"

        val kotlinProjectExtensionClass = "org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension"
        val kotlinSourceSetClass = "org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet"

        val kotlinPluginWrapper = "org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapperKt"

        fun Task.getSourceSetName(): String {
            return try {
                javaClass.methods.firstOrNull { it.name.startsWith("getSourceSetName") && it.parameterTypes.isEmpty() }?.invoke(this) as? String
            } catch (e: InvocationTargetException) {
                null // can be thrown if property is not initialized yet
            } ?: "main"
        }
    }
}

class KotlinGradleModelBuilder : AbstractKotlinGradleModelBuilder() {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build Kotlin project configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == KotlinGradleModel::class.java.name

    private fun getImplementedProjects(project: Project): List<Project> {
        return listOf("expectedBy", "implement")
            .flatMap { project.configurations.findByName(it)?.dependencies ?: emptySet<Dependency>() }
            .filterIsInstance<ProjectDependency>()
            .mapNotNull { it.dependencyProject }
    }

    // see GradleProjectResolverUtil.getModuleId() in IDEA codebase
    private fun Project.pathOrName() = if (path == ":") name else path

    @Suppress("UNCHECKED_CAST")
    private fun Task.getCompilerArguments(methodName: String): List<String>? {
        return try {
            javaClass.getDeclaredMethod(methodName).invoke(this) as List<String>
        } catch (e: Exception) {
            // No argument accessor method is available
            null
        }
    }

    private fun Task.getDependencyClasspath(): List<String> {
        try {
            val abstractKotlinCompileClass = javaClass.classLoader.loadClass(ABSTRACT_KOTLIN_COMPILE_CLASS)
            val getCompileClasspath = abstractKotlinCompileClass.getDeclaredMethod("getCompileClasspath").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            return (getCompileClasspath.invoke(this) as Collection<File>).map { it.path }
        } catch (e: ClassNotFoundException) {
            // Leave arguments unchanged
        } catch (e: NoSuchMethodException) {
            // Leave arguments unchanged
        } catch (e: InvocationTargetException) {
            // We can safely ignore this exception here as getCompileClasspath() gets called again at a later time
            // Leave arguments unchanged
        }
        return emptyList()
    }

    private fun getCoroutines(project: Project): String? {
        val kotlinExtension = project.extensions.findByName("kotlin") ?: return null
        val experimentalExtension = try {
            kotlinExtension::class.java.getMethod("getExperimental").invoke(kotlinExtension)
        } catch (e: NoSuchMethodException) {
            return null
        }

        return try {
            experimentalExtension::class.java.getMethod("getCoroutines").invoke(experimentalExtension)?.toString()
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    override fun buildAll(modelName: String?, project: Project): KotlinGradleModelImpl {
        val pathMapper = PathItemMapperImpl()
        val kotlinPluginId = kotlinPluginIds.singleOrNull { project.plugins.findPlugin(it) != null }
        val platformPluginId = platformPluginIds.singleOrNull { project.plugins.findPlugin(it) != null }

        val compilerArgumentsBySourceSet = LinkedHashMap<String, CompressedArgsInfo>()
        val extraProperties = HashMap<String, KotlinTaskProperties>()

        project.getAllTasks(false)[project]?.forEach { compileTask ->
            if (compileTask.javaClass.name !in kotlinCompileTaskClasses) return@forEach

            val sourceSetName = compileTask.getSourceSetName()
            val currentArguments = compileTask.getCompilerArguments("getSerializedCompilerArguments")
                ?: compileTask.getCompilerArguments("getSerializedCompilerArgumentsIgnoreClasspathIssues") ?: emptyList()
            val defaultArguments = compileTask.getCompilerArguments("getDefaultSerializedCompilerArguments").orEmpty()
            val dependencyClasspath = compileTask.getDependencyClasspath()
            compilerArgumentsBySourceSet[sourceSetName] =
                CompressedArgsInfoImpl(
                    currentArguments.toArgumentList(pathMapper),
                    defaultArguments.toArgumentList(pathMapper),
                    dependencyClasspath.toArgumentList(pathMapper)
                )
            extraProperties.acknowledgeTask(compileTask)
        }

        val platform = platformPluginId ?: pluginToPlatform.entries.singleOrNull { project.plugins.findPlugin(it.key) != null }?.value
        val implementedProjects = getImplementedProjects(project)

        return KotlinGradleModelImpl(
            kotlinPluginId != null || platformPluginId != null,
            compilerArgumentsBySourceSet,
            getCoroutines(project),
            platform,
            implementedProjects.map { it.pathOrName() },
            platform ?: kotlinPluginId,
            extraProperties,
            pathMapper
        )
    }
}
