// FILE: K1.kt

interface A<T> {
    fun foo(x: T) {}
}

open class KFirst :  {
    fun foo() {
    }
}

// FILE: J1.java
public class J1 extends KFirst {
    void baz() {}
}

// FILE: K2.kt
class K2: J1() {
    fun bar() {
        foo()
        baz()
    }
}
