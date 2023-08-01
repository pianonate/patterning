package patterning.life

import kotlin.math.abs

class Zoom(private val lifePattern: LifePattern) {
    private var targetSize = LifePattern.cell.size
    private var isZooming = false
    private var zoomCenterX = 0f
    private var zoomCenterY = 0f
    private val t = 0.1f

    fun stopZooming() {
        isZooming = false
    }

    fun zoom(zoomIn: Boolean, x: Float, y: Float) {
        lifePattern.saveUndoState()

        // Adjust cell width to align with grid
        val factor = if (zoomIn) 1.8f else 0.5f
        targetSize = LifePattern.cell.size * factor
        isZooming = true
        this.zoomCenterX = x
        this.zoomCenterY = y
    }

    fun update() {
        if (isZooming) {
            val previousCellWidth = LifePattern.cell.size
            LifePattern.cell.size += (targetSize - LifePattern.cell.size) * t

            // Calculate zoom factor
            val zoomFactor = LifePattern.cell.size / previousCellWidth

            // Calculate the difference in canvas offset-s before and after zoom
            val offsetX = (1 - zoomFactor) * (zoomCenterX - LifePattern.canvasOffsetX.toFloat())
            val offsetY = (1 - zoomFactor) * (zoomCenterY - LifePattern.canvasOffsetY.toFloat())

            // Update canvas offsets
            lifePattern.adjustCanvasOffsets(offsetX.toBigDecimal(), offsetY.toBigDecimal())

            // Stop zooming if we're close enough to the target size
            // we used to set the cell.size to the target but that makes it jumpy
            if (abs(LifePattern.cell.size - targetSize) < 0.01) {
                isZooming = false
            }
        }
    }
}