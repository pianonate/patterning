package patterning.life

import kotlin.math.abs
import patterning.actions.KeyCallbackFactory
import patterning.actions.KeyHandler

class Zoom(private val lifePattern: LifePattern) {
    private var targetSize = LifePattern.cell.size
    private var isZooming = false
    private var zoomCenterX = 0f
    private var zoomCenterY = 0f
    private val t = 0.1f
    
    // used to stop immediately if the user releases the zoom key while holding it down
    // if the user only presses it once then the invoke count will only be 1
    // and we should let the zoom play out
    private var zoomInvokeCount = 0
    
    fun stopZooming() {
        isZooming = false
        if (zoomInvokeCount > 0) zoomInvokeCount = 0
    }
    
    fun zoom(zoomIn: Boolean, x: Float, y: Float) {
        lifePattern.saveUndoState()
        
        // Adjust cell width to align with grid
        val factor = if (zoomIn) 2f else 0.2f
        
        targetSize = LifePattern.cell.size * factor
        
        this.zoomCenterX = x
        this.zoomCenterY = y
        
        isZooming = true
        zoomInvokeCount++
    }
    
    fun update() {
        if (isZooming) {
            
            if (!KeyHandler.pressedKeys.contains(KeyCallbackFactory.SHORTCUT_ZOOM_IN.code) &&
                !KeyHandler.pressedKeys.contains(KeyCallbackFactory.SHORTCUT_ZOOM_OUT.code) &&
                !KeyHandler.pressedKeys.contains(KeyCallbackFactory.SHORTCUT_ZOOM_CENTERED.code)
            ) {
                if (zoomInvokeCount > 1) {
                    stopZooming()
                    return
                }
            }
            
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
                stopZooming()
            }
        }
    }
}