/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirResolvedTypeRefImpl(
    psi: PsiElement?,
    override val type: ConeKotlinType,
    override val annotations: List<FirAnnotationCall> = emptyList(),
    override var delegatedTypeRef: FirTypeRef? = null
) : FirResolvedTypeRef, FirAbstractElement(psi) {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        delegatedTypeRef = delegatedTypeRef?.transformSingle(transformer, data)
        return super<FirAbstractElement>.transformChildren(transformer, data)
    }
}