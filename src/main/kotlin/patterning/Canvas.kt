package patterning


import java.awt.Color
import java.awt.Component
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.log2
import kotlin.math.roundToInt
import patterning.actions.KeyHandler
import patterning.pattern.KeyCallbackFactory
import patterning.util.FlexibleInteger
import patterning.util.PRECISION_BUFFER
import patterning.util.minPrecisionForDrawing
import processing.core.PApplet
import processing.core.PFont
import processing.core.PGraphics
import processing.core.PImage

class Canvas(private val pApplet: PApplet) {
    private data class CanvasState(
        val level: BigDecimal,
        val canvasOffsetX: BigDecimal,
        val canvasOffsetY: BigDecimal
    )
    
    data class ReferenceInfo(val graphicsReference: GraphicsReference, val resizable: Boolean)
    
    private val zoom = Zoom()
    private val graphicsReferenceCache = mutableMapOf<String, ReferenceInfo>()
    private val offsetsMovedObservers = mutableListOf<OffsetsMovedObserver>()
    private val undoDeque = ArrayDeque<CanvasState>()
    
    private var prevWidth: Int = 0
    private var prevHeight: Int = 0
    
    private var resized = false
    
    var width: BigDecimal = BigDecimal.ZERO
        private set
    var height: BigDecimal = BigDecimal.ZERO
        private set
    var offsetX: BigDecimal = BigDecimal.ZERO
        private set
    var offsetY: BigDecimal = BigDecimal.ZERO
        private set
    
    init {
        resetMathContext()
        updateDimensions()
    }
    
    /**
     * zoom delegates
     */
    var zoomLevel: BigDecimal
        get() = zoom.level
        set(value) {
            zoom.level = value
        }
    
    val zoomLevelAsFloat: Float
        get() = zoom.levelAsFloat()
    
    fun updateZoom() = zoom.update()
    
    fun zoom(zoomIn: Boolean, x: Float, y: Float) = zoom.zoom(zoomIn, x, y)
    
    // without this precision on the MathContext, small imprecision propagates at
    // large levels on the LifePattern - sometimes this will cause the image to jump around or completely
    // off the screen.  don't skimp on precision!
    // updateBiggestDimension allows us to ensure we keep this up to date
    lateinit var mc: MathContext
        private set
    
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
        zoom.minZoomLevel = BigDecimal.ONE.divide(biggestDimension.bigDecimal, mc)
    }
    
    fun newPattern() {
        undoDeque.clear()
        zoom.stopZooming()
        resetMathContext()
    }
    
    private fun resetMathContext() {
        previousPrecision = 0
        mc = MathContext(PRECISION_BUFFER)
    }
    
    fun addOffsetsMovedObserver(observer: OffsetsMovedObserver) {
        offsetsMovedObservers.add(observer)
    }
    
    /**
     * Retrieve the PGraphics instance by its name. create if it doesn't exist
     */
    fun getNamedGraphicsReference(name: String, resizable: Boolean = true): GraphicsReference {
        return graphicsReferenceCache.computeIfAbsent(name) {
            val newGraphics = pApplet.createGraphics(pApplet.width, pApplet.height)
            ReferenceInfo(GraphicsReference(newGraphics), resizable)
        }.graphicsReference
        
    }
    
    fun getGraphics(width: Int, height: Int): PGraphics {
        return pApplet.createGraphics(width, height)
    }
    
    fun createFont(name: String, size: Float): PFont {
        return pApplet.createFont(name, size)
    }
    
    fun loadImage(fileSpec: String): PImage {
        return pApplet.loadImage(fileSpec)
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
    
    private fun handleResize() {
        resized = (pApplet.width != prevWidth || pApplet.height != prevHeight)
        
        if (resized) {
            updateResizableGraphicsReferences()
            updateDimensions()
        }
        
        if (resized || Theme.isTransitioning) {
            mitigateFlicker()
        }
    }
    
    private fun updateResizableGraphicsReferences() {
        graphicsReferenceCache.forEach { (_, referenceInfo) ->
            if (referenceInfo.resizable) {
                val newGraphics = pApplet.createGraphics(pApplet.width, pApplet.height)
                referenceInfo.graphicsReference.updateGraphics(newGraphics)
            }
        }
    }
    
    /**
     * internal for PatterningPApplet to delegate drawing background
     * in a more rigorous way :)
     */
    internal fun drawBackground() {
        handleResize()
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
    
    private inner class Zoom(
        initialLevel: Float = DEFAULT_ZOOM_LEVEL
    ) {
        var minZoomLevel: BigDecimal = BigDecimal.ZERO
        private var _level = initialLevel.toBigDecimal()
        private var _targetSize = initialLevel.toBigDecimal() // backing property for targetSize
        
        private var isZooming = false
        private var zoomCenterX = BigDecimal.ZERO
        private var zoomCenterY = BigDecimal.ZERO
        
        private var stepsTaken = 0
        private var stepSize = BigDecimal.ZERO  // This is the amount to change the level by on each update
        private val totalSteps = 20  // Say you want to reach the target in 10 updates
        
        // used to stop immediately if the user releases the zoom key while holding it down
        // if the user only presses it once then the invoke count will only be 1
        // and we should let the zoom play out
        private var zoomInvokeCount = 0
        
        private var targetSize: BigDecimal
            get() = _targetSize
            set(value) {
                _targetSize = when {
                    value > BigDecimal.ONE -> computeTargetSize(value)
                    value in BigDecimal.ZERO..BigDecimal.ONE -> computeTargetSize(value)
                    else -> minZoomLevel
                }
            }
        
        /**
         * the purpose is to constrain values to powers of 2 for zooming in and out as that generally
         * provides a pleasing effect. however figuring out powers of 2 on super large universes is
         * problematic so if casing toDouble() results in POSITIVE_INFINITY then we just return the
         * requested targetSize - truly an edge case for a very large universe
         */
        private fun computeTargetSize(value: BigDecimal): BigDecimal {
            val isGreaterThanOne = value > BigDecimal.ONE
            
            val adjustedValue = if (isGreaterThanOne) value else BigDecimal.ONE.divide(value, mc)
            
            val logValueDouble = log2(adjustedValue.toDouble())
            if (logValueDouble == Double.POSITIVE_INFINITY) return value
            
            val logValue = logValueDouble.roundToInt()
            val resultPower = BigDecimal(2).pow(logValue)
            
            return if (isGreaterThanOne) resultPower else BigDecimal.ONE.divide(resultPower, mc)
        }
        
        
        var level: BigDecimal
            get() = _level
            set(value) {
                _level = value
                cachedFloatLevel = null // Invalidate the cache
            }
        
        private var cachedFloatLevel: Float? = null
        
        fun levelAsFloat(): Float {
            return cachedFloatLevel ?: run {
                require(_level > BigDecimal.ZERO) { "zoom levels can't be < 0 $_level" }
                val floatValue = _level.toFloat()
                cachedFloatLevel = floatValue
                floatValue
            }
        }
        
        fun stopZooming() {
            isZooming = false
            if (zoomInvokeCount > 0) zoomInvokeCount = 0
        }
        
        fun zoom(zoomIn: Boolean, x: Float, y: Float) {
            
            val factor = if (zoomIn) ZOOM_FACTOR_IN else ZOOM_FACTOR_OUT
            targetSize = level * factor.toBigDecimal()
            
            if (targetSize <= minZoomLevel) {
                return
            }
            
            saveUndoState()
            
            stepSize = (targetSize - level).divide(totalSteps.toBigDecimal(), mc)  // Compute the step size
            
            this.zoomCenterX = x.toBigDecimal()
            this.zoomCenterY = y.toBigDecimal()
            
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
                val zoomFactor = level.divide(previousCellWidth, mc)
                
                // Calculate the difference in canvas offset-s before and after zoom
                val offsetX = (BigDecimal.ONE - zoomFactor).multiply((zoomCenterX - offsetX), mc)
                val offsetY = (BigDecimal.ONE - zoomFactor).multiply((zoomCenterY - offsetY), mc)
                
                // Update canvas offsets
                adjustCanvasOffsets(offsetX, offsetY)
                
                if (level == targetSize) {
                    stopZooming()
                }
            }
        }
    }
    
    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 1f
        private const val ZOOM_FACTOR_IN = 4f
        private const val ZOOM_FACTOR_OUT = .25f
        
    }
}