fun box() {
    lookAtMe {
        42
    }
}

inline fun lookAtMe(f: () -> Int) {
    val a = 21
    a + f()
}

// IGNORE_BACKEND: JVM_IR
// 2 14 15 65100 3 16 5 8 9 10