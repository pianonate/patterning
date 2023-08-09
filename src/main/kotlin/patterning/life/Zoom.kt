package patterning.life

import java.math.BigDecimal
import kotlin.math.pow
import patterning.Canvas
import patterning.actions.KeyHandler
import patterning.pattern.KeyCallbackFactory

class Zoom(
    private val lifePattern: LifePattern,
    private val canvas: Canvas,
    initialLevel: Float = DEFAULT_ZOOM_LEVEL
) {
    private var _level = initialLevel // backing property
    private var _targetSize = initialLevel // backing property for targetSize
    
    private var isZooming = false
    private var zoomCenterX = 0f
    private var zoomCenterY = 0f
    
    private var stepsTaken: Int = 0
    private var stepSize: Float = 0f  // This is the amount to change the level by on each update
    private val totalSteps = 50  // Say you want to reach the target in 10 updates
    
    
    // used to stop immediately if the user releases the zoom key while holding it down
    // if the user only presses it once then the invoke count will only be 1
    // and we should let the zoom play out
    private var zoomInvokeCount = 0
    
    private var targetSize: Float
        get() = _targetSize
        set(value) {
            _targetSize = if (value > 1) {
                2.0f.pow(kotlin.math.round(kotlin.math.log2(value)))
            } else if (value <= 1 && value > 0) {
                1.0f / (2.0f.pow(kotlin.math.round(kotlin.math.log2(1 / value))))
            } else {
                MINIMUM_ZOOM_LEVEL
            }
        }
    
    var level: Float
        get() = _level
        set(value) {
            _level = value
            bigLevelCached = _level.toBigDecimal()
        }
    
    private var bigLevelCached: BigDecimal = BigDecimal.ZERO // Cached value initialized with initial size
    
    var bigLevel: BigDecimal = BigDecimal.ZERO
        get() = bigLevelCached
        private set // Make the setter private to disallow external modification
    
    fun stopZooming() {
        isZooming = false
        if (zoomInvokeCount > 0) zoomInvokeCount = 0
    }
    
    fun zoom(zoomIn: Boolean, x: Float, y: Float) {
        
        if (targetSize <= MINIMUM_ZOOM_LEVEL) return
        
        //lifePattern.saveUndoState()
        
        val factor = if (zoomIn) ZOOM_FACTOR else 1 / ZOOM_FACTOR
        targetSize = level * factor
        
        stepSize = (targetSize - level) / totalSteps  // Compute the step size
        
        
        this.zoomCenterX = x
        this.zoomCenterY = y
        
        isZooming = true
        zoomInvokeCount++
        stepsTaken = 0
        
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
            
            val previousCellWidth = level
            
            if (stepsTaken == totalSteps - 1) {
                // On the last step, set the level directly to targetSize
                level = targetSize
            } else {
                // Otherwise, increment by the step size
                level += stepSize
                stepsTaken++  // Increment the step counter
            }
            
            // Calculate zoom factor
            val zoomFactor = level / previousCellWidth
            
            // Calculate the difference in canvas offset-s before and after zoom
            val offsetX = (1 - zoomFactor) * (zoomCenterX - canvas.offsetX.toFloat())
            val offsetY = (1 - zoomFactor) * (zoomCenterY - canvas.offsetY.toFloat())
            
            // Update canvas offsets
            canvas.adjustCanvasOffsets(offsetX.toBigDecimal(), offsetY.toBigDecimal())
            
            if (level == targetSize) {
                stopZooming()
            }
        }
    }
    
    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 1f
        private const val MINIMUM_ZOOM_LEVEL = 0.00001f
        private const val ZOOM_FACTOR = 4f
    }
}