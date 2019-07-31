// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-106
 * PLACE: expressions, when-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' without bound value and with else branch in the last position.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String {
    when {
        value_1 == 1 -> return ""
        value_1 == 2 -> return ""
        else -> return ""
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String = when {
    value_1 == 1 -> ""
    value_1 == 2 -> ""
    else -> ""
}
