package patterning.util

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import patterning.ThreeD
import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PVector

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
/*fun PGraphics.quadPlus(corners: List<PVector>, getFillColor: (Float, Float) -> Int) {

    fun setFill(corners: List<PVector>) {
        this.fill(getFillColor(corners[0].x, corners[0].y))
    }

    this.push()

    if ((abs(corners[2].y - corners[0].y) <= 2) || (abs(corners[1].x - corners[0].x) <= 2)) {
        this.noStroke()
    }

    setFill(corners)

    this.quad(
        corners[0].x, corners[0].y,
        corners[1].x, corners[1].y,
        corners[2].x, corners[2].y,
        corners[3].x, corners[3].y
    )

    this.pop()
}

fun PGraphics.boxPlus(threeD: ThreeD, depth: Float, getFillColor: (Float, Float) -> Int) {

    fun setFill(corners: List<PVector>) {
        this.fill(getFillColor(corners[0].x, corners[0].y))
    }

    fun guardStroke(corners: List<PVector>) {
        if ((abs(corners[2].y - corners[0].y) <= 2) || (abs(corners[1].x - corners[0].x) <= 2)) {
            //  this.noStroke()
        }
    }

    val (frontCorners, backCorners) = threeD.getBoxCoordsAtDepth(depth)

    this.beginShape(PConstants.QUADS)

    // Back face
    setFill(backCorners)
    backCorners.forEach { vertex(it.x, it.y) }
    guardStroke(frontCorners)

    // Sides
    for (i in 0 until 4) {
        val j = (i + 1) % 4 // Next corner index
        val sideCorners = listOf(frontCorners[i], frontCorners[j], backCorners[j], backCorners[i])

        setFill(sideCorners) // Set the fill before defining the vertices of this quad.
        guardStroke(sideCorners)

        vertex(frontCorners[i].x, frontCorners[i].y)
        vertex(frontCorners[j].x, frontCorners[j].y)
        vertex(backCorners[j].x, backCorners[j].y)
        vertex(backCorners[i].x, backCorners[i].y)
    }

    // Front face
    setFill(frontCorners)
    guardStroke(frontCorners)
    frontCorners.forEach { vertex(it.x, it.y) }

    this.endShape()
}*/


// Modified quadPlus to handle multiple quads
fun PGraphics.quadPlus(corners: List<PVector>, getFillColor: (Float, Float, Boolean) -> Int) {
    this.push()
    this.beginShape(PConstants.QUADS)

    corners.chunked(4).forEachIndexed { index, quadCorners ->
        if (quadCorners.size == 4) {

            if ((abs(corners[2].y - corners[0].y) <= 2) || (abs(corners[1].x - corners[0].x) <= 2)) {
                this.noStroke()
            }

            // if we're drawing a cube there are 6 faces chunked into 4 corners
            val applyCubeAlpha = (index > 0)
            val fillColor = getFillColor(quadCorners[0].x, quadCorners[0].y, applyCubeAlpha)
            this.fill(fillColor)


            quadCorners.forEach { vertex(it.x, it.y) }
        }
    }

    this.endShape()
    this.pop()
}

fun PGraphics.boxPlus(frontCorners:List<PVector>, threeD: ThreeD, depth: Float, getFillColor: (Float, Float, Boolean) -> Int) {
    val allCorners = mutableListOf<PVector>()

    val backCorners = threeD.getBackCornersAtDepth(depth)

    allCorners.addAll(backCorners)

    for (i in 0 until 4) {
        val j = (i + 1) % 4
        allCorners.add(frontCorners[i])
        allCorners.add(frontCorners[j])
        allCorners.add(backCorners[j])
        allCorners.add(backCorners[i])
    }

    allCorners.addAll(frontCorners)

    quadPlus(allCorners, getFillColor)

}



