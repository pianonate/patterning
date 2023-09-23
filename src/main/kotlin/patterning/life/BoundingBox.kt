package patterning.life

import kotlin.math.abs
import patterning.Canvas
import patterning.Theme
import patterning.ThreeD
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector

class BoundingBox(
    bounds: Bounds,
    private val canvas: Canvas,
    private val threeD: ThreeD,
    private val getPixelColor: (Float, Float) -> Int

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

    /*    private fun drawBoundingBoxEdges(corners: List<PVector>, graphics: PGraphics, canvas: Canvas) {
            val coercedCorners = corners.map { corner ->
                Pair(
                    corner.x.toInt().coerceIn(0, canvas.width.toInt()),
                    corner.y.toInt().coerceIn(0, canvas.height.toInt())
                )
            }

            val lines = listOf(
                Pair(coercedCorners[0], coercedCorners[2]),
                Pair(coercedCorners[2], coercedCorners[3]),
                Pair(coercedCorners[3], coercedCorners[1]),
                Pair(coercedCorners[1], coercedCorners[0])
            )

            for ((start, end) in lines) {
                drawPixelLine(start.first, start.second, end.first, end.second, graphics)
            }
        }*/
    private fun drawBoundingBoxEdges(corners: List<PVector>, graphics: PGraphics) {

        val lines = listOf(
            Pair(corners[0], corners[2]),
            Pair(corners[2], corners[3]),
            Pair(corners[3], corners[1]),
            Pair(corners[1], corners[0])
        )

        for ((start, end) in lines) {
            val clippedLine = cohenSutherlandClip(start.x, start.y, end.x, end.y)
            clippedLine?.let { (clippedStart, clippedEnd) ->
                drawPixelLine(
                    clippedStart.first.toInt(),
                    clippedStart.second.toInt(),
                    clippedEnd.first.toInt(),
                    clippedEnd.second.toInt(),
                    graphics
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
     */
    private fun cohenSutherlandClip(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): Pair<Pair<Float, Float>, Pair<Float, Float>>? {
        var (currentStartX, currentStartY, currentEndX, currentEndY) = listOf(startX, startY, endX, endY)
        var outCodeStart = computeOutCode(currentStartX, currentStartY)
        var outCodeEnd = computeOutCode(currentEndX, currentEndY)


        while (true) {
            when {
                outCodeStart == OutCode.INSIDE && outCodeEnd == OutCode.INSIDE -> return Pair(
                    Pair(
                        currentStartX,
                        currentStartY
                    ), Pair(currentEndX, currentEndY)
                )

                outCodeStart and outCodeEnd != OutCode.INSIDE -> return null
                else -> {
                    val currentOutCode = if (outCodeStart != OutCode.INSIDE) outCodeStart else outCodeEnd
                    val (x, y) = when (currentOutCode) {
                        OutCode.TOP -> {
                            if (currentEndY == currentStartY) {
                                return null
                            } else
                                currentStartX + (currentEndX - currentStartX) * -currentStartY / (currentEndY - currentStartY) to 0f
                        }

                        OutCode.BOTTOM -> {
                            if (currentEndY == currentStartY) {
                                return null
                            } else
                                currentStartX + (currentEndX - currentStartX) * (canvas.height - currentStartY) / (currentEndY - currentStartY) to canvas.height
                        }

                        OutCode.RIGHT -> {
                            if (currentEndX == currentStartX) {
                                return null
                            } else
                                canvas.width to currentStartY + (currentEndY - currentStartY) * (canvas.width - currentStartX) / (currentEndX - currentStartX)
                        }

                        OutCode.LEFT -> {
                            if (currentEndX == currentStartX) {
                                return null
                            } else
                                0f to currentStartY + (currentEndY - currentStartY) * -currentStartX / (currentEndX - currentStartX)
                        }

                        else -> 0f to 0f  // Should never happen
                    }

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
    }


    fun draw(graphics: PGraphics, drawCrossHair: Boolean = false) {
        with(graphics) {
            push()
            noFill()
            stroke(Theme.textColor)
            strokeWeight(Theme.strokeWeightBounds)

            val transformedBoundingBox =
                threeD.getTransformedRectCorners(left, top, this@BoundingBox.width, this@BoundingBox.height)

            drawBoundingBoxEdges(transformedBoundingBox, graphics)

            if (drawCrossHair) {
                drawCrossHair(graphics)
            }

            pop()
        }
    }


    inner class Point(var x: Float, var y: Float) {
        /*        init {
                    x = x.roundToIntIfGreaterThanReference(this@BoundingBox.canvas.zoomLevel)
                    y = y.roundToIntIfGreaterThanReference(this@BoundingBox.canvas.zoomLevel)
                }*/

        operator fun component1(): Float = x
        operator fun component2(): Float = y
    }

    fun drawPixelLine(startX: Int, startY: Int, endX: Int, endY: Int, graphics: PGraphics) {
        var (x, y) = startX to startY
        val (dx, dy) = abs(endX - startX) to abs(endY - startY)
        val (sx, sy) = Pair(if (startX < endX) 1 else -1, if (startY < endY) 1 else -1)
        var err = dx - dy

        generateSequence { Pair(x, y) }
            .takeWhile { it.first != endX || it.second != endY }
            .forEach { (px, py) ->

                val pixelColor = getPixelColor(px.toFloat(), py.toFloat())
                graphics.set(px, py, pixelColor)

                val e2 = 2 * err
                if (e2 > -dy) {
                    err -= dy
                    x += sx
                }
                if (e2 < dx) {
                    err += dx
                    y += sy
                }
            }
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

            val xLength = end.x - start.x
            val yLength = end.y - start.y

            val distance = PApplet.dist(start.x, start.y, end.x, end.y)
            val numDashes = distance / (Theme.dashedLineDashLength + Theme.dashedLineSpaceLength)

            val dxDash = xLength / numDashes / 2
            val dyDash = yLength / numDashes / 2

            with(graphics) {
                push()
                strokeWeight(Theme.strokeWeightDashedLine)

                val upperBound = (numDashes * 2).toInt()
                val initialState = Triple(start.x, start.y, true)

                (0 until upperBound).fold(initialState) { (curX, curY, draw), _ ->
                    val endX = (curX + dxDash).coerceAtMost(end.x)
                    val endY = (curY + dyDash).coerceAtMost(end.y)

                    if (draw) {
                        val (start, end) = threeD.getTransformedLineCoords(curX, curY, endX, endY)
                        val clippedLine = cohenSutherlandClip(start.x, start.y, end.x, end.y)

                        clippedLine?.let { (clippedStart, clippedEnd) ->
                            drawPixelLine(
                                clippedStart.first.toInt(),
                                clippedStart.second.toInt(),
                                clippedEnd.first.toInt(),
                                clippedEnd.second.toInt(),
                                this
                            )
                        }
                    }

                    Triple(endX, endY, !draw)
                }

                pop()
            }

        }
    }

}