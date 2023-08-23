package patterning.life

import patterning.Canvas
import patterning.Theme
import patterning.util.FlexibleDecimal
import patterning.util.roundToIntIfGreaterThanReference
import processing.core.PApplet
import processing.core.PGraphics

class BoundingBox(bounds: Bounds, private val canvas: Canvas) {
    
    private val mc = canvas.mc
    private val leftBD = bounds.left.toFlexibleDecimal().scale(mc)
    private val topBD = bounds.top.toFlexibleDecimal().scale(mc)
    
    private val zoomLevel = canvas.zoomLevel.scale(mc)
    private val offsetX = canvas.offsetX.scale(mc)
    private val offsetY = canvas.offsetY.scale(mc)
    
    private val leftZoomed = leftBD.multiply(zoomLevel, canvas.mc).scale(mc)
    private val leftWithOffset = leftZoomed.plus(offsetX).scale(mc)
    
    private val topZoomed = topBD.multiply(zoomLevel, canvas.mc).scale(mc)
    private val topWithOffset = topZoomed.plus(offsetY).scale(mc)
    
    private val widthDecimal =
        (bounds.right - bounds.left).toFlexibleDecimal().scale(mc).multiply(zoomLevel, canvas.mc).scale(mc)
    private val heightDecimal =
        (bounds.bottom - bounds.top).toFlexibleDecimal().scale(mc).multiply(zoomLevel, canvas.mc).scale(mc)
    
    // coerce boundaries to be drawable with floats
    val left = if (leftWithOffset < FlexibleDecimal.ZERO) negativeOffScreen else leftWithOffset.toFloat()
    val top = if (topWithOffset < FlexibleDecimal.ZERO) negativeOffScreen else topWithOffset.toFloat()
    
    val right = if (leftWithOffset + widthDecimal > canvas.width)
        canvas.width.toFloat() + positiveOffScreen
    else
        (leftWithOffset + widthDecimal).toFloat()
    
    val bottom = if (topWithOffset + heightDecimal > canvas.height)
        canvas.height.toFloat() + positiveOffScreen
    else
        (topWithOffset + heightDecimal).toFloat()
    
    val width = if (left == negativeOffScreen) (right + positiveOffScreen) else (right - left)
    val height = if (top == negativeOffScreen) (bottom + positiveOffScreen) else (bottom - top)
    
    // Calculate Lines
    private val horizontalLine: Line
        get() {
            val startX = left
            
            // divide by half cell size because the draw algorithm ends up with cells at center of universe right now...
            val halfHeight = heightDecimal.divide(FlexibleDecimal.TWO, canvas.mc)
            val startYDecimal = topWithOffset + halfHeight
            
            val startY = when {
                startYDecimal < FlexibleDecimal.ZERO -> negativeOffScreen
                startYDecimal > canvas.height -> canvas.height.toFloat() + positiveOffScreen
                else -> startYDecimal.toFloat()
            }
            
            val endXDecimal = leftWithOffset + widthDecimal
            
            val endX =
                if (endXDecimal > canvas.width) canvas.width.toFloat() + positiveOffScreen else endXDecimal.toFloat()
            
            return Line(Point(startX, startY), Point(endX, startY))
        }
    
    private val verticalLine: Line
        get() {
            // divide by half cell size because the draw algorithm ends up with cells at center of universe right now...
            
            val halfWidth = widthDecimal.divide(FlexibleDecimal.TWO, canvas.mc)
            val startXDecimal = leftWithOffset + halfWidth
            
            val startX = when {
                startXDecimal < FlexibleDecimal.ZERO -> negativeOffScreen
                startXDecimal > canvas.width -> canvas.width.toFloat() + positiveOffScreen
                else -> startXDecimal.toFloat()
            }
            
            val endYDecimal = topWithOffset + heightDecimal
            
            val endY =
                if (endYDecimal > canvas.height) canvas.height.toFloat() + positiveOffScreen else endYDecimal.toFloat()
            
            return Line(Point(startX, top), Point(startX, endY))
        }
    
    
    private fun drawCrossHair(graphics: PGraphics) {
        horizontalLine.drawDashedLine(graphics)
        verticalLine.drawDashedLine(graphics)
    }
    
    fun draw(graphics: PGraphics, drawCrosshair: Boolean = false) {
        
        graphics.pushStyle()
        graphics.noFill()
        graphics.stroke(Theme.textColor)
        graphics.strokeWeight(Theme.strokeWeightBounds)
        graphics.rect(left, top, width, height)
        
        if (drawCrosshair) {
            drawCrossHair(graphics)
        }
        
        graphics.popStyle()
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
        private const val positiveOffScreen = Theme.strokeWeightBounds
        private const val negativeOffScreen = -positiveOffScreen
    }
}