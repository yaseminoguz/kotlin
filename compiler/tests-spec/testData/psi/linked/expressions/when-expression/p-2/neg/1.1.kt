/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-106
 * PLACE: expressions, when-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' without bound value and missed control structure body.
 */

fun case_1() {
    when {
        value == 1 ->
    }
}