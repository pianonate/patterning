package patterning


import com.jogamp.newt.opengl.GLWindow
import kotlin.math.log2
import kotlin.math.roundToInt
import patterning.util.FlexibleDecimal
import patterning.util.FlexibleInteger
import patterning.util.PRECISION_BUFFER
import patterning.util.minPrecisionForDrawing
import processing.core.PApplet
import processing.core.PConstants.P3D
import processing.core.PFont
import processing.core.PGraphics
import processing.core.PImage
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.math.MathContext
import java.math.RoundingMode

class Canvas(private val pApplet: PApplet) {
    private data class CanvasState(
        val level: FlexibleDecimal,
        val canvasOffsetX: FlexibleDecimal,
        val canvasOffsetY: FlexibleDecimal
    )

    private val zoom = Zoom()
    private val graphicsReferenceCache = mutableMapOf<String, GraphicsReference>()
    private val offsetsMovedObservers = mutableListOf<OffsetsMovedObserver>()
    private val undoDeque = ArrayDeque<CanvasState>()

    private var prevWidth: Int = 0
    private var prevHeight: Int = 0

    var width: FlexibleDecimal = FlexibleDecimal.ZERO
        private set
    var height: FlexibleDecimal = FlexibleDecimal.ZERO
        private set
    var offsetX: FlexibleDecimal = FlexibleDecimal.ZERO
        private set
    var offsetY: FlexibleDecimal = FlexibleDecimal.ZERO
        private set

    init {
        resetMathContext()
        updateDimensions()
    }

    /**
     * zoom delegates
     */
    var zoomLevel: FlexibleDecimal
        get() = zoom.level
        set(value) {
            zoom.level = zoom.computeNearestPowerOf2TargetSize(value, findNextLowerPowerOf2 = true)
        }

    val zoomLevelAsFloat: Float
        get() = zoom.levelAsFloat()

    fun updateZoom() = zoom.update()

    fun zoom(zoomIn: Boolean, x: Float, y: Float) = zoom.zoom(zoomIn, x, y)

    val position: Point
        get() {
            val window = pApplet.surface.native as GLWindow
            return Point(window.x, window.y)
        }

    fun nextScreen() {
        val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices

        val position = position

        // Find the screen where the window is located
        var index = 0
        for (i in screens.indices) {
            val screen = screens[i]
            if (screen.defaultConfiguration.bounds.contains(position)) {
                index = i
                break
            }
        }

        // go to the next and then wrap around to the beginning
        val next = (index + 1) % screens.size

        val bounds = screens[next].defaultConfiguration.bounds
        with(pApplet.surface) {
            setLocation(bounds.x, bounds.y)
            setSize(bounds.width, bounds.height)
        }
    }

    // without this precision on the MathContext, small imprecision propagates at
    // large levels on the LifePattern - sometimes this will cause the image to jump around or completely
    // off the screen.  don't skimp on precision!
    // updateBiggestDimension allows us to ensure we keep this up to date
    lateinit var mc: MathContext
        private set

    // i was wondering why empirically we needed a PRECISION_BUFFER to add to the precision
    // now that i'm thinking about it, this is probably the required precision for a float
    // which is what the cell.cellSize is - especially for minuscule numbers
    // without it, we'd be off by only looking at the integer part of the largest dimension
    private var previousPrecision: Int = 0

    fun updateBiggestDimension(biggestDimension: Long) {
        // update math context for calculations
        val precision = FlexibleInteger.create(biggestDimension).minPrecisionForDrawing()
        if (precision != previousPrecision) {
            mc = MathContext(precision)
            previousPrecision = precision
        }

        // update the minimum zoom level, so we don't ask for zooms that can't happen
        zoom.minZoomLevel = FlexibleDecimal.ONE.divide(FlexibleDecimal.create(biggestDimension), mc)
    }

    fun newPattern() {
        undoDeque.clear()
        zoom.stopZooming()
        resetMathContext()
    }

    private fun resetMathContext() {
        previousPrecision = 0
        mc = MathContext(FlexibleInteger.ZERO.minPrecisionForDrawing())
    }

    fun addOffsetsMovedObserver(observer: OffsetsMovedObserver) {
        offsetsMovedObservers.add(observer)
    }

    /**
     * Retrieve the PGraphics instance by its name. create if it doesn't exist
     */
    fun getNamedGraphicsReference(
        name: String,
        width: Int = pApplet.width,
        height: Int = pApplet.height,
        resizable: Boolean = true,
        useOpenGL: Boolean = false
    ): GraphicsReference {
        return graphicsReferenceCache.computeIfAbsent(name) {
            val newGraphics = getGraphics(width = width, height = height, useOpenGL = useOpenGL)
            GraphicsReference(newGraphics, name, resizable, useOpenGL)
        }
    }

    fun getGraphics(width: Int, height: Int, creator: PApplet = pApplet, useOpenGL: Boolean = false): PGraphics {

        return if (useOpenGL) {
            creator.createGraphics(width, height, P3D).also {
                it.smooth(OPENGL_PGRAPHICS_SMOOTH)
                it.beginDraw()

                // necessary so ghost mode looks correct for alpha values when rotating in 3 dimensions
                // otherwise when it is on the right side of the screen it draws visibly darker
                it.hint(PGraphics.DISABLE_DEPTH_TEST)

                // seemingly necessary to stop errors occurring between end of draw() and the
                // under the hood processing endDraw() call - which can occur (for example) when switching
                // screens during startup, or sometimes just on a slow startup
                // so far this seems to be a reliable fix - i consider it a hack :(
                it.hint(PGraphics.DISABLE_OPENGL_ERRORS)
                it.endDraw()
            }
        } else {
            // we use plain ol' renderer for the UX
            return creator.createGraphics(width, height)
        }
    }

    fun createFont(name: String, size: Float): PFont {
        return pApplet.createFont(name, size)
    }

    fun loadImage(fileSpec: String): PImage {
        return pApplet.loadImage(fileSpec)
    }

    fun moveCanvasOffsets(dx: FlexibleDecimal, dy: FlexibleDecimal) {
        updateCanvasOffsets(offsetX + dx, offsetY + dy)
    }

    fun updateCanvasOffsets(offsetX: FlexibleDecimal, offsetY: FlexibleDecimal) {
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

    internal fun handleResize() {
        val resized = (pApplet.width != prevWidth || pApplet.height != prevHeight)

        if (resized) {
            updateDimensions()
            updateResizableGraphicsReferences()
        }
    }

    private fun updateResizableGraphicsReferences() {
        // filter for whether they are openGL or not as we create them at different times
        graphicsReferenceCache.forEach { (_, reference) ->
            if (reference.isResizable) {
                val newGraphics = getGraphics(pApplet.width, pApplet.height, useOpenGL = reference.useOpenGL)
                reference.updateGraphics(newGraphics)
            }
        }
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

        width = FlexibleDecimal.create(pApplet.width)
        height = FlexibleDecimal.create(pApplet.height)

        val centerXAfter = calcCenterOnResize(width, offsetX)
        val centerYAfter = calcCenterOnResize(height, offsetY)

        moveCanvasOffsets(
            dx = (centerXAfter - centerXBefore),
            dy = (centerYAfter - centerYBefore)
        )

    }

    private fun calcCenterOnResize(dimension: FlexibleDecimal, offset: FlexibleDecimal): FlexibleDecimal {
        return dimension.divide(FlexibleDecimal.TWO, mc) - offset
    }

    private inner class Zoom(
        initialLevel: Float = DEFAULT_ZOOM_LEVEL
    ) {
        var minZoomLevel = FlexibleDecimal.ZERO
        private var _level = FlexibleDecimal.create(initialLevel)// initialLevel.toBigDecimal()
        private var _targetSize =
            FlexibleDecimal.create(initialLevel) // initialLevel.toBigDecimal() // backing property for targetSize

        private var isZooming = false
        private var zoomCenterX = FlexibleDecimal.ZERO
        private var zoomCenterY = FlexibleDecimal.ZERO

        private var stepsTaken = 0
        private var stepSize = FlexibleDecimal.ZERO  // This is the amount to change the level by on each update
        private val totalSteps = 20  // Say you want to reach the target in 10 updates

        private var targetSize: FlexibleDecimal
            get() = _targetSize
            set(value) {
                _targetSize = when {
                    value > FlexibleDecimal.ZERO -> computeNearestPowerOf2TargetSize(value)
                    else -> minZoomLevel
                }
            }

        /**
         * the purpose is to constrain values to powers of 2 for zooming in and out as that generally
         * provides a pleasing effect. however figuring out powers of 2 on super large universes is
         * problematic so if casing toDouble() results in POSITIVE_INFINITY then we just return the
         * requested targetSize - truly an edge case for a very large universe
         */
        /*fun computeNearestPowerOf2TargetSize(value: FlexibleDecimal): FlexibleDecimal {
            val isGreaterThanOne = value > FlexibleDecimal.ONE
            
            val adjustedValue = if (isGreaterThanOne) value else FlexibleDecimal.ONE.divide(value, mc)
            
            val logValueDouble = log2(adjustedValue.toDouble())
            if (logValueDouble == Double.POSITIVE_INFINITY) return value
            
            val logValue = logValueDouble.roundToInt()
            val resultPower = FlexibleDecimal.TWO.pow(logValue)
            
            return if (isGreaterThanOne) resultPower else FlexibleDecimal.ONE.divide(resultPower, mc)
        }*/
        fun computeNearestPowerOf2TargetSize(
            requestedZoomLevel: FlexibleDecimal,
            findNextLowerPowerOf2: Boolean = false
        ): FlexibleDecimal {
            val isGreaterThanOne = requestedZoomLevel > FlexibleDecimal.ONE
            val adjustedValue =
                if (isGreaterThanOne) requestedZoomLevel else FlexibleDecimal.ONE.divide(requestedZoomLevel, mc)

            val logValueDouble = log2(adjustedValue.toDouble())
            if (logValueDouble == Double.POSITIVE_INFINITY) return requestedZoomLevel

            var logValue = logValueDouble.roundToInt()
            var resultPower = FlexibleDecimal.TWO.pow(logValue)
            var result = if (isGreaterThanOne) resultPower else FlexibleDecimal.ONE.divide(resultPower, mc)

            // If findLower is true and the result is greater than the original value, subtract 1 from the log value
            if (findNextLowerPowerOf2 && result > requestedZoomLevel) {

                logValue = if (isGreaterThanOne)
                    logValue - 1
                else
                    logValue + 1

                resultPower = FlexibleDecimal.TWO.pow(logValue)
                result = if (isGreaterThanOne) resultPower else FlexibleDecimal.ONE.divide(resultPower, mc)
            }

            return result
        }


        var level: FlexibleDecimal
            get() = _level
            set(value) {
                _level = value
                cachedFloatLevel = null // Invalidate the cache
            }

        private var cachedFloatLevel: Float? = null

        fun levelAsFloat(): Float {
            return cachedFloatLevel ?: run {
                require(_level > FlexibleDecimal.ZERO) { "zoom levels can't be < 0 $_level" }
                val floatValue = _level.toFloat()
                cachedFloatLevel = floatValue
                floatValue
            }
        }

        fun stopZooming() {
            isZooming = false
        }

        fun zoom(zoomIn: Boolean, x: Float, y: Float) {

            val factor = if (zoomIn) ZOOM_FACTOR_IN else ZOOM_FACTOR_OUT
            targetSize = level.multiply(factor, mc)

            if (targetSize <= minZoomLevel) {
                return
            }

            saveUndoState()

            stepSize = (targetSize - level).divide(FlexibleDecimal.create(totalSteps), mc)  // Compute the step size

            this.zoomCenterX = FlexibleDecimal.create(x)
            this.zoomCenterY = FlexibleDecimal.create(y)

            isZooming = true
            stepsTaken = 0

        }

        fun update() {
            if (isZooming) {

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
                val zoomFactor = level.divide(previousCellWidth, ZOOM_MATH_CONTEXT)

                // Calculate the difference in canvas offset-s before and after zoom
                val dx = (FlexibleDecimal.ONE - zoomFactor).multiply((zoomCenterX - offsetX), ZOOM_MATH_CONTEXT)
                val dy = (FlexibleDecimal.ONE - zoomFactor).multiply((zoomCenterY - offsetY), ZOOM_MATH_CONTEXT)

                // move canvas offsets by this amount
                moveCanvasOffsets(dx, dy)

                if (level == targetSize) {
                    stopZooming()
                }
            }
        }
    }

    companion object {
        private const val OPENGL_PGRAPHICS_SMOOTH = 4

        private const val DEFAULT_ZOOM_LEVEL = 1f
        private val ZOOM_FACTOR_IN = FlexibleDecimal.create(2)
        private val ZOOM_FACTOR_OUT = FlexibleDecimal.create(.5f)
        private val ZOOM_MATH_CONTEXT = MathContext(PRECISION_BUFFER, RoundingMode.HALF_UP)
    }
}