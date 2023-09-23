package patterning.util

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import processing.core.PGraphics
import processing.core.PVector

/**
 *  FLoat extension functions
 */
fun Float.roundToIntIfGreaterThanReference(referenceValue: Float): Float {
    return (if (referenceValue > 2)
        (this.roundToInt() - 1).toFloat()
    else
        this)
}

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
        numAsDouble in 0.0..1.0 -> "%.3f".format(numAsDouble)
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
 * Long extension functions
 */
fun Long.isOne(): Boolean = this == 1L
fun Long.isZero(): Boolean = this == 0L
fun Long.isNotZero(): Boolean = this != 0L
fun Long.addOne(): Long = this + 1L

/**
 * PGraphics extension functions
 */

/**
 * as we are rotating, if we're large enough to have a stroke on the box outline
 * we _don't_ want to show it because it will eliminate too many frames as it gets edge on to
 * the camera - this is because the strokeColor is set to the background color
 * so that's all we see - is background color
 *
 * so edge on, we turn off the stroke temporarily
 */
fun PGraphics.quadPlus(corners: List<PVector>) {

    this.push()

    if ((abs(corners[2].y - corners[0].y) <= 2) || (abs(corners[1].x - corners[0].x) <= 2)) {
        this.noStroke()
    }

    this.quad(
        corners[0].x, corners[0].y,
        corners[2].x, corners[2].y,
        corners[3].x, corners[3].y,
        corners[1].x, corners[1].y
    )

    this.pop()
}


