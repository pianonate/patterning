package patterning.util

import kotlin.math.pow
import kotlin.math.roundToInt
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat

/**
 * Number extension functions
 */
fun Number.formatWithCommas(): String = NumberFormat.getInstance().format(this)

/**
 *  FLoat extension functions
 */
fun Float.roundToIntIfGreaterThanReference(referenceValue: Float): Float {
    return (if (referenceValue > 2)
        (this.roundToInt() - 1).toFloat()
    else
        this)
}


fun Number.hudFormatted(): String {
    if (this == 0L) return "0"
    return if (this is Int) {
        if (this < 1_000_000_000)
            this.formatWithCommas()
        else formatLargeNumber()
    } else {
        formatLargeNumber()
    }
}

private val largeNumberNames = arrayOf(
    "thousand", "million", "billion", "trillion", "quadrillion", "quintillion", "sextillion",
    "septillion", "octillion", "nonillion", "decillion", "undecillion", "duodecillion",
    "tredecillion", "quattuordecillion"
)

private fun Number.formatLargeNumber(): String {
    if (this.toLong() < 1_000_000_000L) return this.formatWithCommas()

    val bigIntegerValue = BigInteger.valueOf(this.toLong())
    val exponent = bigIntegerValue.toString().length - 1
    val index = (exponent - 3) / 3
    return when {
        index < largeNumberNames.size && index >= 0 -> {
            val divisor = BigDecimal.valueOf(10.0.pow((index * 3 + 3).toDouble()))
            val shortNumber = bigIntegerValue.toBigDecimal().divide(divisor, 1, RoundingMode.HALF_UP).toDouble()
            "${shortNumber.toInt()}.${(shortNumber % 1 * 10).roundToInt()} ${largeNumberNames[index]}"
        }

        else -> String.format("%.1e", bigIntegerValue.toBigDecimal())
    }
}

fun Long.isOne(): Boolean = this == 1L
fun Long.isZero(): Boolean = this == 0L
fun Long.isNotZero(): Boolean = this != 0L
fun Long.addOne(): Long = this + 1L