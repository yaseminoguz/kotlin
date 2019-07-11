/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.inline.getCachedClassBytes
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode

object JvmCodegenDebuggerUtil {
    @JvmStatic
    fun isDebuggerContext(context: CodegenContext<*>): Boolean {
        val contextDescriptor = context.contextDescriptor as? DeclarationDescriptorWithSource
        val sourceElement = contextDescriptor?.source as? PsiSourceElement
        return sourceElement?.psi?.containingFile is KtCodeFragment
    }

    @JvmStatic
    fun shouldSkipAccessorsInDebugger(context: CodegenContext<*>, propertyDescriptor: PropertyDescriptor): Boolean {
        if (propertyDescriptor.isDelegated) {
            return false
        }

        val state = context.state

        if (propertyDescriptor is DeserializedPropertyDescriptor) {
            val containerClass = KotlinTypeMapper.getContainingClassesForDeserializedCallable(propertyDescriptor).implClassId
            val bytes = state.getCachedClassBytes(containerClass)

            val classNode = ClassNode()
            /*
                Probably, this should be optimized so the class won't be parsed multiple times.
                However, it's unlikely that there will be several same-class private property access in an evaluated expression,
                    so it's unclear if this will bring any good.
            */
            ClassReader(bytes).accept(classNode, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES or ClassReader.SKIP_FRAMES)

            val fieldOwnerContext = FieldOwnerContextForDebugger(propertyDescriptor)
            val fieldName = fieldOwnerContext.getFieldName(propertyDescriptor, false)

            return classNode.fields.any { it.name == fieldName }
        }

        return state.bindingContext[BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor] == true
    }

    private class FieldOwnerContextForDebugger(propertyDescriptor: PropertyDescriptor) :
        FieldOwnerContext<DeclarationDescriptor>(propertyDescriptor, OwnerKind.IMPLEMENTATION, null, null, null, null)
}