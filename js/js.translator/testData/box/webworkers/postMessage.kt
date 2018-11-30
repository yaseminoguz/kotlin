// TARGET_BACKEND: JS_IR

import kotlin.worker.*

var res = "FAIL"

fun prepare() {
    val worker = worker {
        "OK"
    }
    worker.onmessage {
        res = it as String
    }
    worker.postMessage("Ping")
}

fun box(): String {
    return res
}
