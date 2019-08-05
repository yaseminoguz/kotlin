/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.intellij.openapi.util.Disposer
import junit.framework.TestCase
import org.jetbrains.kotlin.ir.backend.js.loadKlib
import org.junit.Assert
import org.junit.Test
import kotlin.script.jsrepl.JsReplBase

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private val fullRuntimeKlib = loadKlib("compiler/ir/serialization.js/build/fullRuntime/klib")

class ReplTest : TestCase() {

    @Test
    fun testIndependentLines() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            "var x = 38",
            "var y = 99",
            "4 + 1"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals(5, result)
    }

    @Test
    fun testDependentLines() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            "var x = 7",
            "var y = 32",
            "x + y"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals(39, result)
    }

    @Test
    fun testFunctionCall() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            "var x = 2",
            "var y = 3",
            "fun foo(x: Int, unused: Int) = x + y",
            "foo(x, x) * foo(y, y)"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals(30, result)
    }

    @Test
    fun testList() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            "var a = 4",
            "var b = 6",
            "listOf(a, 5, b).last()"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Assert.assertEquals(6, result)
    }

    @Test
    fun testMatchingNames() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            "fun foo(i: Int) = i + 2",
            "fun foo(s: String) = s",
            "class C {fun foo(s: String) = s + s}",
            "foo(\"x\") + foo(2) + C().foo(\"class\")"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals("x4classclass", result)
    }

    @Test
    fun testInline() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            "inline fun foo(i : Int) = if (i % 2 == 0) {} else i",
            """
            fun box(): String {
                val a = foo(1)
                if (a != 1) return "fail1: ${'$'}a"

                val b = foo(2)
                if (b != Unit) return "fail2: ${'$'}b"

                return "OK"
            },
            box()
            """
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals("OK", result)
    }

    @Test
    fun testAnonymous() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            """
            inline fun foo(f: () -> String): () -> String {
                val result = f()
                return { result }
            }
            """,
            "fun bar(f: () -> String) = foo(f)()",
            "fun box(): String = bar { \"OK\" }",
            "box()"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals("OK", result)
    }

    @Test
    fun testNoneLocalReturn() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            """
            inline fun f(ignored: () -> Any): Any {
                return ignored()
            }
            """,
            """
            fun test(): String {
                f { return "OK" };
                return "error"
            }
            """,
            "test()"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals("OK", result)
    }

    @Test
    fun testInstanceOf() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            """
            val list = listOf(1, 2, 3)
            val f: Boolean = list is List<Int>
            """,
            "val s = list is List<Int>",
            "f.toString() + s.toString()"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals("truetrue", result)
    }


    @Test
    fun testScopes() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            """fun foo(): Int {
                var t = 2 * 2
                class A(val value: Int = 5) {
                    fun bar(): Int {
                        var q = 4
                        var w = 1
                        return q + w
                    }
                }

                return A().bar() * 2
            }
            foo()
            """
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals(10, result)
    }

    @Test
    fun testEvaluateFunctionName() {
        val disposable = Disposer.newDisposable()
        val base = JsReplBase(disposable, fullRuntimeKlib)
        val lines = listOf(
            "fun evaluateScript() = 5",
            "fun foo(i: Int) = i + evaluateScript()",
            "foo(5)"
        )

        var result: Any? = null
        lines.forEach { result = compileAndEval(base, it) }

        Disposer.dispose(disposable)
        Assert.assertEquals(10, result)
    }

    private fun compileAndEval(base: JsReplBase, snippet: String): Any? {
        val jsCode = base.compiler.generateJsByReplSnippet(snippet, base.newSnippetId())
        Assert.assertTrue(base.collector.getMessage() + jsCode, base.collector.hasNotErrors())
        return base.jsEngine.eval(jsCode)
    }
}
