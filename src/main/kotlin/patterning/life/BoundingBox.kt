package patterning.life

import patterning.Canvas
import patterning.Theme
import patterning.util.FlexibleDecimal
import patterning.util.roundToIntIfGreaterThanReference
import processing.core.PApplet
import processing.core.PGraphics

class BoundingBox(private val bounds: Bounds, private val canvas: Canvas) {
    
    data class BoundingBoxSnapshot(
        val leftBD: FlexibleDecimal,
        val topBD: FlexibleDecimal,
        val leftZoomed: FlexibleDecimal,
        val offsetX: FlexibleDecimal,
        val leftWithOffset: FlexibleDecimal,
        val topZoomed: FlexibleDecimal,
        val offsetY: FlexibleDecimal,
        val topWithOffset: FlexibleDecimal,
        val widthDecimal: FlexibleDecimal,
        val heightDecimal: FlexibleDecimal,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val width: Float,
        val height: Float,
        val bounds: Bounds,
        val halfHeight: FlexibleDecimal,
        val startYDecimal: FlexibleDecimal,
    )
    
    val mc = canvas.mc
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
            count++
            val startX = left
            
            // divide by half cell size because the draw algorithm ends up with cells at center of universe right now...
            val halfHeight = heightDecimal.divide(FlexibleDecimal.TWO, canvas.mc)
            val startYDecimal = topWithOffset + halfHeight
            
            
            if (count % 2 == 0) {
                secondSnapshot = createSnapshot(halfHeight, startYDecimal)
                val discrepancies = compareSnapshots(firstSnapshot!!, secondSnapshot!!)
                discrepancies.forEach(::println)
            } else {
                firstSnapshot = createSnapshot(halfHeight, startYDecimal)
            }
            
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
    
    private fun compareSnapshots(snapshot1: BoundingBoxSnapshot, snapshot2: BoundingBoxSnapshot): List<String> {
        val discrepancies = mutableListOf<String>()
        
        if (snapshot1.leftBD.multiply(
                FlexibleDecimal.TWO,
                canvas.mc
            ).scale(mc) != snapshot2.leftBD
        ) discrepancies.add("Discrepancy in leftBD: ${snapshot1.leftBD} vs ${snapshot2.leftBD}")
        
        if (snapshot1.topBD.multiply(
                FlexibleDecimal.TWO,
                canvas.mc
            ).scale(mc) != snapshot2.topBD
        ) discrepancies.add("Discrepancy in topBD: ${snapshot1.topBD} vs ${snapshot2.topBD}")
        
        if (snapshot1.leftZoomed.multiply(
                FlexibleDecimal.TWO,
                canvas.mc
            ).scale(mc) != snapshot2.leftZoomed
        ) discrepancies.add("Discrepancy in leftZoomed: ${snapshot1.leftZoomed} vs ${snapshot2.leftZoomed}")
        
        val leftDelta = (snapshot1.leftWithOffset + snapshot1.leftZoomed).scale(mc)
        if (leftDelta != snapshot2.leftWithOffset) {
            discrepancies.add("Discrepancy in leftWithOffset: ${snapshot1.leftWithOffset} + leftZoomed:${snapshot1.leftZoomed} = $leftDelta but is instead vs ${snapshot2.leftWithOffset} for a difference of: ${snapshot2.leftWithOffset - leftDelta}")
            discrepancies.add("             zoomed + canvasOffset = leftWithOffset ${snapshot1.leftZoomed} + ${snapshot1.offsetX} = ${snapshot1.leftWithOffset} vs. ${snapshot2.leftZoomed} + ${snapshot2.offsetX} = ${snapshot2.leftWithOffset} ")
        }
        
        if (snapshot1.topZoomed.multiply(
                FlexibleDecimal.TWO,
                canvas.mc
            ).scale(mc) != snapshot2.topZoomed
        ) discrepancies.add("Discrepancy in topZoomed: ${snapshot1.topZoomed} vs ${snapshot2.topZoomed}")
        
        val topDelta = (snapshot1.topWithOffset + snapshot1.topZoomed).scale(mc)
        if (topDelta != snapshot2.topWithOffset) {
            discrepancies.add("Discrepancy in topWithOffset: ${snapshot1.topWithOffset} + leftZoomed:${snapshot1.topZoomed} = $leftDelta but is instead vs ${snapshot2.topWithOffset} for a difference of: ${snapshot2.topWithOffset - topDelta}")
            discrepancies.add("             zoomed + canvasOffset = topWithOffset ${snapshot1.topZoomed} + ${snapshot1.offsetY} = ${snapshot1.topWithOffset} vs. ${snapshot2.topZoomed} + ${snapshot2.offsetY} = ${snapshot2.topWithOffset} ")
            
        }
        if (snapshot1.widthDecimal.multiply(
                FlexibleDecimal.TWO,
                canvas.mc
            ).scale(mc) != snapshot2.widthDecimal
        ) discrepancies.add("Discrepancy in widthDecimal: ${snapshot1.widthDecimal} vs ${snapshot2.widthDecimal}")
        
        if (snapshot1.heightDecimal.multiply(
                FlexibleDecimal.TWO,
                canvas.mc
            ).scale(mc) != snapshot2.heightDecimal
        ) discrepancies.add("Discrepancy in heightDecimal: ${snapshot1.heightDecimal} vs ${snapshot2.heightDecimal}")
        
        if (snapshot1.left != snapshot2.left) discrepancies.add("Discrepancy in left: ${snapshot1.left} vs ${snapshot2.left}")
        if (snapshot1.top != snapshot2.top) discrepancies.add("Discrepancy in top: ${snapshot1.top} vs ${snapshot2.top}")
        if (snapshot1.right != snapshot2.right) discrepancies.add("Discrepancy in right: ${snapshot1.right} vs ${snapshot2.right}")
        if (snapshot1.bottom != snapshot2.bottom) discrepancies.add("Discrepancy in bottom: ${snapshot1.bottom} vs ${snapshot2.bottom}")
        if (snapshot1.width != snapshot2.width) discrepancies.add("Discrepancy in width: ${snapshot1.width} vs ${snapshot2.width}")
        if (snapshot1.height != snapshot2.height) discrepancies.add("Discrepancy in height: ${snapshot1.height} vs ${snapshot2.height}")
        
        return discrepancies
    }
    
    
    private fun createSnapshot(halfHeight: FlexibleDecimal, startYDecimal: FlexibleDecimal): BoundingBoxSnapshot {
        return BoundingBoxSnapshot(
            leftBD = leftBD,
            topBD = topBD,
            leftZoomed = leftZoomed,
            offsetX = offsetX,
            leftWithOffset = leftWithOffset,
            topZoomed = topZoomed,
            offsetY = offsetY,
            topWithOffset = topWithOffset,
            widthDecimal = widthDecimal,
            heightDecimal = heightDecimal,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            width = width,
            height = height,
            bounds = bounds,
            halfHeight = halfHeight,
            startYDecimal = startYDecimal,
        )
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
        
        var count = 0
        var firstSnapshot: BoundingBoxSnapshot? = null
        var secondSnapshot: BoundingBoxSnapshot? = null
        
        // we draw the box just a bit off screen so it won't be visible
        // but if the box is more than a pixel, we need to push it further offscreen
        // since we're using a Theme constant we can change we have to account for it
        private const val positiveOffScreen = Theme.strokeWeightBounds
        private const val negativeOffScreen = -positiveOffScreen
    }
}