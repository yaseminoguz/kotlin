/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder.Companion.getSourceSetName
import java.io.Serializable
import java.lang.Exception

interface KotlinTaskProperties : Serializable {
    val incremental: Boolean?
    val packagePrefix: String?
}

data class KotlinTaskPropertiesImpl(
    override val incremental: Boolean?,
    override val packagePrefix: String?
) : KotlinTaskProperties

typealias KotlinTaskPropertiesBySourceSet = MutableMap<String, KotlinTaskProperties>

private fun Task.getPackagePrefix(): String? {
    try {
        val kotlinCompileClass = javaClass.classLoader.loadClass(AbstractKotlinGradleModelBuilder.KOTLIN_COMPILE_CLASS)
        val getCompileClasspath = kotlinCompileClass.getDeclaredMethod("getJavaPackagePrefix").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return (getCompileClasspath.invoke(this) as? String)
    } catch (e: Exception) {
    }
    return null
}

private fun Task.getIsIncremental(): Boolean? {
    try {
        val abstractKotlinCompileClass = javaClass.classLoader.loadClass(AbstractKotlinGradleModelBuilder.ABSTRACT_KOTLIN_COMPILE_CLASS)
        val getCompileClasspath = abstractKotlinCompileClass.getDeclaredMethod("getIncremental").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return (getCompileClasspath.invoke(this) as? Boolean)
    } catch (e: Exception) {
    }
    return null
}

fun KotlinTaskPropertiesBySourceSet.acknowledgeTask(compileTask: Task) {
    this[compileTask.getSourceSetName()] = KotlinTaskPropertiesImpl(compileTask.getIsIncremental(), compileTask.getPackagePrefix())
}
