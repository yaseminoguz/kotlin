/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.KtLightClassForLocalDeclaration.Companion.getClassNameForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClassForLocalDeclaration.Companion.getParentForLocalDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject

open class KtUltraLightClassForLocalDeclaration(
    classOrObject: KtClassOrObject,
    support: KtUltraLightSupport
) : KtUltraLightClass(classOrObject, support) {

    private val _parent: PsiElement? by lazyPub { getParentForLocalDeclaration(classOrObject) }

    private val _name by lazyPub { getClassNameForLocalDeclaration(classOrObject) }

    override fun copy() = KtUltraLightClassForLocalDeclaration(classOrObject.copy() as KtClassOrObject, support)

    override fun getQualifiedName(): String? = null

    override fun getName(): String? = _name

    override fun getParent() = _parent
}