// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 4 -> sentence 2
 * NUMBER: 21
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality) using if expression and simple types.
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val a: Inv<Inv<Number>> = Inv(Inv(42))
    val b: Inv<Inv<Int>> = Inv(Inv(42))
    val c = if (true) a else b

    if (c.prop_3.prop_3 is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>.equals(1)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>.equals(1)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>.equals(1)
    }
}

// TESTCASE NUMBER: 2
fun case_2() {
    val a: Inv<out Inv<Number>> = Inv(Inv(42))
    val b: Inv<out Inv<out Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>.equals(1)
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756
 */
fun case_3() {
    val a: Inv<out Number> = Inv(42)
    val b: Inv<out Nothing?> = Inv(null)
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>c.prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>c.prop_3<!>?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
}

// TESTCASE NUMBER: 4
fun case_4() {
    val a: Inv<out Inv<Number>> = Inv(Inv(42))
    val b: Inv<out Inv<Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>.equals(1)
}

// TESTCASE NUMBER: 5
fun case_5() {
    val a: Inv<Inv<out Number>> = Inv(Inv(42))
    val b: Inv<out Inv<Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>.equals(1)
}

// TESTCASE NUMBER: 6
fun case_6() {
    val a: Inv<Inv< Number>> = Inv(Inv(42))
    val b: Inv<out Inv<out Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>.equals(1)
}

// TESTCASE NUMBER: 7
fun case_7() {
    val a: Inv<out Inv<out Number>> = Inv(Inv(42))
    val b: Inv<out Inv<out Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c.prop_3<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3.prop_3<!>.equals(1)
}

// TESTCASE NUMBER: 8
fun case_8() {
    val a: Inv<out Inv<out Number?>> = Inv(Inv(42))
    val b: Inv<out Inv<out Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>")!>c.prop_3<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c.prop_3.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c.prop_3.prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
}

// TESTCASE NUMBER: 9
fun case_9() {
    val a: Inv<out Inv<out Number>> = Inv(Inv(42))
    val b: Inv<out Inv<out Int>?> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>?>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>?>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>?")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>?")!>c.prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!>.equals(1)
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a: Inv<out Inv<out Number>> = Inv(Inv(42))
    val b: Inv<Inv<out Int>?> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>?>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>?>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>?")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>?")!>c.prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!>.equals(1)
}

// TESTCASE NUMBER: 11
fun case_11() {
    val a: Inv<out Inv<out Number>> = Inv(Inv(42))
    val b: Inv<Inv<out Int?>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>")!>c.prop_3<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c.prop_3.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c.prop_3.prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
}

// TESTCASE NUMBER: 12
fun case_12() {
    val a: Inv<out Inv<out Number>?> = Inv(Inv(42))
    val b: Inv<Inv<out Int?>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c.prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
}

// TESTCASE NUMBER: 13
fun case_13() {
    val a: Inv<out Inv<Number>?> = Inv(Inv(42))
    val b: Inv<out Inv<out Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>?>")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number>?>")!>c<!>.equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>?")!>c.prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>?")!>c.prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>c.prop_3<!UNSAFE_CALL!>.<!>prop_3<!>.equals(1)
}

// TESTCASE NUMBER: 14
fun case_14() {
    val a: Inv<out Inv<Number>?>? = Inv(Inv(42))
    val b: Inv<out Inv<out Int?>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>?")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>?")!>c<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c<!UNSAFE_CALL!>.<!>prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c<!UNSAFE_CALL!>.<!>prop_3<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c<!UNSAFE_CALL!>.<!>prop_3<!UNSAFE_CALL!>.<!>prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
}

// TESTCASE NUMBER: 15
fun case_15() {
    val a: Inv<Inv<Number?>?>? = Inv(Inv(42))
    val b: Inv<out Inv<out Int>> = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>?")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>?")!>c<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c<!UNSAFE_CALL!>.<!>prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c<!UNSAFE_CALL!>.<!>prop_3<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c<!UNSAFE_CALL!>.<!>prop_3<!UNSAFE_CALL!>.<!>prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
}

// TESTCASE NUMBER: 16
fun case_16() {
    val a: Inv<Inv<Number>> = Inv(Inv(42))
    val b: Inv<out Inv<out Int?>?>? = Inv(Inv(42))
    val c = if (true) a else b

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>?")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out kotlin.Number?>?>?")!>c<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number?>?")!>c<!UNSAFE_CALL!>.<!>prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c<!UNSAFE_CALL!>.<!>prop_3<!UNSAFE_CALL!>.<!>prop_3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>c<!UNSAFE_CALL!>.<!>prop_3<!UNSAFE_CALL!>.<!>prop_3<!><!UNSAFE_CALL!>.<!>equals(1)
}
