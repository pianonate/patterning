package patterning.life

import kotlin.math.abs
import patterning.Canvas
import patterning.Theme
import patterning.ThreeD
import patterning.util.quadPlus3D
import patterning.util.roundToIntIfGreaterThanReference
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector

class BoundingBox(
    bounds: Bounds,
    private val canvas: Canvas,
    private val threeD: ThreeD,
    private val setFillLambda: (Float, Float) -> Unit
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

    fun drawBoundingBoxEdges(corners: List<PVector>, graphics: PGraphics, canvas: Canvas) {
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
    }


    fun draw(graphics: PGraphics, drawCrossHair: Boolean = false) {

        with(graphics) {
            push()
            noFill()
            stroke(Theme.textColor)
            strokeWeight(Theme.strokeWeightBounds)

            val transformedBoundingBox =
                threeD.getTransformedRectCorners(left, top, this@BoundingBox.width, this@BoundingBox.height)

            //quadPlus3D(transformedBoundingBox)
            drawBoundingBoxEdges(transformedBoundingBox, graphics, canvas)

            if (drawCrossHair) {
                drawCrossHair(graphics)
            }

            pop()
        }
    }

    inner class Point(var x: Float, var y: Float) {
        init {
            x = x.roundToIntIfGreaterThanReference(this@BoundingBox.canvas.zoomLevel)
            y = y.roundToIntIfGreaterThanReference(this@BoundingBox.canvas.zoomLevel)
        }

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
                setFillLambda(px.toFloat(), py.toFloat())
                // Replace with your setFill function or any other pixel setting code.
                graphics.point(px.toFloat(), py.toFloat())

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
    inner class Line(val start: Point, val end: Point, val threeD: ThreeD) {

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

                    val transformedCoords = threeD.getTransformedLineCoords(curX, curY, endX, endY)


                    if (draw) {
                        drawPixelLine(
                            transformedCoords.first.x.toInt(),
                            transformedCoords.first.y.toInt(),
                            transformedCoords.second.x.toInt(),
                            transformedCoords.second.y.toInt(),
                            this
                        )
                    }

                    Triple(endX, endY, !draw)
                }

                pop()
            }

        }
    }

    companion object {

        // we draw the box just a bit offscreen so it won't be visible
        // but if the box is more than a pixel, we need to push it further offscreen
        // since we're using a Theme constant we can change we have to account for it
        private const val POSITIVE_OFFSCREEN = Theme.strokeWeightBounds
        private const val NEGATIVE_OFFSCREEN = -POSITIVE_OFFSCREEN
    }
}