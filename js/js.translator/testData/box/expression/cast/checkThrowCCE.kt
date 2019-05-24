// EXPECTED_REACHABLE_NODES: 1217

// Bad unsafe cast
@file:SkipRuntimeTypeChecks

package foo

import kotlin.reflect.*

fun box(): String {
    try {
        "fail".unsafeCast<KClass<*>>().js
        return "fail try"
    } catch (cce: ClassCastException) {
        return "OK"
    }

    return "fail common"
}