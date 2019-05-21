/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import org.jetbrains.kotlin.scripting.definitions.LazyScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import kotlin.concurrent.write

open class CliScriptDefinitionProvider : LazyScriptDefinitionProvider() {
    private val definitionsFromSources: MutableList<Sequence<ScriptDefinition>> = arrayListOf()
    private val definitions: MutableList<ScriptDefinition> = arrayListOf()
    private var hasStandardDefinition = true

    override val currentDefinitions: Sequence<ScriptDefinition>
        get() {
            val base = definitions.asSequence() + definitionsFromSources.asSequence().flatMap { it }
            return if (hasStandardDefinition) base + getDefaultDefinition() else base
        }

    fun setScriptDefinitions(newDefinitions: List<ScriptDefinition>) {
        lock.write {
            definitions.clear()
            val (withoutStdDef, stdDef) = newDefinitions.partition { !it.isDefault }
            definitions.addAll(withoutStdDef)
            hasStandardDefinition = stdDef.isNotEmpty()
        }
    }

    fun setScriptDefinitionsSources(newSources: List<ScriptDefinitionsSource>) {
        lock.write {
            definitionsFromSources.clear()
            for (it in newSources) {
                definitionsFromSources.add(it.definitions.constrainOnce())
            }
        }
    }
}
