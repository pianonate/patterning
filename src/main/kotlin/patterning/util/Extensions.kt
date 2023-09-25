package patterning.util

import kotlin.math.ln
import kotlin.math.pow

/**
 * Number extension functions
 */
fun Number.formatWithCommas(): String {
    val parts = this.toString().split(".")
    val intPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
    return if (parts.size > 1 && parts[1] != "0") "$intPart.${parts[1]}" else intPart
}


fun Number.hudFormatted(): String {
    val numAsDouble = this.toDouble()
    return when {
        numAsDouble == 0.0 -> "0"
        numAsDouble in 0.0..<1.0 -> "%.3f".format(numAsDouble)
        numAsDouble < 1_000_000_000L -> this.formatWithCommas()
        else -> formatLargeNumber()
    }
}


private val largeNumberNames = arrayOf(
    "thousand", "million", "billion", "trillion", "quadrillion", "quintillion", "sextillion",
    "septillion", "octillion", "nonillion", "decillion", "undecillion", "duodecillion",
    "tredecillion", "quattuordecillion"
)

private fun Number.formatLargeNumber(): String {
    val num = this.toLong()
    if (num < 1_000_000L) return this.formatWithCommas()

    val exponent = (ln(num.toDouble()) / ln(10.0)).toInt()
    val index = (exponent - 3) / 3

    return if (index < largeNumberNames.size && index >= 0) {
        val divisor = 10.0.pow((index * 3 + 3).toDouble())
        val shortNumber = (num / divisor).toInt()
        val remainder = ((num % divisor) / (divisor / 10)).toInt()
        "$shortNumber.$remainder ${largeNumberNames[index]}"
    } else {
        String.format("%.1e", num.toDouble())
    }
}

/**
 * Int extensions
 */
fun Int.applyAlpha(alphaValue: Int): Int {
    return (this and 0xFFFFFF) or (alphaValue shl 24)
}

/**
 * Long extension functions
 */
fun Long.isOne(): Boolean = this == 1L
fun Long.isZero(): Boolean = this == 0L
fun Long.isNotZero(): Boolean = this != 0L
fun Long.addOne(): Long = this + 1L



