package patterning


import com.jogamp.newt.opengl.GLWindow
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import processing.core.PApplet
import processing.core.PConstants.P3D
import processing.core.PFont
import processing.core.PGraphics
import processing.core.PImage
import processing.core.PVector
import java.awt.GraphicsEnvironment
import java.awt.Point

class Canvas(private val pApplet: PApplet) {

    private data class CanvasState(
        val level: Float,
        val canvasOffsetX: Float,
        val canvasOffsetY: Float
    )

    private val zoom = Zoom()
    private val graphicsReferenceCache = mutableMapOf<String, GraphicsReference>()
    private val offsetsMovedObservers = mutableListOf<OffsetsMovedObserver>()
    private val undoDeque = ArrayDeque<CanvasState>()

    private var prevWidth: Int = 0
    private var prevHeight: Int = 0
    private lateinit var threeD: ThreeD

    var width: Float = 0f
        private set
    var height: Float = 0f
        private set
    var offsetX: Float = 0f
        private set
    var offsetY: Float = 0f
        private set

    val offsetVector
        get() = PVector(offsetX, offsetY)

    fun registerThreeD(threeD: ThreeD) {
        this.threeD = threeD
    }

    /**
     * zoom delegates
     */
    var zoomLevel: Float
        get() = zoom.level
        set(value) {
            zoom.level = zoom.computeNearestPowerOf2TargetSize(value, findNextLowerPowerOf2 = true)
        }

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

    fun newPattern() {
        undoDeque.clear()
        zoom.stopZooming()
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

    private fun notifyObservers() {

        //undo will already set the threeD offset so no need to do it twice
        for (observer in offsetsMovedObservers) {
            observer.onCanvasOffsetsMoved()
        }
    }

    fun setCanvasOffsets(offset: PVector) {
        this.offsetX = offset.x
        this.offsetY = offset.y
        notifyObservers()
    }

    fun moveCanvasOffsets(delta: PVector) {
        this.offsetX += delta.x
        this.offsetY += delta.y
        notifyObservers()
    }

    fun saveUndoState() {

        undoDeque.add(CanvasState(zoom.level, offsetX, offsetY))
    }


    fun undoMovement() {
        if (undoDeque.isNotEmpty()) {
            zoom.stopZooming()
            val previous = undoDeque.removeLast()
            zoom.level = previous.level

            setCanvasOffsets(
                offset = PVector(previous.canvasOffsetX, previous.canvasOffsetY),
            )
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

        width = pApplet.width.toFloat()
        height = pApplet.height.toFloat()

        val centerXAfter = calcCenterOnResize(width, offsetX)
        val centerYAfter = calcCenterOnResize(height, offsetY)

        moveCanvasOffsets(
            PVector(
                (centerXAfter - centerXBefore),
                (centerYAfter - centerYBefore)
            )
        )

    }

    private fun calcCenterOnResize(dimension: Float, offset: Float): Float {
        return dimension / 2f - offset
    }

    private inner class Zoom(
        initialLevel: Float = DEFAULT_ZOOM_LEVEL
    ) {
        var minZoomLevel = 0f
        private var _level = initialLevel
        private var _targetSize = initialLevel

        private var isZooming = false
        private var zoomCenterX = 0f
        private var zoomCenterY = 0f

        private var stepsTaken = 0
        private var stepSize = 0f  // This is the amount to change the level by on each update
        private val totalSteps = 20  // Say you want to reach the target in 10 updates

        private var targetSize: Float
            get() = _targetSize
            set(value) {
                _targetSize = when {
                    value > 0f -> computeNearestPowerOf2TargetSize(value)
                    else -> minZoomLevel
                }
            }

        /**
         * the purpose is to constrain values to powers of 2 for zooming in and out as that generally
         * provides a pleasing effect. however figuring out powers of 2 on super large universes is
         * problematic so if casing toDouble() results in POSITIVE_INFINITY then we just return the
         * requested targetSize - truly an edge case for a very large universe
         */
        /*fun computeNearestPowerOf2TargetSize(value: Float): Float {
            val isGreaterThanOne = value > 1f
            
            val adjustedValue = if (isGreaterThanOne) value else 1f / value
            
            val logValueDouble = log2(adjustedValue.toDouble())
            if (logValueDouble == Double.POSITIVE_INFINITY) return value
            
            val logValue = logValueDouble.roundToInt()
            val resultPower = Float.TWO.pow(logValue)
            
            return if (isGreaterThanOne) resultPower else 1f / resultPower
        }*/
        fun computeNearestPowerOf2TargetSize(
            requestedZoomLevel: Float,
            findNextLowerPowerOf2: Boolean = false
        ): Float {
            val isGreaterThanOne = requestedZoomLevel > 1f
            val adjustedValue =
                if (isGreaterThanOne) requestedZoomLevel else 1f / requestedZoomLevel

            val logValueDouble = log2(adjustedValue.toDouble())
            if (logValueDouble == Double.POSITIVE_INFINITY) return requestedZoomLevel

            var logValue = logValueDouble.roundToInt()
            var resultPower = 2.0.pow(logValue)
            var result = if (isGreaterThanOne) resultPower else 1f / resultPower

            // If findLower is true and the result is greater than the original value, subtract 1 from the log value
            if (findNextLowerPowerOf2 && result > requestedZoomLevel) {

                logValue = if (isGreaterThanOne)
                    logValue - 1
                else
                    logValue + 1

                resultPower = 2.0.pow(logValue)
                result = if (isGreaterThanOne) resultPower else 1f / resultPower
            }

            return result.toFloat()
        }


        var level: Float
            get() = _level
            set(value) {
                _level = value
                cachedFloatLevel = null // Invalidate the cache
            }

        private var cachedFloatLevel: Float? = null

        fun stopZooming() {
            isZooming = false
        }

        fun zoom(zoomIn: Boolean, x: Float, y: Float) {

            val factor = if (zoomIn) ZOOM_FACTOR_IN else ZOOM_FACTOR_OUT
            targetSize = level * factor

            if (targetSize <= minZoomLevel) {
                return
            }

            saveUndoState()

            stepSize = (targetSize - level) / totalSteps

            this.zoomCenterX = x
            this.zoomCenterY = y

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
                val zoomFactor = level / previousCellWidth

                // Calculate the difference in canvas offset-s before and after zoom
                val dx = (1f - zoomFactor) * (zoomCenterX - offsetX)
                val dy = (1f - zoomFactor) * (zoomCenterY - offsetY)

                // move canvas offsets by this amount
                moveCanvasOffsets(delta = PVector(dx, dy))

                if (level == targetSize) {
                    stopZooming()
                }
            }
        }
    }

    companion object {
        private const val OPENGL_PGRAPHICS_SMOOTH = 2
        private const val DEFAULT_ZOOM_LEVEL = 1f
        private const val ZOOM_FACTOR_IN = 2f
        private const val ZOOM_FACTOR_OUT = .5f
    }
}