package patterning.life

import patterning.Canvas
import patterning.Theme
import patterning.util.roundToIntIfGreaterThanReference
import processing.core.PApplet
import processing.core.PGraphics

class BoundingBox(bounds: Bounds, private val canvas: Canvas) {

    private fun Float.offset(offset: Float): Float = this.plus(offset)

    // take the bounds parameter, multiply times zoom level - intermediate values need to be scaled
    private fun Long.zoomAndScale(): Float = this * canvas.zoomLevelAsFloat

    private fun Long.zoomOffsetAndScale(offset: Float): Float =
        this.zoomAndScale().offset(offset)

    private val leftWithOffset = bounds.left.zoomOffsetAndScale(canvas.offsetX.toFloat())
    private val topWithOffset = bounds.top.zoomOffsetAndScale(canvas.offsetY.toFloat())

    private val widthDecimal = bounds.width.zoomAndScale()
    private val heightDecimal = bounds.height.zoomAndScale()

    private fun Float.coerceLeftAndTopToFloat(): Float =
        if (this < 0f) NEGATIVE_OFFSCREEN else this

    private fun Float.coerceRightAndBottomToFloat(size: Float, canvasSize: Float): Float {

        return if (this + size > canvasSize)
            canvasSize + POSITIVE_OFFSCREEN
        else
            (this + size)
    }

    // if the actual drawing calculates to be larger than a float then the call to rect will fail
    // so - coerce boundaries to be drawable with floats
    val left = leftWithOffset.coerceLeftAndTopToFloat()
    val top = topWithOffset.coerceLeftAndTopToFloat()
    val right = leftWithOffset.coerceRightAndBottomToFloat(widthDecimal, canvas.width.toFloat())
    val bottom = topWithOffset.coerceRightAndBottomToFloat(heightDecimal, canvas.height.toFloat())


    private fun Float.fitWidthAndHeight(opposite: Float): Float =
        if (this == NEGATIVE_OFFSCREEN) (opposite + POSITIVE_OFFSCREEN) else (opposite - this)

    val width = left.fitWidthAndHeight(right)
    val height = top.fitWidthAndHeight(bottom)


    private fun startPos(
        orthogonalStartWithOffset: Float,
        orthogonalDimensionSize: Float,
        orthogonalCanvasSize: Float
    ): Float {
        // divide by half cell size because the draw algorithm ends up with cells at center of universe right now...
        val halfDimension = orthogonalDimensionSize / 2f
        val startPosDecimal = orthogonalStartWithOffset + halfDimension

        return when {
            startPosDecimal < 0f -> NEGATIVE_OFFSCREEN
            startPosDecimal > orthogonalCanvasSize -> orthogonalCanvasSize + POSITIVE_OFFSCREEN
            else -> startPosDecimal
        }
    }

    private fun endPos(
        startWithOffset: Float,
        dimensionSize: Float,
        canvasSize: Float
    ): Float {
        val endDecimal = startWithOffset + dimensionSize

        return if (endDecimal > canvasSize) canvasSize + POSITIVE_OFFSCREEN else endDecimal
    }

    // Calculate Lines
    private val horizontalLine: Line
        get() {
            val startY = startPos(
                orthogonalStartWithOffset = topWithOffset,
                orthogonalDimensionSize = heightDecimal,
                orthogonalCanvasSize = canvas.height.toFloat()
            )

            val endX = endPos(
                startWithOffset = leftWithOffset,
                dimensionSize = widthDecimal,
                canvasSize = canvas.width.toFloat()
            )

            return Line(Point(left, startY), Point(endX, startY))
        }

    private val verticalLine: Line
        get() {

            val startX = startPos(
                orthogonalStartWithOffset = leftWithOffset,
                orthogonalDimensionSize = widthDecimal,
                orthogonalCanvasSize = canvas.width.toFloat()
            )

            val endY = endPos(
                startWithOffset = topWithOffset,
                dimensionSize = heightDecimal,
                canvasSize = canvas.height.toFloat()
            )

            return Line(Point(startX, top), Point(startX, endY))
        }

    private fun drawCrossHair(graphics: PGraphics) {
        horizontalLine.drawDashedLine(graphics)
        verticalLine.drawDashedLine(graphics)
    }

    fun draw(graphics: PGraphics, drawCrossHair: Boolean = false) {

        with(graphics) {
            push()
            noFill()
            stroke(Theme.textColor)
            strokeWeight(Theme.strokeWeightBounds)
            rect(left, top, this@BoundingBox.width, this@BoundingBox.height)

            if (drawCrossHair) {
                drawCrossHair(graphics)
            }

            pop()
        }
    }

    private inner class Point(var x: Float, var y: Float) {
        init {
            x = x.roundToIntIfGreaterThanReference(this@BoundingBox.canvas.zoomLevelAsFloat)
            y = y.roundToIntIfGreaterThanReference(this@BoundingBox.canvas.zoomLevelAsFloat)
        }

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
    private data class Line(val start: Point, val end: Point) {

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
                        line(curX, curY, endX, endY)
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