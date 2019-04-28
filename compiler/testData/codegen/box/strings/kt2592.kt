// IGNORE_BACKEND: JS_IR
// Fix String() constructor

fun box(): String {
    String()
    return String() + "OK" + String()
}
