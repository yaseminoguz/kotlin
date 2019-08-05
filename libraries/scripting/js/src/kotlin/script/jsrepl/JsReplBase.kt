/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.jsrepl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class JsReplBase(disposable: Disposable, klib: KotlinLibrary) {
    private val configuration: CompilerConfiguration
    val collector: ReplMessageCollector
    val compiler: KJsReplCompiler
    val jsEngine: ScriptEngineNashorn

    private var snippetId: Int = 1 //index 0 for libs
    fun newSnippetId(): Int = snippetId++

    fun dumpJsCode() {
        val path = "" //set path for dump here
        for (i in compiler.jsCode.indices) {
            val writer = BufferedWriter(OutputStreamWriter(FileOutputStream("$path/line_$i.js")))
            writer.write(compiler.jsCode[i])
            writer.close()
        }
    }

    init {
        collector = ReplMessageCollector()

        configuration = CompilerConfiguration()
        configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, ScriptingCompilerConfigurationComponentRegistrar())
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, collector)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "repl.kts")

        compiler = KJsReplCompiler(configuration, klib, disposable)

        jsEngine = ScriptEngineNashorn()
        jsEngine.eval<Any?>(compiler.jsCode.first())
    }
}
