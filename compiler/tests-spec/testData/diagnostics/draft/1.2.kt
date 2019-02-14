fun case_1() {
    val mm: MutableList<in Number> = mutableListOf()
    val nn: MutableList<in Number> = mutableListOf()

    val cc = if (true) mm else nn

    cc // smart cast from MutableList<out Any?> to MutableList<in Number>
}

interface A<in T>

fun case_2() {
    val mm: A<in Int> = object : A<Number> {}
    val nn: A<in Number> = object : A<Number> {}

    val cc = if (true) mm else nn

    cc // A<[in] Int>
}

// declaration-site invariance
interface A<T>

fun case_3() {
    val mm: A<in Int> = object : A<Number> {}
    val nn: A<in Number> = object : A<Number> {}

    val cc = if (true) mm else nn

    cc // A<out Any?>
}