/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.worker

// TODO: use kotlin-native like workers
class WebWorker(private val worker: Any) {
    fun postMessage(message: Any) {
        worker.asDynamic().postMessage(message)
    }

    fun onmessage(c: (Any) -> Unit) {
        worker.asDynamic().onmessage = c
    }
}

// TODO: Return Future
fun worker(c: (Any) -> Any): WebWorker {
    throw UnsupportedOperationException("Implemented as intrinsic")
}

internal external fun postMessage(message: Any)
