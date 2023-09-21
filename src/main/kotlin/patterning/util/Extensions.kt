package patterning.util

import kotlin.math.ceil
import kotlin.math.ln
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

// i was wondering why empirically we needed a PRECISION_BUFFER to add to the precision
// now that i'm thinking about it, this is probably the required precision for a float
// which is what the cell.cellSize is - especially for really small numbers
// without it we'd be off by only looking at the integer part of the largest dimension
const val PRECISION_BUFFER = 10

fun FlexibleInteger.minPrecisionForDrawing(): Int {
    return when (val number = this.value) { // Access the value property of FlexibleInteger
        0 -> PRECISION_BUFFER
        is Int, is Long -> ceil(ln(number.toDouble()) / ln(10.0)).toInt() + PRECISION_BUFFER
        is BigInteger -> BigDecimal(number).precision() + PRECISION_BUFFER
        else -> throw IllegalArgumentException("Unsupported number type")
    }
}

fun Long.minPrecisionForDrawing(): Int = ceil(ln(this.toDouble()) / ln(10.0)).toInt() + PRECISION_BUFFER

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

private fun FlexibleInteger.formatLargeNumber(): String {

    val bigIntegerValue = if (value is BigInteger) value else BigInteger.valueOf(value.toLong())
    val exponent = bigIntegerValue.toString().length - 1
    val index = (exponent - 3) / 3
    return when {
        index < largeNumberNames.size -> {
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
