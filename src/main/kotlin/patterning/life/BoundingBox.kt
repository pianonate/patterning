package patterning.life

import patterning.Canvas
import patterning.Theme
import patterning.ThreeD
import patterning.util.drawPixelLine
import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PVector

class BoundingBox(
    bounds: Bounds,
    private val canvas: Canvas,
    private val threeD: ThreeD,
    private val getFillColor: (Float, Float, Boolean) -> Int,

    ) {

    private val widthDecimal = bounds.width * canvas.zoomLevel
    private val heightDecimal = bounds.height * canvas.zoomLevel

    private val leftWithOffset = (bounds.left * canvas.zoomLevel) + canvas.offsetX
    private val topWithOffset = (bounds.top * canvas.zoomLevel) + canvas.offsetY

    val left = leftWithOffset
    val top = topWithOffset
    val right = leftWithOffset + widthDecimal
    val bottom = topWithOffset + heightDecimal

    val width = right - left
    val height = bottom - top

    // Calculate Lines
    private val horizontalLine: Line
        get() {
            val startY = topWithOffset + heightDecimal / 2f
            val endX = leftWithOffset + widthDecimal

            return Line(Point(left, startY), Point(endX, startY), threeD)
        }

    private val verticalLine: Line
        get() {

            val startX = leftWithOffset + widthDecimal / 2
            val endY = topWithOffset + heightDecimal

            return Line(Point(startX, top), Point(startX, endY), threeD)
        }

    private fun drawCrossHair(graphics: PGraphics) {
        horizontalLine.drawDashedLine(graphics)
        verticalLine.drawDashedLine(graphics)
    }

    private fun drawBoundingBoxEdges(corners: List<PVector>, graphics: PGraphics) {

        val lines = listOf(
            Pair(corners[0], corners[1]),
            Pair(corners[1], corners[2]),
            Pair(corners[2], corners[3]),
            Pair(corners[3], corners[0])
        )

        for ((start, end) in lines) {
            val clippedLine = cohenSutherlandClip(start.x, start.y, end.x, end.y)
            clippedLine?.let { (clippedStart, clippedEnd) ->
                graphics.drawPixelLine(
                    startX = clippedStart.first.toInt(),
                    startY = clippedStart.second.toInt(),
                    endX = clippedEnd.first.toInt(),
                    endY = clippedEnd.second.toInt(),
                    getFillColor = getFillColor,
                )
            }
        }
    }

    enum class OutCode(val code: Int) {
        INSIDE(0), // 0000
        LEFT(1),  // 0001
        RIGHT(2), // 0010
        BOTTOM(4),// 0100
        TOP(8);   // 1000

        infix fun and(other: OutCode): OutCode {
            return OutCode.fromCode(this.code and other.code)
        }

        infix fun or(other: OutCode): OutCode {
            return OutCode.fromCode(this.code or other.code)
        }

        companion object {
            fun fromCode(code: Int): OutCode {
                return entries.firstOrNull { it.code == code } ?: INSIDE
            }
        }
    }

    private fun computeOutCode(x: Float, y: Float): OutCode = when {
        x < 0 -> OutCode.LEFT
        x > canvas.width -> OutCode.RIGHT
        y < 0 -> OutCode.TOP
        y > canvas.height -> OutCode.BOTTOM
        else -> OutCode.INSIDE
    }


    /**
     * used to clip the lines to only draw the portion that overlaps with the screen
     *
     * there was an issue i don't care to explore further that sometimes this algo wouldn't exit the while(true)
     * so i added maxIterations for the following reason:
     *
     * Four iterations should be enough because in 2D space, a line can intersect a rectangle at most in 4 places:
     * two vertical sides and two horizontal sides.
     *
     * UPDATE: empirically i still see some lines disappearing and reappearing so i changed maxIterations to 8
     * i don't have enough of a care factor right now to debug why but it would be cool if someone could create a
     * deterministic version of this algo that doesn't require on empiricism
     *
     * In the Cohen-Sutherland algorithm, each iteration should move at least one endpoint of the line to be inside
     * the clipping rectangle or determine that the line is completely outside of it. Therefore, within 4 iterations,
     * the algorithm should be able to resolve the line's position relative to the rectangle.
     *
     * However, it's still theoretically possible to have corner cases where the line keeps bouncing
     * back and forth due to floating-point errors. So, a maximum iteration counter is a safety measure.
     */
    private fun cohenSutherlandClip(
        startX: Float, startY: Float, endX: Float, endY: Float
    ): Pair<Pair<Float, Float>, Pair<Float, Float>>? {
        var (currentStartX, currentStartY, currentEndX, currentEndY) = listOf(startX, startY, endX, endY)
        var outCodeStart = computeOutCode(currentStartX, currentStartY)
        var outCodeEnd = computeOutCode(currentEndX, currentEndY)

        var iterationCount = 0
        val maxIterations = 8 // Maximum number of iterations, set based on domain knowledge

        while (iterationCount < maxIterations) {
            iterationCount++

            when {
                outCodeStart == OutCode.INSIDE && outCodeEnd == OutCode.INSIDE -> return Pair(
                    Pair(currentStartX, currentStartY), Pair(currentEndX, currentEndY)
                )

                outCodeStart and outCodeEnd != OutCode.INSIDE -> return null
                else -> {
                    val currentOutCode = if (outCodeStart != OutCode.INSIDE) outCodeStart else outCodeEnd
                    val (x, y) = computeNewCoordinates(
                        currentStartX,
                        currentStartY,
                        currentEndX,
                        currentEndY,
                        currentOutCode
                    )

                    if (currentOutCode == outCodeStart) {
                        currentStartX = x
                        currentStartY = y
                        outCodeStart = computeOutCode(currentStartX, currentStartY)
                    } else {
                        currentEndX = x
                        currentEndY = y
                        outCodeEnd = computeOutCode(currentEndX, currentEndY)
                    }
                }
            }
        }
        return null // Fallback in case it didn't exit within the maximum allowed iterations
    }

    private fun computeNewCoordinates(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        currentOutCode: OutCode
    ): List<Float> {
        return when (currentOutCode) {
            OutCode.TOP -> {
                if (endY == startY) {
                    listOf(startX, 0f)
                } else {
                    listOf(startX + (endX - startX) * -startY / (endY - startY), 0f)
                }
            }

            OutCode.BOTTOM -> {
                if (endY == startY) {
                    listOf(startX, canvas.height)
                } else {
                    listOf(startX + (endX - startX) * (canvas.height - startY) / (endY - startY), canvas.height)
                }
            }

            OutCode.RIGHT -> {
                if (endX == startX) {
                    listOf(canvas.width, startY)
                } else {
                    listOf(canvas.width, startY + (endY - startY) * (canvas.width - startX) / (endX - startX))
                }
            }

            OutCode.LEFT -> {
                if (endX == startX) {
                    listOf(0f, startY)
                } else {
                    listOf(0f, startY + (endY - startY) * -startX / (endX - startX))
                }
            }

            else -> listOf(0f, 0f)  // Should never happen
        }
    }


    fun draw(graphics: PGraphics, drawCrossHair: Boolean = false) {
        with(graphics) {
            beginShape(PConstants.POINTS)
            noFill()
            strokeWeight(Theme.STROKE_WEIGHT_BOUNDS)

            val transformedBoundingBox =
                threeD.getTransformedCorners(left, top, this@BoundingBox.width, this@BoundingBox.height)

            drawBoundingBoxEdges(transformedBoundingBox, graphics)

            if (drawCrossHair) {
                drawCrossHair(graphics)
            }

            endShape()
        }
    }


    inner class Point(var x: Float, var y: Float) {

        operator fun component1(): Float = x
        operator fun component2(): Float = y
    }

    /**
     * fold takes an initial value and a lambda. it applies the lambda cumulatively to the items
     * in the collection, using the initial value as the starting point.  in each iteration, the
     * lambda function gets two arguments: the accumulated value from the previous steps and
     * the current item in the collection.  the function returns a new accumulated value, which is
     * then used in the next iteration
     *
     * allows ut avoid using mutable state.
     *
     * interesting
     */
    inner class Line(private val start: Point, private val end: Point, private val threeD: ThreeD) {

        fun drawDashedLine(graphics: PGraphics) {
            with(graphics) {
                strokeWeight(Theme.STROKE_WEIGHT_DASHED_LINES)

                val (transformedStart, transformedEnd) = threeD.getTransformedLineCoords(start.x, start.y, end.x, end.y)
                val clippedLine =
                    cohenSutherlandClip(transformedStart.x, transformedStart.y, transformedEnd.x, transformedEnd.y)
                        ?: return

                val (clippedStart, clippedEnd) = clippedLine
                drawPixelLine(
                    clippedStart.first.toInt(),
                    clippedStart.second.toInt(),
                    clippedEnd.first.toInt(),
                    clippedEnd.second.toInt(),
                    listOf(Theme.DASHED_LINE_DASH_LENGTH, Theme.DASHED_LINE_SPACE_LENGTH),
                    getFillColor,
                )
            }
        }
    }

}