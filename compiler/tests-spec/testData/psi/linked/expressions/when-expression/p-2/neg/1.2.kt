/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-106
 * PLACE: expressions, when-expression -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' without bound value and missed 'when condition'.
 */

fun case_1() {
    when {
        -> { println(1) }
    }
}