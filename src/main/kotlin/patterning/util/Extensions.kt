package patterning.util

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import patterning.ThreeD
import processing.core.PApplet
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

/**
 * PGraphics extensions

 * PGraphics extension functions as a helper for drawing the quads...
 *
 * we pass in a list of PVectors 4 at time so chunked can work to draw boxes correctly
 *
 * no stroke handling - as we are rotating, if we're large enough to have a stroke on the box outline
 * we _don't_ want to show it because it will eliminate too many frames as it gets edge on to
 * the camera - this is because the strokeColor is set to the background color
 * so that's all we see - is background color
 *
 * so edge on, we turn off the stroke temporarily
 *
 * both quadPlus and boxPlus require that beginShape and endShape are called around them or you'll experience bugs
 * i could go through a lot of trouble to avoid requiring the caller to know this by only calling beginShape if it hasn't
 * already been called but i don't know how I'd know when to call endshape...
 */
fun PGraphics.quadPlus(corners: List<PVector>, getFillColor: (Float, Float, Boolean) -> Int) {
    this.beginShape(PConstants.QUADS)

    corners.chunked(4).forEachIndexed { index, quadCorners ->
        if (quadCorners.size == 4) {

            if ((abs(corners[2].y - corners[0].y) <= 2) || (abs(corners[1].x - corners[0].x) <= 2)) {
                this.noStroke()
            }

            // if we're drawing a cube there are 6 faces chunked into 4 corners
            // don't let apply the cube alpha to the first face drawn - which is the background face
            // so one of the faces will always be full strength and the rest will be semi-transparent
            // because it looks cool
            val applyCubeAlpha = (index > 0)
            val fillColor = getFillColor(quadCorners[0].x, quadCorners[0].y, applyCubeAlpha)
            this.fill(fillColor)


            quadCorners.forEach { vertex(it.x, it.y) }
        }
    }

    this.endShape()
}

fun PGraphics.boxPlus(
    frontCorners: List<PVector>,
    threeD: ThreeD,
    depth: Float,
    getFillColor: (Float, Float, Boolean) -> Int
) {
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

fun PGraphics.drawPixelLine(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    dashPattern: List<Int> = emptyList(),
    getFillColor: (Float, Float, Boolean) -> Int,
) {
    var (posX, posY) = startX to startY
    val (deltaX, deltaY) = abs(endX - startX) to abs(endY - startY)
    val (xDirection, yDirection) = Pair(if (startX < endX) 1 else -1, if (startY < endY) 1 else -1)
    var err = deltaX - deltaY

    var patternIdx = 0
    var stepCounter = 0

    fun drawStep(px: Float, py: Float) {
        this.stroke(getFillColor(px, py, false))

        if (dashPattern.isEmpty()) {
            this.vertex(px, py)
        } else {
            if (dashPattern[patternIdx] > stepCounter && patternIdx % 2 == 0) {
                this.vertex(px, py)
            }
            stepCounter++
            if (stepCounter >= dashPattern[patternIdx]) {
                stepCounter = 0
                patternIdx = (patternIdx + 1) % dashPattern.size
            }
        }
    }

    generateSequence { Pair(posX, posY) }
        .takeWhile { it.first != endX || it.second != endY }
        .forEach { (px, py) ->
            drawStep(px.toFloat(), py.toFloat())

            val doubleError = 2 * err
            if (doubleError > -deltaY) {
                err -= deltaY
                posX += xDirection
            }
            if (doubleError < deltaX) {
                err += deltaX
                posY += yDirection
            }
        }
}

/**
 * PApplet extension
 */

val PApplet.mouseVector
    get() =  PVector(mouseX.toFloat(), mouseY.toFloat())