/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public actual fun Long.toString(radix: Int): String {
    checkRadix(radix)

    if (isZero()) {
        return "0"
    }

    if (isNegative()) {
        if (equalsLong(Long.MIN_VALUE)) {
            // We need to change the Long value before it can be negated, so we remove
            // the bottom-most digit in this base and then recurse to do the rest.
            val radixLong = fromInt(radix)
            val div = div(radixLong)
            val rem = div.multiply(radixLong).subtract(this).toInt()
            return div.toString(radix) + rem.toString(radix)
        } else {
            return "-${negate().toString(radix)}"
        }
    }

    // Do several (6) digits each time through the loop, so as to
    // minimize the calls to the very expensive emulated div.
    val radixToPower = fromNumber(JsMath.pow(radix.toDouble(), 6.0))

    var rem = this
    var result = ""
    while (true) {
        val remDiv = rem.div(radixToPower)
        val intval = rem.subtract(remDiv.multiply(radixToPower)).toInt()
        var digits = intval.asDynamic().toString(radix).unsafeCast<String>()

        rem = remDiv
        if (rem.isZero()) {
            return digits + result
        } else {
            while (digits.length < 6) {
                digits = "0" + digits
            }
            result = digits + result
        }
    }
}