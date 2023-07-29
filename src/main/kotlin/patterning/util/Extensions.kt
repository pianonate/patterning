package patterning.util

import java.text.NumberFormat

fun Number.formatWithCommas(): String = NumberFormat.getInstance().format(this)

// Extension function to count decimal places in a Double
fun Double.countDecimalPlaces(): Int {
    val str = this.toString()
    val index = str.indexOf(".")
    return if (index < 0) {
        0
    } else {
        str.length - index - 1
    }
}