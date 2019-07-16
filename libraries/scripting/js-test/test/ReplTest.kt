/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.intellij.openapi.util.Disposer
import com.sun.tools.javac.util.Assert
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.junit.Test

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class ReplTest : TestCase() {

    @Test
    fun testCompileAndEval() {
        val compileResult = compile(
            """fun sq(x: Int): Int {
                return x + x
            }
            val tempX = sq(3)
            val tempY = sq(4)
            1 + tempX + tempY + 2
            tempX"""
        )
        Assert.check(evaluate(compileResult) == 6)
    }

    private fun compile(snippet: String): String {
        val configuration = CompilerConfiguration()
        val collector = object : MessageCollector {
            override fun clear() {
            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            }

            override fun hasErrors(): Boolean {
                return false
            }
        }

        configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, ScriptingCompilerConfigurationComponentRegistrar())
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, collector)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "test.kts")

        val compiler = KJsReplCompiler(configuration)
        return compiler.generateJsByReplSnippet(snippet, 0)
    }

    private fun evaluate(jsCode: String): Any? {
        val jsEngine = ScriptEngineNashorn()
        return jsEngine.eval(jsCode)
    }
}
