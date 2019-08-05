/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.jetbrains.kotlin.ir.backend.js.loadKlib
import kotlin.script.jsrepl.JsReplBase
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader

class Repl : Closeable {
    private val disposable: Disposable = Disposer.newDisposable()
    private val base: JsReplBase

    init {
        val fullRuntimeKlib = loadKlib("compiler/ir/serialization.js/build/fullRuntime/klib")
        base = JsReplBase(disposable, fullRuntimeKlib)
    }

    fun read(reader: BufferedReader): String {
        val input = StringBuilder()
        var balance = 0

        do {
            val line = reader.readLine()
            input.append(line)
            input.append(System.lineSeparator())

            balance += line.count { it == '{' }
            balance -= line.count { it == '}' }
        } while (balance != 0)

        return input.toString()
    }

    fun eval(line: String): Any? {
        base.collector.clear()
        val jsCode = base.compiler.generateJsByReplSnippet(line, base.newSnippetId())
        return base.jsEngine.eval(jsCode)
    }

    override fun close() {
        Disposer.dispose(disposable)
    }
}

fun main() {
    BufferedReader(InputStreamReader(System.`in`)).use { reader ->
        Repl().use { repl ->
            println("KJsRepl")

            while (true) {
                val input = repl.read(reader)
                if (input == "stop" + System.lineSeparator()) break

                try {
                    val result = repl.eval(input)
                    if (result != null && result !is String && result !is Number) {
                        println("Object { ")
                        (result as ScriptObjectMirror).entries.forEach {
                            println(it.toString().replace("  ", "    "))
                        }
                        println("}")
                    } else {
                        println("-> $result")
                    }
                } catch (e: Throwable) {
                    System.err.print(e.message)
                }
            }
        }
    }
}
