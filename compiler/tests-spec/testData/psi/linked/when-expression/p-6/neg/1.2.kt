/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-85
 * PLACE: when-expression -> paragraph 6 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and empty 'when condition'.
 */

fun case_1() {
    when (value) {
        -> { println(1) }
    }
}