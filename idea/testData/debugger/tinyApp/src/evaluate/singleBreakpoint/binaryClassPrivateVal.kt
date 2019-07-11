package binaryClassPrivateVal

import binaryClass.BinaryClass

fun main() {
    val cl = BinaryClass()
    //Breakpoint!
    val a = 5
}

// EXPRESSION: cl.foo_field
// RESULT: Unresolved reference: foo_field

// EXPRESSION: cl.foo
// RESULT: "foo": Ljava/lang/String;

// EXPRESSION: cl.delegated
// RESULT: "bar": Ljava/lang/String;

// EXPRESSION: cl.field
// RESULT: "baz": Ljava/lang/String;