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
// RESULT: java.lang.NoSuchFieldError : Field not found: MemberDescription(ownerInternalName = binaryClass/BinaryClass, name = foo, desc = Ljava/lang/String;, isStatic = false)