package patterning

import java.awt.Color
import java.awt.Component
import java.math.BigDecimal
import kotlin.math.pow
import patterning.actions.KeyHandler
import patterning.pattern.KeyCallbackFactory
import processing.core.PApplet
import processing.core.PGraphics

class Canvas(private val pApplet: PApplet) {
    
    val zoom = Zoom()
    
    private data class CanvasState(
        val level: Float,
        val canvasOffsetX: BigDecimal,
        val canvasOffsetY: BigDecimal
    )
    
    private val undoDeque = ArrayDeque<CanvasState>()
    
    private var prevWidth: Int = 0
    private var prevHeight: Int = 0
    var resized = false
        private set
    var width: BigDecimal = BigDecimal.ZERO
        private set
    var height: BigDecimal = BigDecimal.ZERO
        private set
    var offsetX = BigDecimal.ZERO
        private set
    var offsetY = BigDecimal.ZERO
        private set
    
    private val offsetsMovedObservers = mutableListOf<OffsetsMovedObserver>()
    
    
    init {
        updateDimensions()
    }
    
    fun addOffsetsMovedObserver(observer: OffsetsMovedObserver) {
        offsetsMovedObservers.add(observer)
    }
    
    fun getPGraphics(): PGraphics {
        return pApplet.createGraphics(pApplet.width, pApplet.height)
    }
    
    fun adjustCanvasOffsets(dx: BigDecimal, dy: BigDecimal) {
        updateCanvasOffsets(offsetX + dx, offsetY + dy)
    }
    
    fun updateCanvasOffsets(offsetX: BigDecimal, offsetY: BigDecimal) {
        this.offsetX = offsetX
        this.offsetY = offsetY
        for (observer in offsetsMovedObservers) {
            observer.onOffsetsMoved()
        }
    }
    
    fun clearHistory() {
        undoDeque.clear()
    }
    
    fun saveUndoState() {
        undoDeque.add(CanvasState(zoom.level, offsetX, offsetY))
    }
    
    fun undoMovement() {
        if (undoDeque.isNotEmpty()) {
            zoom.stopZooming()
            val previous = undoDeque.removeLast()
            zoom.level = previous.level
            updateCanvasOffsets(previous.canvasOffsetX, previous.canvasOffsetY)
        }
    }
    
    /**
     * internal for PatterningPApplet to delegate drawing background
     * in a more rigorous way :)
     */
    internal fun drawBackground() {
        resized = (pApplet.width != prevWidth || pApplet.height != prevHeight)
        if (resized || Theme.isTransitioning) {
            updateDimensions()
            mitigateFlicker()
        }
        pApplet.background(Theme.backGroundColor)
    }
    
    /**
     * internal work around for initialization challenges with Processing
     */
    internal fun updateDimensions() {
        prevWidth = pApplet.width
        prevHeight = pApplet.height
        width = BigDecimal(pApplet.width)
        height = BigDecimal(pApplet.height)
    }
    
    /**
     * what is going on here? why is this necessary?
     * using background() in the draw loop draws on top of the default grey color for a
     * processing sketch. when you resize a window, for a split second it shows that grey
     * background. which makes the screen flicker massively
     *
     * this is a hacky way to get around that.
     *
     * first off - we have to get the theme's background color. we use this because when
     * the theme is transitioning, this color changes for the duration of the transition.
     * to mitigateFlicker we get the surface.native AWT component and
     * set it's background Color. The AWT Color can't just be given an int so we need
     * Processing's PApplet.color fun to convert it to something Color can use and then
     * be applied to the native AWT background
     *
     * the discovery on how to fix this flickering took awhile so the documentation is way
     * longer than the code itself :)
     *
     * see for yourself what it looks ike if you change the color to red .color(255,0,0) and
     * then resize the window
     */
    private fun mitigateFlicker() {
        val nativeSurface = pApplet.surface.native as Component
        nativeSurface.background = Color(pApplet.color(Theme.backGroundColor))
    }
    
    inner class Zoom(
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
            
            saveUndoState()
            
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
                val offsetX = (1 - zoomFactor) * (zoomCenterX - offsetX.toFloat())
                val offsetY = (1 - zoomFactor) * (zoomCenterY - offsetY.toFloat())
                
                // Update canvas offsets
                adjustCanvasOffsets(offsetX.toBigDecimal(), offsetY.toBigDecimal())
                
                if (level == targetSize) {
                    stopZooming()
                }
            }
        }
        
    }
    
    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 1f
        private const val MINIMUM_ZOOM_LEVEL = 0.00001f
        private const val ZOOM_FACTOR = 4f
    }
}