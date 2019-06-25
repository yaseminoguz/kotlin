/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

internal sealed class MetadataDependencyResolution(
    val dependency: ResolvedDependency,
    val projectDependency: ProjectDependency?
) {
    override fun toString(): String {
        val verb = when (this) {
            is KeepOriginalDependency -> "keep"
            is ExcludeAsUnrequested -> "exclude"
            is ChooseVisibleSourceSets -> "choose"
        }
        return "$verb, dependency = $dependency"
    }

    class KeepOriginalDependency(
        dependency: ResolvedDependency,
        projectDependency: ProjectDependency?
    ) : MetadataDependencyResolution(dependency, projectDependency)

    class ExcludeAsUnrequested(
        dependency: ResolvedDependency,
        projectDependency: ProjectDependency?
    ) : MetadataDependencyResolution(dependency, projectDependency)

    abstract class ChooseVisibleSourceSets(
        dependency: ResolvedDependency,
        projectDependency: ProjectDependency?,
        val projectStructureMetadata: KotlinProjectStructureMetadata,
        val allVisibleSourceSetNames: Set<String>,
        val visibleSourceSetNamesExcludingDependsOn: Set<String>,
        val visibleTransitiveDependencies: Set<ResolvedDependency>
    ) : MetadataDependencyResolution(dependency, projectDependency) {
        /** Returns the mapping of source set names to files which should be used as the [dependency] parts representing the source sets.
         * If any temporary files need to be created, their paths are built from the [baseDir].
         * If [doProcessFiles] is true, these temporary files are actually re-created during the call,
         * otherwise only their paths are returned, while the files might be missing.
         */
        abstract fun getMetadataFilesBySourceSet(baseDir: File, doProcessFiles: Boolean): Map<String, FileCollection>

        override fun toString(): String =
            super.toString() + ", sourceSets = " + allVisibleSourceSetNames.joinToString(", ", "[", "]") {
                (if (it in visibleSourceSetNamesExcludingDependsOn) "*" else "") + it
            }
    }
}

private typealias ModuleId = Pair<String?, String> // group ID, artifact ID

private val ResolvedDependency.moduleId: ModuleId
    get() = moduleGroup to moduleName

private val Dependency.moduleId: ModuleId
    get() = group to name

internal class GranularMetadataTransformation(
    val project: Project,
    val kotlinSourceSet: KotlinSourceSet,
    /** A list of scopes that the dependencies from [kotlinSourceSet] are treated as requested dependencies. */
    val sourceSetRequestedScopes: List<KotlinDependencyScope>,
    /** A configuration that holds the dependencies of the appropriate scope for all Kotlin source sets in the project */
    val allSourceSetsConfigurations: Iterable<Configuration>
) {
    val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> by lazy { doTransform() }

    // Keep parents of each dependency, too. We need a dependency's parent when it's an MPP's metadata module dependency:
    // in this case, the parent is the MPP's root module.
    private data class ResolvedDependencyWithParent(
        val dependency: ResolvedDependency,
        val parent: ResolvedDependency?
    )

    private fun collectProjectDependencies(
        requestedDependencies: Iterable<ProjectDependency>,
        resolvedDependencies: Iterable<ResolvedDependency>
    ): Map<ModuleId, ProjectDependency> {
        val result = mutableMapOf<ModuleId, ProjectDependency>()

        val resolvedDependenciesMap: Map<ModuleId, ResolvedDependency> = resolvedDependencies.associateBy { it.moduleId }

        fun visitProjectDependency(projectDependency: ProjectDependency) {
            val moduleId = projectDependency.group to projectDependency.name

            if (moduleId in result) return
            result[moduleId] = projectDependency

            val resolvedDependency = resolvedDependenciesMap[moduleId] ?: return

            projectDependency.dependencyProject.configurations.getByName(resolvedDependency.configuration)
                .allDependencies
                .withType(ProjectDependency::class.java)
                .forEach(::visitProjectDependency)
        }

        requestedDependencies.forEach(::visitProjectDependency)

        return result
    }

    private data class RequestedDependencies(
        val allDependencies: Set<Dependency>,

        /** If a dependency is declared in a non-root source set, it is important for us to know which dependsdOn parents actually
         * see the dependency. Those that don't should not be passed to the [SourceSetVisibilityProvider], as determining which
         * source sets they would see based on their compilations is useless. The keys are
         * the direct dependsOn parents of [kotlinSourceSet]
         */
        val dependencyModuleIdsByDirectParent: Map<KotlinSourceSet, Set<Dependency>>
    )

    private fun getRequestedDependencies(kotlinSourceSet: KotlinSourceSet): RequestedDependencies {
        // TODO generalize once a general production-test and other kinds of inter-compilation visibility are supported
        // Currently, this is a temporary ad-hoc mechanism for exposing the commonMain dependencies to the test source sets
        fun sourceSetsHierarchyInterCompilationClosure(sourceSets: Set<KotlinSourceSet>): Set<KotlinSourceSet> = when {
            sourceSets.any { it.name == COMMON_TEST_SOURCE_SET_NAME } ->
                sourceSets + project.kotlinExtension.sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME).getSourceSetHierarchy()
            else -> sourceSets
        }

        fun collectScopedDependenciesFromSourceSets(sourceSets: Iterable<KotlinSourceSet>) =
            sourceSets.flatMapTo(mutableSetOf<Dependency>()) { sourceSet ->
                sourceSetRequestedScopes.flatMapTo(mutableSetOf()) { scope ->
                    project.sourceSetDependencyConfigurationByScope(sourceSet, scope).allDependencies
                }
            }

        val dependenciesByParentOrSelf =
            sourceSetsHierarchyInterCompilationClosure(kotlinSourceSet.dependsOn + kotlinSourceSet)
                .associate { sourceSet ->
                    val hierarchyFromParent = sourceSetsHierarchyInterCompilationClosure(sourceSet.getSourceSetHierarchy())
                    sourceSet to collectScopedDependenciesFromSourceSets(hierarchyFromParent)
                }

        val allDependencies = dependenciesByParentOrSelf.values.flatMapTo(mutableSetOf()) { it }

        return RequestedDependencies(allDependencies, dependenciesByParentOrSelf.filterKeys { it !== kotlinSourceSet })
    }

    private val resolvedConfigurations: Iterable<LenientConfiguration> by lazy {
        allSourceSetsConfigurations.map { it.resolvedConfiguration.lenientConfiguration }
    }

    private fun doTransform(): Iterable<MetadataDependencyResolution> {
        val result = mutableListOf<MetadataDependencyResolution>()

        val (allRequestedDependencies, dependenciesInDirectParents) = getRequestedDependencies(kotlinSourceSet)

        val allModuleDependencies = resolvedConfigurations.flatMap { it.allModuleDependencies }

        val knownProjectDependencies = collectProjectDependencies(
            allRequestedDependencies.filterIsInstance<ProjectDependency>(),
            allModuleDependencies
        )

        val resolvedDependencyQueue: Queue<ResolvedDependencyWithParent> = ArrayDeque<ResolvedDependencyWithParent>().apply {
            val requestedModules: Set<ModuleId> = allRequestedDependencies.mapTo(mutableSetOf()) { it.moduleId }

            addAll(
                resolvedConfigurations.flatMap { it.firstLevelModuleDependencies }
                    .filter { it.moduleId in requestedModules }
                    .map { ResolvedDependencyWithParent(it, null) }
            )
        }

        val modulesByParentSourceSet =
            dependenciesInDirectParents.mapValues { (_, dependencies) ->
                dependencies.mapTo(mutableSetOf()) { it.moduleId }
            }

        val visitedDependencies = mutableSetOf<ResolvedDependency>()

        while (resolvedDependencyQueue.isNotEmpty()) {
            val (resolvedDependency, parent: ResolvedDependency?) = resolvedDependencyQueue.poll()

            val projectDependency: ProjectDependency? = knownProjectDependencies[resolvedDependency.moduleId]

            visitedDependencies.add(resolvedDependency)

            val parentSourceSetsWithParentDependency =
                parent?.moduleId?.let { id -> modulesByParentSourceSet.filter { id in it.value }.keys }.orEmpty()

            val dependencyResult = processDependency(
                resolvedDependency,
                parentSourceSetsWithParentDependency,
                parent,
                projectDependency
            )

            result.add(dependencyResult)

            val transitiveDependenciesToVisit = when (dependencyResult) {
                is MetadataDependencyResolution.KeepOriginalDependency -> resolvedDependency.children
                is MetadataDependencyResolution.ChooseVisibleSourceSets -> dependencyResult.visibleTransitiveDependencies
                is MetadataDependencyResolution.ExcludeAsUnrequested -> error("a visited dependency is erroneously considered unrequested")
            }

            resolvedDependencyQueue.addAll(
                transitiveDependenciesToVisit.filter { it !in visitedDependencies }
                    .map { ResolvedDependencyWithParent(it, resolvedDependency) }
            )
        }

        allModuleDependencies.forEach { resolvedDependency ->
            if (resolvedDependency !in visitedDependencies) {
                result.add(
                    MetadataDependencyResolution.ExcludeAsUnrequested(
                        resolvedDependency,
                        knownProjectDependencies[resolvedDependency.moduleGroup to resolvedDependency.moduleName]
                    )
                )
            }
        }

        return result
    }

    /**
     * If the [module] is an MPP metadata module, we extract [KotlinProjectStructureMetadata] and do the following:
     *
     * * get the [KotlinProjectStructureMetadata] from the dependency (either deserialize from the artifact or build from the project)
     *
     * * determine the set *S* of source sets that should be seen in the [kotlinSourceSet] by finding which variants the [parent]
     *   dependency got resolved for the compilations where [kotlinSourceSet] participates:
     *
     * * transform the single Kotlin metadata artifact into a set of Kotlin metadata artifacts for the particular source sets in
     *   *S* and add the results as [MetadataDependencyResolution.ChooseVisibleSourceSets]
     *
     * * based on the project structure metadata, determine which of the module's dependencies are requested by the
     *   source sets in *S*, then consider only these transitive dependencies, ignore the others;
     */
    private fun processDependency(
        module: ResolvedDependency,
        parentSourceSetsWithDependency: Set<KotlinSourceSet>,
        parent: ResolvedDependency?,
        projectDependency: ProjectDependency?
    ): MetadataDependencyResolution {

        val mppDependencyMetadataExtractor = when {
            projectDependency != null -> ProjectMppDependencyMetadataExtractor(project, module, projectDependency.dependencyProject)
            parent != null -> JarArtifactMppDependencyMetadataExtractor(project, module)
            else -> null
        }

        val projectStructureMetadata = mppDependencyMetadataExtractor?.getProjectStructureMetadata()

        if (projectStructureMetadata == null) {
            return MetadataDependencyResolution.KeepOriginalDependency(module, projectDependency)
        }

        val (allVisibleSourceSets, visibleByParents) =
            SourceSetVisibilityProvider(project).getVisibleSourceSets(
                kotlinSourceSet,
                sourceSetRequestedScopes,
                parent ?: module,
                projectStructureMetadata,
                projectDependency?.dependencyProject,
                parentSourceSetsWithDependency
            )

        // Keep only the transitive dependencies requested by the visible source sets:
        // Visit the transitive dependencies visible by parents, too (i.e. allVisibleSourceSets), as this source set might get a more
        // concrete view on them:
        val requestedTransitiveDependencies: Set<ModuleId> =
            mutableSetOf<ModuleId>().apply {
                projectStructureMetadata.sourceSetModuleDependencies.forEach { (sourceSetName, moduleDependencies) ->
                    if (sourceSetName in allVisibleSourceSets) {
                        addAll(moduleDependencies.map { ModuleId(it.groupId, it.moduleId) })
                    }
                }
            }

        val transitiveDependenciesToVisit = module.children.filterTo(mutableSetOf()) {
            (it.moduleId) in requestedTransitiveDependencies
        }

        val visibleSourceSetsExcludingDependsOn = allVisibleSourceSets.filterTo(mutableSetOf()) { it !in visibleByParents }

        return object : MetadataDependencyResolution.ChooseVisibleSourceSets(
            module,
            projectDependency,
            projectStructureMetadata,
            allVisibleSourceSets,
            visibleSourceSetsExcludingDependsOn,
            transitiveDependenciesToVisit
        ) {
            override fun getMetadataFilesBySourceSet(baseDir: File, doProcessFiles: Boolean): Map<String, FileCollection> =
                mppDependencyMetadataExtractor.getVisibleSourceSetsMetadata(visibleSourceSetsExcludingDependsOn, baseDir, doProcessFiles)
        }
    }
}

private abstract class MppDependencyMetadataExtractor(val project: Project, val dependency: ResolvedDependency) {
    abstract fun getProjectStructureMetadata(): KotlinProjectStructureMetadata?
    abstract fun getVisibleSourceSetsMetadata(
        visibleSourceSetNames: Set<String>,
        baseDir: File,
        doProcessFiles: Boolean
    ): Map<String, FileCollection>
}

private class ProjectMppDependencyMetadataExtractor(
    project: Project,
    dependency: ResolvedDependency,
    private val dependencyProject: Project
) : MppDependencyMetadataExtractor(project, dependency) {
    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? =
        buildKotlinProjectStructureMetadata(dependencyProject)

    override fun getVisibleSourceSetsMetadata(
        visibleSourceSetNames: Set<String>,
        baseDir: File,
        doProcessFiles: Boolean
    ): Map<String, FileCollection> =
        dependencyProject.multiplatformExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME).compilations
            .filter { it.defaultSourceSet.name in visibleSourceSetNames }
            .associate { it.defaultSourceSet.name to it.output.classesDirs }
}

private class JarArtifactMppDependencyMetadataExtractor(
    project: Project,
    dependency: ResolvedDependency
) : MppDependencyMetadataExtractor(project, dependency) {

    private val artifact: ResolvedArtifact?
        get() = dependency.moduleArtifacts.singleOrNull { it.extension == "jar" }

    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? {
        val artifactFile = artifact?.file ?: return null

        return ZipFile(artifactFile).use { zip ->
            val metadata = zip.getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_FILE_NAME")
                ?: return null

            val metadataXmlDocument = zip.getInputStream(metadata).use { inputStream ->
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
            }

            parseKotlinSourceSetMetadataFromXml(metadataXmlDocument)
        }
    }

    override fun getVisibleSourceSetsMetadata(
        visibleSourceSetNames: Set<String>,
        baseDir: File,
        doProcessFiles: Boolean
    ): Map<String, FileCollection> {
        val jarArtifact = artifact ?: return emptyMap()
        val artifactFile = jarArtifact.file
        val moduleId = jarArtifact.moduleVersion.id

        return extractSourceSetMetadataFromJar(moduleId, visibleSourceSetNames, artifactFile, baseDir, doProcessFiles)
    }

    private fun extractSourceSetMetadataFromJar(
        module: ModuleVersionIdentifier,
        chooseSourceSetsByNames: Set<String>,
        artifactJar: File,
        baseDir: File,
        doProcessFiles: Boolean
    ): Map<String, FileCollection> {
        val moduleString = "${module.group}-${module.name}-${module.version}"
        val transformedModuleRoot = run { baseDir.resolve(moduleString).also { it.mkdirs() } }

        val resultFiles = mutableMapOf<String, FileCollection>()

        ZipFile(artifactJar).use { zip ->
            val entriesBySourceSet = zip.entries().asSequence()
                .groupBy { it.name.substringBefore("/") }
                .filterKeys { it in chooseSourceSetsByNames }

            entriesBySourceSet.forEach { (sourceSetName, entries) ->
                // TODO: once IJ supports non-JAR metadata dependencies, extraact to a directory, not a JAR
                // Also, if both IJ and the CLI compiler can read metadata from a path inside a JAR, then no operations will be needed
                val extractToJarFile = transformedModuleRoot.resolve("$moduleString-$sourceSetName.jar")

                resultFiles[sourceSetName] = project.files(extractToJarFile)

                if (doProcessFiles) {
                    ZipOutputStream(extractToJarFile.outputStream()).use { resultZipOutput ->
                        for (entry in entries) {
                            if (entry.isDirectory)
                                continue

                            // Drop the source set name from the entry path
                            val resultEntry = ZipEntry(entry.name.substringAfter("/"))

                            zip.getInputStream(entry).use { inputStream ->
                                resultZipOutput.putNextEntry(resultEntry)
                                inputStream.copyTo(resultZipOutput)
                                resultZipOutput.closeEntry()
                            }
                        }
                    }
                }
            }
        }

        return resultFiles
    }
}
