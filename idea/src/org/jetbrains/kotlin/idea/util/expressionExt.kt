/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinPropertyShortNameIndex
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor


fun KtCallExpression.replaceOrCreateTypeArgumentList(newTypeArgumentList: KtTypeArgumentList) {
    if (typeArgumentList != null) typeArgumentList?.replace(newTypeArgumentList)
    else addAfter(
        newTypeArgumentList,
        calleeExpression
    )
}

fun KtModifierListOwner.hasInlineModifier() = hasModifier(KtTokens.INLINE_KEYWORD)

fun KtModifierListOwner.hasPrivateModifier() = hasModifier(KtTokens.PRIVATE_KEYWORD)

fun KtPrimaryConstructor.allowedValOrVar(): Boolean = containingClass()?.let {
    it.isAnnotation() || it.hasInlineModifier()
} ?: false

// TODO: add cases
fun KtExpression.hasNoSideEffects(): Boolean = when (this) {
    is KtStringTemplateExpression -> !hasInterpolation()
    is KtConstantExpression -> true
    else -> ConstantExpressionEvaluator.getConstant(this, analyze(BodyResolveMode.PARTIAL)) != null
}

fun PsiElement.textRangeIn(other: PsiElement): TextRange = textRange.shiftLeft(other.startOffset)

fun KtDotQualifiedExpression.calleeTextRangeInThis(): TextRange? = callExpression?.calleeExpression?.textRangeIn(this)

fun KtNamedDeclaration.nameIdentifierTextRangeInThis(): TextRange? = nameIdentifier?.textRangeIn(this)

fun PsiElement.hasComments(): Boolean = anyDescendantOfType<PsiComment>()

fun DeclarationDescriptor.findPsiElements(
    project: Project,
    resolveScopeGenerator: () -> GlobalSearchScope
): Collection<PsiElement> {
    findPsi()?.let { return listOf(it) }
    val fqName = importableFqName ?: return emptyList()
    return when (this) {
        is DeserializedClassDescriptor -> KotlinFullClassNameIndex.getInstance()[fqName.asString(), project, resolveScopeGenerator()]
        is DeserializedSimpleFunctionDescriptor -> KotlinFunctionShortNameIndex.getInstance()[fqName.shortName().asString(), project, resolveScopeGenerator()]
        is PropertyImportedFromObject -> KotlinPropertyShortNameIndex.getInstance()[fqName.shortName().asString(), project, resolveScopeGenerator()]
        else -> emptyList()
    }.filter { fqName == it.fqName }
}