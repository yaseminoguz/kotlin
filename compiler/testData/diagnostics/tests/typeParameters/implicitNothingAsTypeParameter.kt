// !DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST
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

fun <T> Any?.decodeOut(typeFrom: Context<out T>): T {
    return this?.<!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>decodeIn<!>(typeFrom) ?: <!UNRESOLVED_REFERENCE!>println<!>("")
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
