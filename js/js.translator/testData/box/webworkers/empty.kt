// TARGET_BACKEND: JS_IR

import kotlin.worker.*

fun box(): String {
    worker {
        // Just a smoke test: no exception should be thrown
    }
    return "OK"
}
