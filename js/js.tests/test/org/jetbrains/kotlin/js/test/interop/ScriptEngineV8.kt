/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.interop

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8Value
import com.eclipsesource.v8.utils.V8ObjectUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import com.eclipsesource.v8.utils.V8Executor

class ScriptEngineV8 : ScriptEngine {
    companion object {
        // It's important that this is not created per test, but rather per process.
        val LIBRARY_PATH_BASE = KotlinTestUtils.tmpDirForReusableFolder("j2v8_library_path").path
    }

    override fun <T> releaseObject(t: T) {
        (t as? V8Object)?.release()
    }

    override fun prepareWorker(obj: Any) {
        if (obj !is V8Object) {
            throw Exception("InteropV8 can deal only with V8Object")
        }
        val prepare = obj.get("prepare")
        if ((prepare as V8Value).isUndefined) {
            // undefined is not a function
            prepare.release()
            return
        }
        val runtimeArray = V8Array(myRuntime)
        obj.executeFunction("prepare", runtimeArray)
        runtimeArray.release()
        prepare.release()
    }

    override fun waitForWorkersIfNeeded() {
        worker.waitForPostMessageIfNeeded()
    }

    private var savedState: List<String>? = null

    override fun restoreState() {
        val scriptBuilder = StringBuilder()

        val globalState = getGlobalPropertyNames()
        val originalState = savedState!!
        for (key in globalState) {
            if (key !in originalState) {
                scriptBuilder.append("this['$key'] = void 0;\n")
            }
        }
        evalVoid(scriptBuilder.toString())
    }

    private fun getGlobalPropertyNames(): List<String> {
        val v8Array = eval<V8Array>("Object.getOwnPropertyNames(this)")
        @Suppress("UNCHECKED_CAST") val javaArray = V8ObjectUtils.toList(v8Array) as List<String>
        v8Array.release()
        return javaArray
    }

    override fun saveState() {
        if (savedState == null) {
            savedState = getGlobalPropertyNames()
        }
    }

    private val worker = WebWorkerRunner()
    private val myRuntime: V8 = V8.createV8Runtime("global", LIBRARY_PATH_BASE).also { worker.configureWorker(it) }

    @Suppress("UNCHECKED_CAST")
    override fun <T> eval(script: String): T {
        return myRuntime.executeScript(script) as T
    }

    override fun evalVoid(script: String) {
        return myRuntime.executeVoidScript(script)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> callMethod(obj: Any, name: String, vararg args: Any?): T {
        if (obj !is V8Object) {
            throw Exception("InteropV8 can deal only with V8Object")
        }

        val runtimeArray = V8Array(myRuntime)
        val result = obj.executeFunction(name, runtimeArray) as T
        runtimeArray.release()
        return result
    }

    override fun loadFile(path: String) {
        worker.mainScriptPath = path
        myRuntime.executeVoidScript(File(path).bufferedReader().use { it.readText() }, path, 0)
    }

    override fun release() {
        worker.release()
        myRuntime.release()
    }
}

class ScriptEngineV8Lazy : ScriptEngine {
    override fun <T> eval(script: String) = engine.eval<T>(script)

    override fun waitForWorkersIfNeeded() = engine.waitForWorkersIfNeeded()

    override fun prepareWorker(testPackage: Any) = engine.prepareWorker(testPackage)

    override fun saveState() = engine.saveState()

    override fun evalVoid(script: String) = engine.evalVoid(script)

    override fun <T> callMethod(obj: Any, name: String, vararg args: Any?) = engine.callMethod<T>(obj, name, args)

    override fun loadFile(path: String) = engine.loadFile(path)

    override fun release() = engine.release()

    override fun <T> releaseObject(t: T) = engine.releaseObject(t)

    override fun restoreState() = engine.restoreState()

    private val engine by lazy { ScriptEngineV8() }
}

class WebWorkerRunner {
    lateinit var mainScriptPath: String
    private var message: Any? = null
    private var workerObject: V8Object? = null

    fun start(worker: V8Object, vararg s: String) {
        assert(workerObject == null) {
            "only one worker is supported now"
        }
        val script = File(File(mainScriptPath).parent + File.separator + s[0]).bufferedReader().use { it.readText() }
        val executor = object : V8Executor(script, true, "onmessage") {
            override fun setup(runtime: V8) {
                val array = V8Array(runtime).pushUndefined()
                runtime.add("onmessage", array)
                array.release()
                runtime.registerJavaMethod(this, "postMessage", "postMessage", arrayOf(V8Object::class.java, Any::class.java), true)
            }

            fun postMessage(worker: V8Object, o: Any) {
                synchronized(this) {
                    message = o
                    (this as Object).notifyAll()
                }
            }
        }
        worker.runtime.registerV8Executor(worker, executor)
        executor.start()
        workerObject = worker.twin()
    }

    private fun terminate(worker: V8Object) {
        worker.runtime.removeExecutor(worker)?.shutdown()
    }

    // todo: must be any js object
    fun postMessage(worker: V8Object, vararg s: String) {
        worker.runtime.getExecutor(worker)!!.postMessage(*s)
    }

    fun configureWorker(runtime: V8) {
        runtime.registerJavaMethod(this, "start", "Worker", arrayOf(V8Object::class.java, Array<String>::class.java), true)
        val worker = runtime.getObject("Worker")
        val prototype = runtime.executeObjectScript("Worker.prototype")
        prototype.registerJavaMethod(
            this, "postMessage", "postMessage",
            arrayOf(V8Object::class.java, Array<String>::class.java), true
        )
        worker.setPrototype(prototype)
        val array = V8Array(runtime).pushUndefined()
        prototype.add("onmessage", array)
        array.release()
        worker.release()
        prototype.release()
    }

    fun release() {
        workerObject?.let {
            terminate(it)
            it.release()
        }
        workerObject = null
    }

    fun waitForPostMessageIfNeeded() {
        workerObject?.let {
            val executor = it.runtime.getExecutor(it)!!
            synchronized(executor) {
                while (message == null) {
                    if (executor.hasException()) throw executor.exception
                    (executor as Object).wait()
                }
            }
            if (executor.hasException()) throw executor.exception
            val array = V8Array(it.runtime).push(message)
            it.executeVoidFunction("onmessage", array)
            array.release()
            it.release()
        }
        workerObject = null
    }
}
