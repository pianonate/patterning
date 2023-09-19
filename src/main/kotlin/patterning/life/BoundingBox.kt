package patterning.life

import patterning.Canvas
import patterning.Theme
import patterning.util.FlexibleDecimal
import patterning.util.FlexibleInteger
import patterning.util.roundToIntIfGreaterThanReference
import processing.core.PApplet
import processing.core.PGraphics

class BoundingBox(bounds: Bounds, private val canvas: Canvas) {

    private val mc = canvas.mc
    private val zoomLevel = canvas.zoomLevel.scale(mc)

    private fun FlexibleDecimal.scale(): FlexibleDecimal = this.scale(mc)
    private fun FlexibleInteger.scale(): FlexibleDecimal = this.toFlexibleDecimal().scale(mc)
    private fun FlexibleDecimal.zoom(): FlexibleDecimal = this.multiply(zoomLevel, canvas.mc)
    private fun FlexibleDecimal.offset(offset: FlexibleDecimal): FlexibleDecimal = this.plus(offset)

    // take the bounds parameter, multiply times zoom level - intermediate values need to be scaled
    private fun FlexibleInteger.zoomAndScale(): FlexibleDecimal = this.scale().zoom().scale()

    // now offset it - again, intermediate values need to be scaled
    private fun FlexibleInteger.zoomOffsetAndScale(offset: FlexibleDecimal): FlexibleDecimal =
        this.zoomAndScale().offset(offset.scale()).scale()

    private val leftWithOffset = bounds.left.zoomOffsetAndScale(canvas.offsetX)
    private val topWithOffset = bounds.top.zoomOffsetAndScale(canvas.offsetY)

    private val widthDecimal = bounds.width.zoomAndScale()
    private val heightDecimal = bounds.height.zoomAndScale()

    private fun FlexibleDecimal.coerceLeftAndTopToFloat(): Float =
        if (this < FlexibleDecimal.ZERO) NEGATIVE_OFFSCREEN else this.toFloat()

    private fun FlexibleDecimal.coerceRightAndBottomToFloat(size: FlexibleDecimal, canvasSize: FlexibleDecimal): Float {

        return if (this + size > canvasSize)
            canvasSize.toFloat() + POSITIVE_OFFSCREEN
        else
            (this + size).toFloat()
    }

    // if the actual drawing calculates to be larger than a float then the call to rect will fail
    // so - coerce boundaries to be drawable with floats
    val left = leftWithOffset.coerceLeftAndTopToFloat()
    val top = topWithOffset.coerceLeftAndTopToFloat()
    val right = leftWithOffset.coerceRightAndBottomToFloat(widthDecimal, canvas.width)
    val bottom = topWithOffset.coerceRightAndBottomToFloat(heightDecimal, canvas.height)


    private fun Float.fitWidthAndHeight(opposite: Float): Float =
        if (this == NEGATIVE_OFFSCREEN) (opposite + POSITIVE_OFFSCREEN) else (opposite - this)

    val width = left.fitWidthAndHeight(right)
    val height = top.fitWidthAndHeight(bottom)


    private fun startPos(
        orthogonalStartWithOffset: FlexibleDecimal,
        orthogonalDimensionSize: FlexibleDecimal,
        orthogonalCanvasSize: FlexibleDecimal
    ): Float {
        // divide by half cell size because the draw algorithm ends up with cells at center of universe right now...
        val halfDimension = orthogonalDimensionSize.divide(FlexibleDecimal.TWO, canvas.mc)
        val startPosDecimal = orthogonalStartWithOffset + halfDimension

        return when {
            startPosDecimal < FlexibleDecimal.ZERO -> NEGATIVE_OFFSCREEN
            startPosDecimal > orthogonalCanvasSize -> orthogonalCanvasSize.toFloat() + POSITIVE_OFFSCREEN
            else -> startPosDecimal.toFloat()
        }
    }

    private fun endPos(startWithOffset: FlexibleDecimal, dimensionSize: FlexibleDecimal, canvasSize: FlexibleDecimal): Float {
        val endDecimal = startWithOffset + dimensionSize

        return if (endDecimal > canvasSize) canvasSize.toFloat() + POSITIVE_OFFSCREEN else endDecimal.toFloat()
    }

    // Calculate Lines
    private val horizontalLine: Line
        get() {
            val startY = startPos(orthogonalStartWithOffset = topWithOffset,
                orthogonalDimensionSize = heightDecimal,
                orthogonalCanvasSize = canvas.height)

            val endX = endPos(startWithOffset = leftWithOffset,
                dimensionSize = widthDecimal,
                canvasSize = canvas.width)

            return Line(Point(left, startY), Point(endX, startY))
        }

    private val verticalLine: Line
        get() {

            val startX = startPos(orthogonalStartWithOffset = leftWithOffset,
                orthogonalDimensionSize = widthDecimal,
                orthogonalCanvasSize = canvas.width)

            val endY = endPos(startWithOffset = topWithOffset,
                dimensionSize = heightDecimal,
                canvasSize = canvas.height)

            return Line(Point(startX, top), Point(startX, endY))
        }

    private fun drawCrossHair(graphics: PGraphics) {
        horizontalLine.drawDashedLine(graphics)
        verticalLine.drawDashedLine(graphics)
    }

    fun draw(graphics: PGraphics, drawCrosshair: Boolean = false) {

        with(graphics) {
            push()
            noFill()
            stroke(Theme.textColor)
            strokeWeight(Theme.strokeWeightBounds)
            rect(left, top, this@BoundingBox.width, this@BoundingBox.height)

            if (drawCrosshair) {
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
    }

    private data class Line(val start: Point, val end: Point) {

        fun drawDashedLine(graphics: PGraphics) {

            val x1 = start.x
            val y1 = start.y
            val x2 = end.x
            val y2 = end.y

            val distance = PApplet.dist(x1, y1, x2, y2)
            val numDashes = distance / (Theme.dashedLineDashLength + Theme.dashedLineSpaceLength)

            var draw = true
            var x = x1
            var y = y1
            graphics.pushStyle()
            graphics.strokeWeight(Theme.strokeWeightDashedLine)
            for (i in 0 until (numDashes * 2).toInt()) {
                if (draw) {
                    // We limit the end of the dash to be at maximum the final point
                    val dxDash = (x2 - x1) / numDashes / 2
                    val dyDash = (y2 - y1) / numDashes / 2
                    val endX = (x + dxDash).coerceAtMost(x2)
                    val endY = (y + dyDash).coerceAtMost(y2)
                    graphics.line(x, y, endX, endY)
                    x = endX
                    y = endY
                } else {
                    val dxSpace = (x2 - x1) / numDashes / 2
                    val dySpace = (y2 - y1) / numDashes / 2
                    x += dxSpace
                    y += dySpace
                }
                draw = !draw
            }
            graphics.popStyle()
        }
    }

    companion object {

        // we draw the box just a bit off screen so it won't be visible
        // but if the box is more than a pixel, we need to push it further offscreen
        // since we're using a Theme constant we can change we have to account for it
        private const val POSITIVE_OFFSCREEN = Theme.strokeWeightBounds
        private const val NEGATIVE_OFFSCREEN = -POSITIVE_OFFSCREEN
    }
}