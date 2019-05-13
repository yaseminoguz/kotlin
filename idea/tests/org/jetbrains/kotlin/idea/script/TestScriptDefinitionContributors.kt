/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import org.jetbrains.kotlin.idea.core.script.NewScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import java.io.File
import kotlin.script.dependencies.Environment
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration


class CustomScriptTemplateProvider(val environment: Environment) : NewScriptDefinitionContributor {

    override val id = "Test"

    override fun getNewDefinitions() = loadDefinitionsFromTemplates(
        templateClassNames = environment["template-classes-names"] as List<String>,
        templateClasspath = listOfNotNull(environment["template-classes"] as? File),
        baseHostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
            getEnvironment { environment }
        }
    )

}

class FromTextTemplateProvider(val environment: Map<String, Any?>) : NewScriptDefinitionContributor {

    override val id = "Test"

    override fun getNewDefinitions() = loadDefinitionsFromTemplates(
        templateClassNames = listOf("org.jetbrains.kotlin.idea.script.Template"),
        templateClasspath = listOfNotNull(environment["template-classes"] as? File),
        baseHostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
            getEnvironment { environment }
        }
    )
}