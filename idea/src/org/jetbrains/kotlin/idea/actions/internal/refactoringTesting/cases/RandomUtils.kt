/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.apache.commons.lang.RandomStringUtils
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import kotlin.random.Random

internal fun <T> List<T>.randomElement(): T = this[Random.nextInt(0, this.size)]

internal fun <T : Any> List<T>.randomElementOrNull(): T? = if (isNotEmpty()) randomElement() else null

internal fun <T : Any> List<T>.toRandomSequence(): Sequence<T> = generateSequence { randomElementOrNull() }

internal fun <T> List<T>.randomElements(count: Int): List<T> =
    mutableListOf<T>().also { list -> repeat(count) { list.add(randomElement()) } }

internal fun <T> List<T>.randomElements(): List<T> = randomElements(Random.nextInt(0, size))

internal fun getRandomFileClassElements(project: Project, ktFiles: List<VirtualFile>): List<KtClass> {
    val randomSourceFile = ktFiles.randomElement()
    return PsiTreeUtil.collectElementsOfType(randomSourceFile.toPsiFile(project), KtClass::class.java).toList()
}

internal fun randomBoolean() = Random.nextBoolean()

internal fun Project.files(): List<VirtualFile> {
    val scope = KotlinSourceFilterScope.projectSources(ProjectScope.getContentScope(this), this)
    return FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope).toList()
}

internal fun randomNullableString() = if (randomBoolean()) randomString() else null

internal fun randomString() = RandomStringUtils.randomAlphanumeric(Random.nextInt(1, 10))

internal fun randomClassName(): String {
    var count = Random.nextInt(1, 10)
    val strings = generateSequence {
        count--
        if (count > 0) randomString() else null
    }
    return strings.joinToString(".")
}