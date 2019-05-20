// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNCHECKED_CAST
// !WITH_NEW_INFERENCE
// SKIP_TXT
// Issue: KT-20849

fun <T>test_1(x: T): T = null as T
fun <T>test_2(x: () -> T): T = null as T

fun case_1() {
    null?.run { return }
    null!!.<!UNREACHABLE_CODE!>run { throw Exception() }<!>
}

fun case_2() {
    test_1 { null!! }
    test_2 { null!! }
}

fun case_3() {
    test_1 { throw Exception() }
    test_2 { throw Exception() }
}

fun case_6() {
    null!!
}

fun case_7(x: Boolean?) {
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (x) {
        true -> throw Exception()
        false -> throw Exception()
        null -> throw Exception()
    }<!>
}

fun <T> something(): T = Any() as T

class Context<T>

fun <T> Any.decodeIn(typeFrom: Context<in T>): T = something()

fun <T> Any?.decodeOut1(typeFrom: Context<out T>): T {
    return this?.<!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>decodeIn<!>(typeFrom) ?: <!OI;TYPE_MISMATCH!>kotlin.Unit<!>
}

fun <T> Any.decodeOut2(typeFrom: Context<out T>): T {
    <!OI;UNREACHABLE_CODE!>val x: Nothing =<!> this.decodeIn(typeFrom)
}

fun <T> Any.decodeOut3(typeFrom: Context<out T>): T {
    <!OI;UNREACHABLE_CODE!>val x =<!> this.<!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>decodeIn<!>(typeFrom)
}

fun <T> Any.decodeOut4(typeFrom: Context<out T>): T {
    <!OI;UNREACHABLE_CODE!>val x: Any =<!> this.<!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>decodeIn<!>(typeFrom)
}

class TrieNode<out E> {
    companion object {
        internal val EMPTY = TrieNode<Nothing>()
    }
}
class PersistentHashSet<out E>(root: TrieNode<E>) {
    companion object {
        internal val EMPTY = PersistentHashSet(TrieNode.EMPTY)
    }
}
