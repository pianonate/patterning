package patterning

import java.awt.Color
import java.awt.Component
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.pow
import patterning.actions.KeyHandler
import patterning.pattern.KeyCallbackFactory
import patterning.pattern.MathContextAware
import patterning.util.FlexibleInteger
import processing.core.PApplet
import processing.core.PGraphics

class Canvas(private val pApplet: PApplet) : MathContextAware {
    
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
    
    // without this precision on the MathContext, small imprecision propagates at
    // large levels on the LifePattern - sometimes this will cause the image to jump around or completely
    // off the screen.  don't skimp on precision!
    // udpateBiggestDimension allows us to ensure we keep this up to date
    lateinit var mc: MathContext
        private set
    
    init {
        resetMathContext()
        updateDimensions()
    }
    
    // i was wondering why empirically we needed a PRECISION_BUFFER to add to the precision
    // now that i'm thinking about it, this is probably the required precision for a float
    // which is what the cell.cellSize is - especially for really small numbers
    // without it we'd be off by only looking at the integer part of the largest dimension
    private var previousPrecision: Int = 0
    
    fun updateBiggestDimension(biggestDimension: FlexibleInteger) {
        // update math context for calculations
        val precision = biggestDimension.minPrecisionForDrawing()
        if (precision != previousPrecision) {
            mc = MathContext(precision)
            previousPrecision = precision
        }
        
        // update the minimum zoom level so we don't ask for zooms that can't happen
        val calculatedMinZoom = BigDecimal.ONE.divide(biggestDimension.bigDecimal, mc)
        zoom.minZoomLevel = if (calculatedMinZoom <= Float.MIN_VALUE.toBigDecimal()) {
            Float.MIN_VALUE
        } else {
            calculatedMinZoom.toFloat()
        }
    }
    
    override fun resetMathContext() {
        previousPrecision = 0
        mc = MathContext(FlexibleInteger.PRECISION_BUFFER)
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
        
        // Calculate the center of the visible portion before resizing
        val centerXBefore = calcCenterOnResize(width, offsetX)
        val centerYBefore = calcCenterOnResize(height, offsetY)
        
        width = BigDecimal(pApplet.width)
        height = BigDecimal(pApplet.height)
        
        val centerXAfter = calcCenterOnResize(width, offsetX)
        val centerYAfter = calcCenterOnResize(height, offsetY)
        
        adjustCanvasOffsets(centerXAfter - centerXBefore, centerYAfter - centerYBefore)
        
    }
    
    private fun calcCenterOnResize(dimension: BigDecimal, offset: BigDecimal): BigDecimal {
        return dimension.divide(BigDecimal.TWO, mc) - offset
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
        internal var minZoomLevel: Float = MINIMUM_ZOOM_LEVEL
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
                    val result = 1.0f / (2.0f.pow(kotlin.math.round(kotlin.math.log2(1 / value))))
                    result.coerceAtLeast(Float.MIN_VALUE)
                } else {
                    minZoomLevel
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
            
            val factor = if (zoomIn) ZOOM_FACTOR else 1 / ZOOM_FACTOR
            targetSize = level * factor
            
            if (targetSize <= minZoomLevel) return
            
            saveUndoState()
            
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
        private const val MINIMUM_ZOOM_LEVEL = Float.MIN_VALUE
        private const val ZOOM_FACTOR = 4f
    }
}