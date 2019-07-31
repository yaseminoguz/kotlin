/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirResolvedTypeRef : FirTypeRef {
    val type: ConeKotlinType

    val delegatedTypeRef: FirTypeRef? get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedTypeRef(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        delegatedTypeRef?.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}

interface FirResolvedFunctionTypeRef : @VisitedSupertype FirResolvedTypeRef, FirFunctionTypeRef {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedFunctionTypeRef(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirFunctionTypeRef>.acceptChildren(visitor, data)
    }
}

inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeUnsafe() = (this as FirResolvedTypeRef).type as T
inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeSafe() = (this as? FirResolvedTypeRef)?.type as? T