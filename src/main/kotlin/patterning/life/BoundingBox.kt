package patterning.life

import java.math.BigDecimal
import patterning.Canvas
import patterning.Theme
import processing.core.PApplet
import processing.core.PGraphics

class BoundingBox(bounds: Bounds, cellSize: BigDecimal, private val canvas: Canvas) {
    // we draw the box just a bit off screen so it won't be visible
    // but if the box is more than a pixel, we need to push it further offscreen
    // since we're using a Theme constant we can change we have to account for it
    private val positiveOffScreen = Theme.strokeWeightBounds
    private val negativeOffScreen = -positiveOffScreen
    
    private val leftBD = bounds.left.bigDecimal
    private val topBD = bounds.top.bigDecimal
    
    private val leftWithOffset = leftBD.multiply(cellSize, LifePattern.mc).add(LifePattern.canvasOffsetX)
    private val topWithOffset = topBD.multiply(cellSize, LifePattern.mc).add(LifePattern.canvasOffsetY)
    
    private val widthDecimal = bounds.width.bigDecimal.multiply(cellSize, LifePattern.mc)
    private val heightDecimal = bounds.height.bigDecimal.multiply(cellSize, LifePattern.mc)
    
    private val rightFloat = (leftWithOffset + widthDecimal).toFloat()
    private val bottomFloat = (topWithOffset + heightDecimal).toFloat()
    
    // coerce boundaries to be drawable with floats
    val left = if (leftWithOffset < BigDecimal.ZERO) negativeOffScreen else leftWithOffset.toFloat()
    val top = if (topWithOffset < BigDecimal.ZERO) negativeOffScreen else topWithOffset.toFloat()
    
    val right =
        if (leftWithOffset + widthDecimal > canvas.width) canvas.width.toFloat() + positiveOffScreen else rightFloat
    val bottom =
        if (topWithOffset + heightDecimal > canvas.height) canvas.height.toFloat() + positiveOffScreen else bottomFloat
    
    val width = if (left == negativeOffScreen) (right + positiveOffScreen) else (right - left)
    val height = if (top == negativeOffScreen) (bottom + positiveOffScreen) else (bottom - top)
    
    // Calculate Lines
    private val horizontalLine: Line
        get() {
            val startX = left
            val startYDecimal = topWithOffset + heightDecimal.divide(BigDecimal.TWO, LifePattern.mc)
            val startY = when {
                startYDecimal < BigDecimal.ZERO -> -1f
                startYDecimal > canvas.height -> canvas.height.toFloat() + 1
                else -> startYDecimal.toFloat()
            }
            val endXDecimal = leftWithOffset + widthDecimal
            val endX =
                if (endXDecimal > canvas.width) canvas.width.toFloat() + 1 else endXDecimal.toFloat()
            return Line(Point(startX, startY), Point(endX, startY))
        }
    
    private val verticalLine: Line
        get() {
            val startXDecimal = leftWithOffset + widthDecimal.divide(BigDecimal.TWO, LifePattern.mc)
            val startX = when {
                startXDecimal < BigDecimal.ZERO -> -1f
                startXDecimal > canvas.width -> canvas.width.toFloat() + 1
                else -> startXDecimal.toFloat()
            }
            val endYDecimal = topWithOffset + heightDecimal
            val endY =
                if (endYDecimal > canvas.height) canvas.height.toFloat() + 1 else endYDecimal.toFloat()
            return Line(Point(startX, top), Point(startX, endY))
        }
    
    
    private fun drawCrossHair(buffer: PGraphics, dashLength: Float, spaceLength: Float) {
        horizontalLine.drawDashedLine(buffer, dashLength, spaceLength)
        verticalLine.drawDashedLine(buffer, dashLength, spaceLength)
    }
    
    fun draw(buffer: PGraphics, drawCrosshair: Boolean = false) {
        
        buffer.pushStyle()
        buffer.noFill()
        buffer.stroke(Theme.textColor)
        buffer.strokeWeight(Theme.strokeWeightBounds)
        buffer.rect(left, top, width, height)
        if (drawCrosshair) {
            drawCrossHair(buffer, Theme.dashedLineDashLength, Theme.dashedLineSpaceLength)
        }
        
        buffer.popStyle()
    }
    
    private data class Point(val x: Float, val y: Float)
    
    private data class Line(val start: Point, val end: Point) {
        fun drawDashedLine(buffer: PGraphics, dashLength: Float, spaceLength: Float) {
            val x1 = start.x
            val y1 = start.y
            val x2 = end.x
            val y2 = end.y
            
            val distance = PApplet.dist(x1, y1, x2, y2)
            val numDashes = distance / (dashLength + spaceLength)
            
            var draw = true
            var x = x1
            var y = y1
            buffer.pushStyle()
            buffer.strokeWeight(Theme.strokeWeightDashedLine)
            for (i in 0 until (numDashes * 2).toInt()) {
                if (draw) {
                    // We limit the end of the dash to be at maximum the final point
                    val dxDash = (x2 - x1) / numDashes / 2
                    val dyDash = (y2 - y1) / numDashes / 2
                    val endX = (x + dxDash).coerceAtMost(x2)
                    val endY = (y + dyDash).coerceAtMost(y2)
                    buffer.line(x, y, endX, endY)
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
            buffer.popStyle()
        }
    }
}