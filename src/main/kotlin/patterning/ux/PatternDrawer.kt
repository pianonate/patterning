package patterning.ux

import patterning.*
import patterning.actions.KeyFactory
import patterning.actions.MouseEventManager.Companion.instance
import patterning.actions.MovementHandler
import patterning.ux.informer.DrawingInfoSupplier
import patterning.ux.informer.DrawingInformer
import patterning.ux.panel.*
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector
import java.math.BigDecimal
import java.util.*
import java.util.function.IntSupplier
import kotlin.collections.ArrayDeque
import kotlin.math.roundToInt

class PatternDrawer(
    private val processing: PApplet
) {
    private val cellBorderWidthRatio = .05f
    private val drawingInformer: DrawingInfoSupplier
    private val patterning: Processing = processing as Processing
    private val hudInfo: HUDStringBuilder
    private val movementHandler: MovementHandler

    // ain't no way to do drawing without a singleton drawables manager
    private val drawables = DrawableManager.instance
    private val keyFactory: KeyFactory
    private var cellBorderWidth = 0.0f

    // lifeFormPosition is used because we now separate the drawing speed from the framerate
    // we may not draw an image every frame
    // if we haven't drawn an image, we still want to be able to move and drag
    // the image around so this allows us to keep track of the current position
    // for the lifeFormBuffer and move that buffer around regardless of whether we've drawn an image
    // whenever an image is drawn, ths PVector is reset to 0,0 to match the current image state
    // it's a nifty way to handle things - just follow lifeFormPosition through the code
    // to see what i'm talking about
    private var lifeFormPosition = PVector(0f, 0f)
    private var isDrawing = false
    private var cell: Cell
    private var countdownText: TextPanel? = null
    private var hudText: TextPanel? = null
    private var lifeFormBuffer: PGraphics
    private var uXBuffer: PGraphics
    private var drawBounds: Boolean
    private var canvasOffsetX = BigDecimal.ZERO
    private var canvasOffsetY = BigDecimal.ZERO

    // if we're going to be operating in BigDecimal then we keep these that way so
    // that calculations can be done without conversions until necessary
    private var canvasWidth: BigDecimal
    private var canvasHeight: BigDecimal

    // surprisingly caching the result of the half size calculation provides
    // a remarkable speed boost
    private val halfSizeMap: MutableMap<BigDecimal, BigDecimal> = HashMap()

    // used for resize detection
    private var prevWidth: Int
    private var prevHeight: Int

    init {
        uXBuffer = buffer
        lifeFormBuffer = buffer
        drawingInformer = DrawingInformer({ uXBuffer }, { isWindowResized }) { isDrawing }
        // resize trackers
        prevWidth = processing.width
        prevHeight = processing.height
        cell = Cell(DEFAULT_CELL_WIDTH)
        canvasWidth = processing.width.toBigDecimal()
        canvasHeight = processing.height.toBigDecimal()
        movementHandler = MovementHandler(this)
        drawBounds = false
        hudInfo = HUDStringBuilder()

        createTextPanel(null) {
            TextPanel.Builder(drawingInformer, Theme.startupText, AlignHorizontal.RIGHT, AlignVertical.TOP)
                .textSize(Theme.startupTextSize)
                .fadeInDuration(Theme.startupTextFadeInDuration)
                .fadeOutDuration(Theme.startupTextFadeOutDuration)
                .displayDuration(Theme.startupTextDisplayDuration.toLong())
        }

        keyFactory = KeyFactory(patterning, this)
        setupControls()
    }

    private val buffer: PGraphics
        get() = processing.createGraphics(processing.width, processing.height)

    private val isWindowResized: Boolean
        get() {
            val widthChanged = prevWidth != processing.width
            val heightChanged = prevHeight != processing.height
            return widthChanged || heightChanged
        }

    private fun setupControls() {

        // all callbacks have to invoke work - either on the Patterning or PatternDrawer
        // so give'em what they need
        keyFactory.setupKeyHandler()
        val panelLeft: ControlPanel
        val panelTop: ControlPanel
        val panelRight: ControlPanel
        val transitionDuration = Theme.controlPanelTransitionDuration
        panelLeft = ControlPanel.Builder(drawingInformer, AlignHorizontal.LEFT, AlignVertical.CENTER)
            .transition(Transition.TransitionDirection.RIGHT, Transition.TransitionType.SLIDE, transitionDuration)
            .setOrientation(Orientation.VERTICAL)
            .addControl("zoomIn.png", keyFactory.callbackZoomInCenter)
            .addControl("zoomOut.png", keyFactory.callbackZoomOutCenter)
            .addControl("fitToScreen.png", keyFactory.callbackFitUniverseOnScreen)
            .addControl("center.png", keyFactory.callbackCenterView)
            .addControl("undo.png", keyFactory.callbackUndoMovement)
            .build()
        panelTop = ControlPanel.Builder(drawingInformer, AlignHorizontal.CENTER, AlignVertical.TOP)
            .transition(Transition.TransitionDirection.DOWN, Transition.TransitionType.SLIDE, transitionDuration)
            .setOrientation(Orientation.HORIZONTAL)
            .addControl("random.png", keyFactory.callbackRandomLife)
            .addControl("stepSlower.png", keyFactory.callbackStepSlower)
            .addControl("drawSlower.png", keyFactory.callbackDrawSlower)
            .addToggleIconControl("pause.png", "play.png", keyFactory.callbackPause, keyFactory.callbackSingleStep)
            .addControl("drawFaster.png", keyFactory.callbackDrawFaster)
            .addControl("stepFaster.png", keyFactory.callbackStepFaster)
            .addControl("rewind.png", keyFactory.callbackRewind)
            .build()
        panelRight = ControlPanel.Builder(drawingInformer, AlignHorizontal.RIGHT, AlignVertical.CENTER)
            .transition(Transition.TransitionDirection.LEFT, Transition.TransitionType.SLIDE, transitionDuration)
            .setOrientation(Orientation.VERTICAL)
            .addToggleHighlightControl("boundary.png", keyFactory.callbackDrawBounds)
            .addToggleHighlightControl("darkmode.png", keyFactory.callbackThemeToggle)
            .addToggleHighlightControl("singleStep.png", keyFactory.callbackSingleStep)
            .build()
        val panels = listOf(panelLeft, panelTop, panelRight)
        Objects.requireNonNull(instance)?.addAll(panels)
        drawables!!.addAll(panels)
    }

    fun toggleDrawBounds() {
        drawBounds = !drawBounds
    }

    private fun calcCenterOnResize(dimension: BigDecimal, offset: BigDecimal): BigDecimal {
        return dimension.divide(BigTWO, mc) - offset
    }

    private fun createTextPanel(
        existingTextPanel: TextPanel?, // could be null
        builderFunction: () -> TextPanel.Builder
    ): TextPanel {
        existingTextPanel?.let {
            if (drawables!!.isManaging(it)) {
                drawables.remove(it)
            }
        }

        return builderFunction()
            .build()
            .also { newTextPanel ->
                drawables!!.add(newTextPanel)
            }
    }

    fun setupNewLife(life: LifeUniverse) {

        clearUndoDeque()

        val bounds = life.rootBounds
        center(bounds, fitBounds = true, saveState = false)

        countdownText = createTextPanel(countdownText) {
            TextPanel.Builder(
                drawingInformer,
                Theme.countdownText,
                AlignHorizontal.CENTER,
                AlignVertical.CENTER
            )
                .runMethod { patterning.run() }
                .fadeInDuration(2000)
                .countdownFrom(3)
                .textWidth(Optional.of(IntSupplier { canvasWidth.toInt() / 2 }))
                .wrap()
                .textSize(24)
        }
        hudText = createTextPanel(hudText) {
            TextPanel.Builder(drawingInformer, "", AlignHorizontal.RIGHT, AlignVertical.BOTTOM)
                .textSize(24)
                .textWidth(Optional.of(IntSupplier { canvasWidth.toInt() }))
        }
    }


    fun center(bounds: Bounds, fitBounds: Boolean, saveState: Boolean) {
        if (saveState) saveUndoState()

        // remember, bounds are inclusive - if you want the count of discrete items, then you need to add one back to it
        val patternWidth = bounds.width.toBigDecimal()
        val patternHeight = bounds.height.toBigDecimal()

        if (fitBounds) {
            val widthRatio =
                patternWidth.takeIf { it > BigDecimal.ZERO }?.let { canvasWidth.divide(it, mc) } ?: BigDecimal.ONE
            val heightRatio =
                patternHeight.takeIf { it > BigDecimal.ZERO }?.let { canvasHeight.divide(it, mc) } ?: BigDecimal.ONE

            cell.size = (widthRatio.coerceAtMost(heightRatio).toFloat() * .9f)

        }

        val bigCell = cell.size.toBigDecimal()

        val drawingWidth = patternWidth.multiply(bigCell, mc)
        val drawingHeight = patternHeight.multiply(bigCell, mc)
        val halfCanvasWidth = canvasWidth.divide(BigTWO, mc)
        val halfCanvasHeight = canvasHeight.divide(BigTWO, mc)
        val halfDrawingWidth = drawingWidth.divide(BigTWO, mc)
        val halfDrawingHeight = drawingHeight.divide(BigTWO, mc)

        // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
        val offsetX = halfCanvasWidth - halfDrawingWidth + (bounds.left.toBigDecimal() * bigCell.negate())
        val offsetY = halfCanvasHeight - halfDrawingHeight + (bounds.top.toBigDecimal() * bigCell.negate())

        canvasOffsetX = offsetX
        canvasOffsetY = offsetY
    }

    fun clearUndoDeque() {
        undoDeque.clear()
    }

    private fun getHUDMessage(life: LifeUniverse): String {

        return hudInfo.getFormattedString(
            processing.frameCount,
            24
        ) {
            hudInfo.addOrUpdate("fps", processing.frameRate.roundToInt())
            hudInfo.addOrUpdate("dps", DrawRateManager.currentDrawRate.roundToInt())
            hudInfo.addOrUpdate("cell", cell.size)
            hudInfo.addOrUpdate("running", "running".takeIf { patterning.isRunning } ?: "stopped")
            val patternInfo = life.patternInfo.getData()
            patternInfo.forEach { (key, value) ->
                hudInfo.addOrUpdate(key, value)
            }
        }
    }

    fun saveUndoState() {
        undoDeque.add(CanvasState(cell, canvasOffsetX, canvasOffsetY))
    }

    fun handlePause() {
        drawables.takeIf { it!!.isManaging(countdownText!!) }?.let {
            countdownText?.interruptCountdown()
            keyFactory.callbackPause.notifyKeyObservers()
        } ?: patterning.toggleRun()
    }

    private fun updateWindowResized() {

        // create new buffers
        uXBuffer = buffer
        lifeFormBuffer = buffer

        // Calculate the center of the visible portion before resizing
        val centerXBefore = calcCenterOnResize(canvasWidth, canvasOffsetX)
        val centerYBefore = calcCenterOnResize(canvasHeight, canvasOffsetY)

        // Update the canvas size
        canvasWidth = processing.width.toBigDecimal()
        canvasHeight = processing.height.toBigDecimal()

        // Calculate the center of the visible portion after resizing
        val centerXAfter = calcCenterOnResize(canvasWidth, canvasOffsetX)
        val centerYAfter = calcCenterOnResize(canvasHeight, canvasOffsetY)

        updateCanvasOffsets(centerXAfter - centerXBefore, centerYAfter - centerYBefore)
    }

    private fun fillSquare(
        x: Float,
        y: Float,
        size: Float,
        color: Int = Theme.cellColor,
        border: Float = cellBorderWidth
    ) {
        val width = size - border

        lifeFormBuffer.apply {
            fill(color)
            noStroke()
            rect(x, y, width, width)
        }
    }

    private fun drawNode(node: Node, size: BigDecimal, left: BigDecimal, top: BigDecimal) {
        node.population.takeIf { it.isNotZero() } ?: return

        val leftWithOffset = left + canvasOffsetX
        val topWithOffset = top + canvasOffsetY

        // no need to draw anything not visible on screen
        (leftWithOffset + size).let { leftWithOffsetAndSize ->
            (topWithOffset + size).let { topWithOffsetAndSize ->
                if (leftWithOffsetAndSize < BigDecimal.ZERO || topWithOffsetAndSize < BigDecimal.ZERO ||
                    leftWithOffset >= canvasWidth || topWithOffset >= canvasHeight
                ) return
            }
        }

        // if we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size <= BigDecimal.ONE && node.population.isNotZero()) {
            fillSquare(leftWithOffset.toInt().toFloat(), topWithOffset.toInt().toFloat(), 1f)
        } else if (node is LeafNode && node.population.isOne()) {
            fillSquare(leftWithOffset.toInt().toFloat(), topWithOffset.toInt().toFloat(), cell.size)
        } else if (node is InternalNode) {
            val halfSize = getHalfSize(size)
            val leftHalfSize = left + halfSize
            val topHalfSize = top + halfSize
            drawNode(node.nw, halfSize, left, top)
            drawNode(node.ne, halfSize, leftHalfSize, top)
            drawNode(node.sw, halfSize, left, topHalfSize)
            drawNode(node.se, halfSize, leftHalfSize, topHalfSize)
        }
    }

    fun move(dx: Float, dy: Float) {
        saveUndoState()
        updateCanvasOffsets(dx.toBigDecimal(), dy.toBigDecimal())
        lifeFormPosition.add(dx, dy)
    }

    private fun updateCanvasOffsets(offsetX: BigDecimal, offsetY: BigDecimal) {
        canvasOffsetX += offsetX
        canvasOffsetY += offsetY
    }

    fun zoomXY(`in`: Boolean, x: Float, y: Float) {
        zoom(`in`, x, y)
    }

    private fun zoom(zoomIn: Boolean, x: Float, y: Float) {
        saveUndoState()
        val previousCellWidth = cell.size

        // Adjust cell width to align with grid
        cell.zoom(zoomIn)

        // Calculate zoom factor
        val zoomFactor = cell.size / previousCellWidth

        // Calculate the difference in canvas offset-s before and after zoom
        val offsetX = (1 - zoomFactor) * (x - canvasOffsetX.toFloat())
        val offsetY = (1 - zoomFactor) * (y - canvasOffsetY.toFloat())

        // Update canvas offsets
        updateCanvasOffsets(offsetX.toBigDecimal(), offsetY.toBigDecimal())
    }

    fun undoMovement() {
        if (undoDeque.isNotEmpty()) {
            val previous = undoDeque.removeLast()
            cell = previous.cell
            canvasOffsetX = previous.canvasOffsetX
            canvasOffsetY = previous.canvasOffsetY
        }
    }

    private fun drawBounds(life: LifeUniverse) {
        if (!drawBounds) return

        val bounds = life.rootBounds

        // use the bounds of the "living" section of the universe to determine
        // a visible boundary based on the current canvas offsets and cell size
        val boundingBox = calculateBoundingBox(bounds)

        lifeFormBuffer.apply {
            pushStyle()
            noFill()
            stroke(200)
            strokeWeight(1f)
            rect(

                boundingBox.left,
                boundingBox.top,
                boundingBox.width,
                boundingBox.height
            )
            popStyle()
        }

    }

    private data class BoundingBox(val left: Float, val top: Float, val width: Float, val height: Float)

    // all this nonsense with -1 for left and top is necessary to deal with large
    // floating point numbers when the universe gets big
    // we calculate the left, top, width and height and then
    // we figure out if any of it is visible on screen and then make a bounding box that will actually
    // work no matter how big the universe  - without this code, we would end up with boundaries
    // that don't always match up with the drawing - and only when the universe gets really large
    // really tough bug to figure out! consequence of using BigDecimal and converting via toFloat()
    private fun calculateBoundingBox(bounds: Bounds): BoundingBox {
        val cellSize = cell.size.toBigDecimal()
        val leftBD = bounds.left.toBigDecimal()
        val rightBD = bounds.right.toBigDecimal()
        val topBD = bounds.top.toBigDecimal()
        val bottomBD = bounds.bottom.toBigDecimal()

        val leftDecimal = leftBD.multiply(cellSize, mc).add(canvasOffsetX)
        val topDecimal = topBD.multiply(cellSize, mc).add(canvasOffsetY)
        val widthDecimal = rightBD.subtract(leftBD).multiply(cellSize, mc).add(cellSize)
        val heightDecimal = bottomBD.subtract(topBD).multiply(cellSize, mc).add(cellSize)

        val drawingLeft = if (leftDecimal < BigDecimal.ZERO) -1.0f else leftDecimal.toFloat()
        val drawingTop = if (topDecimal < BigDecimal.ZERO) -1.0f else topDecimal.toFloat()

        val calculatedRight = (leftDecimal + widthDecimal).toFloat()
        val calculatedBottom = (topDecimal + heightDecimal).toFloat()

        val drawingRight = if (leftDecimal + widthDecimal > canvasWidth) canvasWidth.toFloat() + 1 else calculatedRight
        val drawingBottom = if (topDecimal + heightDecimal > canvasHeight) canvasHeight.toFloat() + 1 else calculatedBottom

        val drawingWidth = if (drawingLeft == -1.0f) (drawingRight + 1.0f) else (drawingRight - drawingLeft)
        val drawingHeight = if (drawingTop == -1.0f) (drawingBottom + 1.0f) else (drawingBottom - drawingTop)

        return BoundingBox(drawingLeft, drawingTop, drawingWidth, drawingHeight)
    }


    fun draw(life: LifeUniverse, shouldDraw: Boolean) {

        // lambdas are interested in this fact
        isDrawing = true

        if (isWindowResized) {
            updateWindowResized()
        }

        prevWidth = processing.width
        prevHeight = processing.height

        processing.apply {
            background(Theme.backGroundColor)
        }

        uXBuffer.apply {
            beginDraw()
            clear()
        }

        movementHandler.handleRequestedMovement()
        cellBorderWidth = cellBorderWidthRatio * cell.size

        // make this threadsafe
        val hudMessage = getHUDMessage(life)
        hudText?.setMessage(hudMessage)
        drawables!!.drawAll()

        uXBuffer.endDraw()

        if (shouldDraw) {
            val node = life.root
            lifeFormBuffer.apply {
                beginDraw()
                clear()
            }


            //val size = BigDecimal(FlexibleInteger.pow2(node.level - 1).toBigDecimal(), mc).multiply(cell.size.toBigDecimal(), mc)
            val size = FlexibleInteger.pow2(node.level - 1).toBigDecimal().multiply(cell.size.toBigDecimal(), mc)
            drawNode(node, size.multiply(BigTWO, mc), size.negate(), size.negate())
            drawBounds(life)

            lifeFormBuffer.endDraw()
            // reset the position in case you've had mouse moves
            lifeFormPosition[0f] = 0f
        }

        processing.apply {
            image(lifeFormBuffer, lifeFormPosition.x, lifeFormPosition.y)
            image(uXBuffer, 0f, 0f)
        }

        isDrawing = false
    }

    // the cell width times 2 ^ level will give you the size of the whole universe
    // you'll need it it to draw the viewport on screen
    private class Cell(initialSize: Float) {

        var size: Float = initialSize
            set(value) {
                field = when {
                    value > CELL_WIDTH_ROUNDING_THRESHOLD && !zoomingIn -> value.toInt().toFloat()
                    value > CELL_WIDTH_ROUNDING_THRESHOLD -> value.toInt().plus(1).toFloat()
                    else -> value
                }
            }

        private var zoomingIn: Boolean = false

        fun zoom(zoomIn: Boolean) {
            zoomingIn = zoomIn
            val factor = if (zoomIn) 1.25f else 0.8f
            size *= factor
        }

        override fun toString() = "Cell{size=$size}"

        companion object {
            private const val CELL_WIDTH_ROUNDING_THRESHOLD = 1.6f
        }
    }

    private class CanvasState(
        cell: Cell,
        val canvasOffsetX: BigDecimal,
        val canvasOffsetY: BigDecimal
    ) {
        val cell: Cell

        init {
            this.cell = Cell(cell.size)
        }
    }

    // re-using these really seems to make a difference
    private fun getHalfSize(size: BigDecimal): BigDecimal {
        return halfSizeMap.getOrPut(size) { size.divide(BigTWO, mc) }
    }

    companion object {
        // without this precision on the MathContext, small imprecision propagates at
        // large levels on the LifeUniverse - sometimes this will cause the image to jump around or completely
        // off the screen.  don't skimp on precision!
        val mc // = MathContext(400)
            get() = Bounds.mathContext
        private val BigTWO = BigDecimal(2)
        private val undoDeque = ArrayDeque<CanvasState>()
        private const val DEFAULT_CELL_WIDTH = 4.0f
    }
}