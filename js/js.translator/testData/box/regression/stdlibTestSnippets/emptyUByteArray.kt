// EXPECTED_REACHABLE_NODES: 1662
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS

fun box(): String {
    val x = ubyteArrayOf()
    if (x.size == 0)
        return "OK"
    return "Fail"
}
