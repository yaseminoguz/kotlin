// !LANGUAGE: +NewInference

/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 1
 * DESCRIPTION: Type annotations on return type with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

val x = 42

fun box(): String {
    val test = x.run {
        if (x == 42) x else return@run
    }

    System.out.println(test)
}
