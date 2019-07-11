package binaryClass

class BinaryClass {
    private val foo: String get() = "foo"

    private val delegated by lazy { "bar" }

    private val field = "baz"
}