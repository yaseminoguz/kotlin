fun foo() {
    bar {
        nop()
        baz()
    }
}

inline fun bar(f: () -> Unit) {
    nop()
    f()
}

inline fun baz() {
    nop()
}

fun nop() {}

// IGNORE_BACKEND: JVM_IR
// 2 21 22 65100 3 4 26 27 5 28 6 9 10 11 14 15 17