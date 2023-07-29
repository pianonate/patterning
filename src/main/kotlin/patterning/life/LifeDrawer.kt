package patterning.life

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.math.BigDecimal
import java.math.MathContext
import java.net.URISyntaxException
import java.util.Optional
import java.util.function.IntSupplier
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import patterning.Drawer
import patterning.PatterningPApplet
import patterning.RunningState
import patterning.Theme
import patterning.actions.KeyFactory
import patterning.actions.KeyHandler
import patterning.actions.MouseEventManager
import patterning.actions.MovementHandler
import patterning.informer.DrawingInfoSupplier
import patterning.informer.DrawingInformer
import patterning.panel.AlignHorizontal
import patterning.panel.AlignVertical
import patterning.panel.ControlPanel
import patterning.panel.Orientation
import patterning.panel.TextPanel
import patterning.panel.Transition
import patterning.util.AsyncCalculationRunner
import patterning.util.FlexibleInteger
import patterning.util.ResourceManager
import patterning.util.StatMap
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector

class LifeDrawer(
    private val processing: PApplet,
    var storedLife: String,

    ) {

    private var life: LifeUniverse
    private val asyncNextGeneration: AsyncCalculationRunner
    private var targetStep = 0
    private val drawingInformer: DrawingInfoSupplier
    private val patterning: PatterningPApplet = processing as PatterningPApplet
    private val hudInfo: HUDStringBuilder
    private val movementHandler: MovementHandler

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

    private val nodePath: DrawNodePath = DrawNodePath()

    private var countdownText: TextPanel? = null
    private var hudText: TextPanel? = null

    private var lifeFormBuffer: PGraphics
    private var uXBuffer: PGraphics
    private var drawBounds: Boolean


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

        canvasWidth = processing.width.toBigDecimal()
        canvasHeight = processing.height.toBigDecimal()
        cell = Cell()

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

        val keyFactory = KeyFactory(patterning, this)
        keyFactory.setupSimpleKeyCallbacks() // the ones that don't need controls
        setupControls(keyFactory)

        asyncNextGeneration = AsyncCalculationRunner() { asyncNextGeneration() }

        // on startup, storedLife may be loaded from the properties file but if it's not
        // just get a random one
        if (storedLife.isEmpty()) {
            getRandomLifeform(false) // todo: convoluted reset boolean
        }
        // life will have been loaded in prior - either from saved life
        // or from the packaged resources so this doesn't need extra protection
        life = instantiateLifeform()
    }

    private val buffer: PGraphics
        get() = processing.createGraphics(processing.width, processing.height)

    private val isWindowResized: Boolean
        get() {
            val widthChanged = prevWidth != processing.width
            val heightChanged = prevHeight != processing.height
            return widthChanged || heightChanged
        }

    private fun setupControls(keyFactory: KeyFactory) {

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
            .addControl("random.png", keyFactory.callbackRandomPattern)
            .addControl("stepSlower.png", keyFactory.callbackStepSlower)
            // .addControl("drawSlower.png", keyFactory.callbackDrawSlower)
            .addPlayPauseControl(
                "play.png",
                "pause.png",
                keyFactory.callbackPause,
            )
            //.addControl("drawFaster.png", keyFactory.callbackDrawFaster)
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

        MouseEventManager.addAll(panels)
        Drawer.addAll(panels)
    }

    fun pasteLifeForm() {
        try {
            // Get the system clipboard
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard

            // Check if the clipboard contains text data and then get it
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                storedLife = clipboard.getData(DataFlavor.stringFlavor) as String
                instantiateLifeform()
            }
        } catch (e: UnsupportedFlavorException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun fitUniverseOnScreen() {
        center(life.rootBounds, fitBounds = true, saveState = true)
    }


    fun centerView() {
        center(life.rootBounds, fitBounds = false, saveState = true)
    }

    fun getRandomLifeform(reset: Boolean) {
        try {
            storedLife = ResourceManager.instance!!.getRandomResourceAsString(ResourceManager.RLE_DIRECTORY)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
        if (reset) life = instantiateLifeform()
    }

    val numberedLifeForm: Unit
        get() {

            // subclasses of PApplet will have a keyCode
            // so this isn't magical
            val number = KeyHandler.latestKeyCode - '0'.code
            try {
                storedLife =
                    ResourceManager.instance!!.getResourceAtFileIndexAsString(ResourceManager.RLE_DIRECTORY, number)
                setNumberedLifeForm(storedLife)
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }
        }

    private fun instantiateLifeform(): LifeUniverse {
        RunningState.pause()
        asyncNextGeneration.cancelAndWait()

        val universe = LifeUniverse()
        targetStep = 0
        universe.step = 0

        try {

            // instance variables - do they need to be?
            val parser = LifeFormats()
            val newLife = parser.parseRLE(storedLife)
            universe.setupLife(newLife.fieldX!!, newLife.fieldY!!)
        } catch (e: NotLifeException) {
            // todo: on failure you need to
            PApplet.println(
                """
    get a life - here's what failed:
    
    ${e.message}
    """.trimIndent()
            )
        }
        // new instances only created in instantiateLife to keep things simple
        // lifeForm not made local as it is intended to be used with display functions in the future
        setupNewLife(universe)

        return universe

    }

    fun handleStep(faster: Boolean) {
        var increment = if (faster) 1 else -1
        if (targetStep + increment < 0) increment = 0
        targetStep += increment
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
            if (Drawer.isManaging(it)) {
                Drawer.remove(it)
            }
        }

        return builderFunction()
            .build()
            .also { newTextPanel ->
                Drawer.add(newTextPanel)
            }
    }

    private fun setupNewLife(life: LifeUniverse) {

        undoDeque.clear()

        val bounds = life.rootBounds
        updateLargestDimension(bounds)
        center(bounds, fitBounds = true, saveState = false)

        countdownText = createTextPanel(countdownText) {
            TextPanel.Builder(
                drawingInformer,
                Theme.countdownText,
                AlignHorizontal.CENTER,
                AlignVertical.CENTER
            )
                .runMethod {
                    RunningState.run()
                }
                .fadeInDuration(2000)
                .countdownFrom(3)
                .textWidth(Optional.of(IntSupplier { processing.width / 2 }))
                .wrap()
                .textSize(24)
        }
        hudText = createTextPanel(hudText) {
            TextPanel.Builder(drawingInformer, "", AlignHorizontal.RIGHT, AlignVertical.BOTTOM)
                .textSize(24)
                .textWidth(Optional.of(IntSupplier { canvasWidth.toInt() }))
        }

        positionMap.clear()

    }


    private fun center(bounds: Bounds, fitBounds: Boolean, saveState: Boolean) {
        if (saveState) saveUndoState()

        // remember, bounds are inclusive - if you want the count of discrete items, then you need to add one back to it
        val patternWidth = bounds.width.bigDecimal
        val patternHeight = bounds.height.bigDecimal

        if (fitBounds) {
            val widthRatio =
                patternWidth.takeIf { it > BigDecimal.ZERO }?.let { canvasWidth.divide(it, mc) } ?: BigDecimal.ONE
            val heightRatio =
                patternHeight.takeIf { it > BigDecimal.ZERO }?.let { canvasHeight.divide(it, mc) } ?: BigDecimal.ONE

            cell.size = (widthRatio.coerceAtMost(heightRatio) * BigDecimal.valueOf(.9)).toFloat()
        }

        val bigCell = cell.bigSize

        val drawingWidth = patternWidth.multiply(bigCell, mc)
        val drawingHeight = patternHeight.multiply(bigCell, mc)
        val halfCanvasWidth = canvasWidth.divide(BigTWO, mc)
        val halfCanvasHeight = canvasHeight.divide(BigTWO, mc)
        val halfDrawingWidth = drawingWidth.divide(BigTWO, mc)
        val halfDrawingHeight = drawingHeight.divide(BigTWO, mc)

        // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
        val offsetX = halfCanvasWidth - halfDrawingWidth + (bounds.left.bigDecimal * -bigCell)
        val offsetY = halfCanvasHeight - halfDrawingHeight + (bounds.top.bigDecimal * -bigCell)

        updateCanvasOffsets(offsetX, offsetY)

    }


    private fun getHUDMessage(life: LifeUniverse): String {

        return hudInfo.getFormattedString(
            processing.frameCount,
            12
        ) {
            hudInfo.addOrUpdate("fps", processing.frameRate.roundToInt())
            hudInfo.addOrUpdate("gps", asyncNextGeneration.getRate())
            hudInfo.addOrUpdate("cell", cell.size)
            hudInfo.addOrUpdate("running", RunningState.runMessage())
            //            hudInfo.addOrUpdate("actuals", actualRecursions)
            hudInfo.addOrUpdate("stack saves", startDelta)
            val patternInfo = life.lifeInfo.info
            patternInfo.forEach { (key, value) ->
                hudInfo.addOrUpdate(key, value)
            }
            hudInfo.addOrUpdate("posHits", positionMap.hitRate)
        }
    }

    private data class CanvasState(
        val cell: Cell,
        val canvasOffsetX: BigDecimal,
        val canvasOffsetY: BigDecimal
    )

    fun saveUndoState() {
        undoDeque.add(CanvasState(Cell(cell.size), canvasOffsetX, canvasOffsetY))
    }

    fun handlePlay() {
        Drawer.takeIf { Drawer.isManaging(countdownText!!) }?.let {
            countdownText?.interruptCountdown()
        } ?: RunningState.toggleRunning()
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

        adjustCanvasOffsets(centerXAfter - centerXBefore, centerYAfter - centerYBefore)
    }

    private val fillSquareMutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun fillSquare(
        x: Float,
        y: Float,
        size: Float,
        color: Int = Theme.cellColor
    ) {
        fillSquareMutex.withLock {
            val width = size - cellBorderWidth

            lifeFormBuffer.apply {
                fill(color)
                noStroke()
                rect(x, y, width, width)
            }
        }
    }

    // Initialize viewPath, also at class level
    private var actualRecursions = FlexibleInteger.ZERO
    private var startDelta = 0

    private val actualRecursionsMutex = kotlinx.coroutines.sync.Mutex()

    private fun drawPattern(life: LifeUniverse) {
        lifeFormBuffer.beginDraw()
        lifeFormBuffer.clear()

        // getStartingEntry returns a DrawNodePathEntry - which is precalculated
        // to traverse to the first node that has children visible on screen
        // for very large drawing this can save hundreds of stack calls
        // making debugging (at least) easier
        //
        // there may be some performance gain to this although i doubt it's a lot
        // this is more for the thrill of solving a complicated problem and it's
        // no small thing that stack traces become much smaller
        //with(getStartingEntry(life)) {
        with(nodePath.getLowestEntryFromRoot(life.root)) {
            actualRecursions = FlexibleInteger.ZERO


            val startingNode = node
            val size = size
            val offsetX = left
            val offsetY = top

            startDelta = life.root.level - startingNode.level

            runBlocking {
                val divisor = 4.toBigDecimal()
                largePopulationThreshold =
                    FlexibleInteger(startingNode.population.bigDecimal.divide(divisor, mc).toBigInteger())
                // largePopulationThreshold = FlexibleInteger.MAX_VALUE
                drawNodeRecurse(startingNode, size, offsetX, offsetY)
            }
        }

        // keep this around - it works - so if your startingNode code has issues, you can resuscitate
        //drawNodeRecurse(life.root, size, -halfSize, -halfSize)

        drawBounds(life)

        lifeFormBuffer.endDraw()
        // reset the position in case you've had mouse moves
        lifeFormPosition[0f] = 0f
    }

    private var largePopulationThreshold = FlexibleInteger.ZERO

    private data class PositionKey(val pos: BigDecimal, val offset: BigDecimal)

    private suspend fun drawNodeRecurse(
        node: Node,
        size: BigDecimal,
        left: BigDecimal,
        top: BigDecimal
    ) {
        actualRecursionsMutex.withLock {
            ++actualRecursions
        }

        // Check if we should continue
        if (!shouldContinue(node, size, left, top)) {
            return
        }

        //val leftWithOffset = left + canvasOffsetX
        // val topWithOffset = top + canvasOffsetY

        val leftWithOffset = getMappedPosition(left, canvasOffsetX)
        val topWithOffset = getMappedPosition(top, canvasOffsetY)

        // If we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size <= BigDecimal.ONE && node.population.isNotZero()) {
            fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), 1f)
        } else if (node is LeafNode && node.population.isOne()) {
            fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), cell.size)
        } else if (node is TreeNode) {

            val halfSize = cell.halfUniverseSize(node.level)
            /*            val leftHalfSize = left + halfSize
                        val topHalfSize = top + halfSize*/
            val leftHalfSize = getMappedPosition(left, halfSize)
            val topHalfSize = getMappedPosition(top, halfSize)

            if (node.population > largePopulationThreshold) {
                coroutineScope {
                    listOf(
                        async { drawNodeRecurse(node.nw, halfSize, left, top) },
                        async { drawNodeRecurse(node.ne, halfSize, leftHalfSize, top) },
                        async { drawNodeRecurse(node.sw, halfSize, left, topHalfSize) },
                        async { drawNodeRecurse(node.se, halfSize, leftHalfSize, topHalfSize) }
                    ).awaitAll()
                }
            } else { // If the node population is small, continue on the same coroutine
                drawNodeRecurse(node.nw, halfSize, left, top)
                drawNodeRecurse(node.ne, halfSize, leftHalfSize, top)
                drawNodeRecurse(node.sw, halfSize, left, topHalfSize)
                drawNodeRecurse(node.se, halfSize, leftHalfSize, topHalfSize)
            }
        }
    }

    fun move(dx: Float, dy: Float) {
        saveUndoState()
        adjustCanvasOffsets(dx.toBigDecimal(), dy.toBigDecimal())
        lifeFormPosition.add(dx, dy)
    }

    private fun adjustCanvasOffsets(dx: BigDecimal, dy: BigDecimal) {
        updateCanvasOffsets(canvasOffsetX + dx, canvasOffsetY + dy)
    }

    private fun updateCanvasOffsets(offsetX: BigDecimal, offsetY: BigDecimal) {
        canvasOffsetX = offsetX
        canvasOffsetY = offsetY
        nodePath.offsetsMoved = true
        // positionMap.clear()
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
        adjustCanvasOffsets(offsetX.toBigDecimal(), offsetY.toBigDecimal())
    }

    fun undoMovement() {
        if (undoDeque.isNotEmpty()) {
            val previous = undoDeque.removeLast()
            cell = previous.cell
            updateCanvasOffsets(previous.canvasOffsetX, previous.canvasOffsetY)
        }
    }

    private fun drawBounds(life: LifeUniverse) {
        if (!drawBounds) return

        val bounds = life.rootBounds

        // use the bounds of the "living" section of the universe to determine
        // a visible boundary based on the current canvas offsets and cell size
        val boundingBox = BoundingBox(bounds, cell.bigSize)
        boundingBox.draw(lifeFormBuffer)

        var currentLevel = life.root.level - 2

        while (currentLevel < life.root.level) {
            val halfSize = FlexibleInteger.pow2(currentLevel)
            val universeBox = BoundingBox(Bounds(-halfSize, -halfSize, halfSize, halfSize), cell.bigSize)
            universeBox.draw(lifeFormBuffer, drawCrosshair = true)
            currentLevel++
        }
    }

    private data class Point(val x: Float, val y: Float)

    private data class Line(val start: Point, val end: Point) {
        fun drawDashedLine(buffer: PGraphics, dashLength: Float, spaceLength: Float) {
            val x1 = start.x
            val y1 = start.y
            val x2 = end.x
            val y2 = end.y

            val distance = PApplet.dist(x1, y1, x2, y2)
            val numDashes = distance / (dashLength + spaceLength)

            var draw = true
            var x = x1
            var y = y1
            buffer.pushStyle()
            buffer.strokeWeight(Theme.strokeWeightDashedLine)
            for (i in 0 until (numDashes * 2).toInt()) {
                if (draw) {
                    // We limit the end of the dash to be at maximum the final point
                    val dxDash = (x2 - x1) / numDashes / 2
                    val dyDash = (y2 - y1) / numDashes / 2
                    val endX = (x + dxDash).coerceAtMost(x2)
                    val endY = (y + dyDash).coerceAtMost(y2)
                    buffer.line(x, y, endX, endY)
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
            buffer.popStyle()
        }
    }

    private class BoundingBox(bounds: Bounds, cellSize: BigDecimal) {
        // we draw the box just a bit off screen so it won't be visible
        // but if the box is more than a pixel, we need to push it further offscreen
        // since we're using a Theme constant we can change we have to account for it
        private val positiveOffScreen = Theme.strokeWeightBounds
        private val negativeOffScreen = -positiveOffScreen

        private val leftBD = bounds.left.bigDecimal
        private val topBD = bounds.top.bigDecimal

        private val leftWithOffset = leftBD.multiply(cellSize, mc).add(canvasOffsetX)
        private val topWithOffset = topBD.multiply(cellSize, mc).add(canvasOffsetY)

        private val widthDecimal = bounds.width.bigDecimal.multiply(cellSize, mc)
        private val heightDecimal = bounds.height.bigDecimal.multiply(cellSize, mc)

        private val rightFloat = (leftWithOffset + widthDecimal).toFloat()
        private val bottomFloat = (topWithOffset + heightDecimal).toFloat()

        // coerce boundaries to be drawable with floats
        val left = if (leftWithOffset < BigDecimal.ZERO) negativeOffScreen else leftWithOffset.toFloat()
        val top = if (topWithOffset < BigDecimal.ZERO) negativeOffScreen else topWithOffset.toFloat()

        val right =
            if (leftWithOffset + widthDecimal > canvasWidth) canvasWidth.toFloat() + positiveOffScreen else rightFloat
        val bottom =
            if (topWithOffset + heightDecimal > canvasHeight) canvasHeight.toFloat() + positiveOffScreen else bottomFloat

        val width = if (left == negativeOffScreen) (right + positiveOffScreen) else (right - left)
        val height = if (top == negativeOffScreen) (bottom + positiveOffScreen) else (bottom - top)

        // Calculate Lines
        private val horizontalLine: Line
            get() {
                val startX = left
                val startYDecimal = topWithOffset + heightDecimal.divide(BigTWO, mc)
                val startY = when {
                    startYDecimal < BigDecimal.ZERO -> -1f
                    startYDecimal > canvasHeight -> canvasHeight.toFloat() + 1
                    else -> startYDecimal.toFloat()
                }
                val endXDecimal = leftWithOffset + widthDecimal
                val endX = if (endXDecimal > canvasWidth) canvasWidth.toFloat() + 1 else endXDecimal.toFloat()
                return Line(Point(startX, startY), Point(endX, startY))
            }


        private val verticalLine: Line
            get() {
                val startXDecimal = leftWithOffset + widthDecimal.divide(BigTWO, mc)
                val startX = when {
                    startXDecimal < BigDecimal.ZERO -> -1f
                    startXDecimal > canvasWidth -> canvasWidth.toFloat() + 1
                    else -> startXDecimal.toFloat()
                }
                val endYDecimal = topWithOffset + heightDecimal
                val endY = if (endYDecimal > canvasHeight) canvasHeight.toFloat() + 1 else endYDecimal.toFloat()
                return Line(Point(startX, top), Point(startX, endY))
            }


        private fun drawCrossHair(buffer: PGraphics, dashLength: Float, spaceLength: Float) {
            horizontalLine.drawDashedLine(buffer, dashLength, spaceLength)
            verticalLine.drawDashedLine(buffer, dashLength, spaceLength)
        }

        fun draw(buffer: PGraphics, drawCrosshair: Boolean = false) {

            buffer.pushStyle()
            buffer.noFill()
            buffer.stroke(Theme.textColor)
            buffer.strokeWeight(Theme.strokeWeightBounds)
            buffer.rect(left, top, width, height)
            if (drawCrosshair) {
                drawCrossHair(buffer, Theme.dashedLineDashLength, Theme.dashedLineSpaceLength)
            }

            buffer.popStyle()
        }
    }


    private fun drawBackground() {
        if (isWindowResized) {
            updateWindowResized()
        }

        prevWidth = processing.width
        prevHeight = processing.height

        processing.apply {
            background(Theme.backGroundColor)
        }

    }

    private fun drawUX(life: LifeUniverse) {
        uXBuffer.apply {
            beginDraw()
            clear()
        }

        movementHandler.handleRequestedMovement()
        cellBorderWidth = cell.size * Cell.WIDTH_RATIO

        val hudMessage = getHUDMessage(life)
        hudText?.setMessage(hudMessage)
        Drawer.drawAll()

        uXBuffer.endDraw()
    }


    fun draw(/*life: LifeUniverse*/) {

        // lambdas are interested in this fact
        isDrawing = true

        drawBackground()
        drawUX(life)
        drawPattern(life)

        processing.apply {
            image(lifeFormBuffer, lifeFormPosition.x, lifeFormPosition.y)
            image(uXBuffer, 0f, 0f)
        }

        isDrawing = false

        goForwardInTime()
    }

    private fun goForwardInTime() {

        if (!asyncNextGeneration.isRunning) {
            with(life) {
                step = when {
                    step < targetStep -> step + 1
                    step > targetStep -> step - 1
                    else -> step
                }
            }
        }

        if (RunningState.shouldAdvance())
            asyncNextGeneration.startCalculation()
    }

    private suspend fun asyncNextGeneration() {
        coroutineScope { life.nextGeneration() }
        // targetStep += 1 // use this for testing - later on you can implement lightspeed around this
    }

    fun setNumberedLifeForm(storedLife: String) {
        this.storedLife = storedLife
        life = instantiateLifeform()
    }

    fun rewind() {
        life = instantiateLifeform()
    }

    companion object {
        // without this precision on the MathContext, small imprecision propagates at
        // large levels on the LifeUniverse - sometimes this will cause the image to jump around or completely
        // off the screen.  don't skimp on precision!
        // Bounds.mathContext is kept up to date with the largest dimension of the universe
        var mc = MathContext(0)

        // i was wondering why empirically we needed a PRECISION_BUFFER to add to the precision
        // now that i'm thinking about it, this is probably the required precision for a float
        // which is what the cell.cellSize is - especially for really small numbers
        // without it we'd be off by only looking at the integer part of the largest dimension
        private const val PRECISION_BUFFER = 10
        private var largestDimension: FlexibleInteger = FlexibleInteger.ZERO
        private var previousPrecision: Int = 0

        fun resetMathContext() {
            largestDimension = FlexibleInteger.ZERO
            previousPrecision = 0
        }

        // as nodes are created their bounds are calculated
        // when the bounds get larger, we need a math context to draw with that
        // allows the pattern to be drawn with the necessary precision
        fun updateLargestDimension(bounds: Bounds) {
            val dimension = bounds.width.max(bounds.height)
            if (dimension > largestDimension) {
                largestDimension = dimension
                // Assuming minBaseToExceed is a function on FlexibleInteger
                val precision = largestDimension.minPrecisionForDrawing()
                if (precision != previousPrecision) {
                    mc = MathContext(precision + PRECISION_BUFFER)
                    previousPrecision = precision
                }
            }
        }

        private val BigTWO = BigDecimal(2)
        private val undoDeque = ArrayDeque<CanvasState>()
        private var canvasOffsetX = BigDecimal.ZERO
        private var canvasOffsetY = BigDecimal.ZERO

        // if we're going to be operating in BigDecimal then we keep these that way so
        // that calculations can be done without conversions until necessary
        private var canvasWidth: BigDecimal = BigDecimal.ZERO
        private var canvasHeight: BigDecimal = BigDecimal.ZERO
        lateinit var cell: Cell
        private val positionMap = StatMap(mutableMapOf<PositionKey, BigDecimal>())

        private fun getMappedPosition(pos: BigDecimal, offset: BigDecimal): BigDecimal =
            positionMap.getOrPut(PositionKey(pos, offset)) { pos + offset }


        // used by PatternDrawer a well as DrawNodePath
        // maintains no state so it can live here as a utility function
        fun shouldContinue(
            node: Node,
            size: BigDecimal,
            nodeLeft: BigDecimal,
            nodeTop: BigDecimal
        ): Boolean {
            if (node.population.isZero()) {
                return false
            }

            /*            val left = nodeLeft + canvasOffsetX
                        val top = nodeTop + canvasOffsetY*/
            // positionMap.getOrPut(PositionKey(nodeLeft, canvasOffsetX)) { nodeLeft + canvasOffsetX }
            // positionMap.getOrPut(PositionKey(nodeTop, canvasOffsetY)) { nodeTop + canvasOffsetY }
            val left = getMappedPosition(nodeLeft, canvasOffsetX)
            val top = getMappedPosition(nodeTop, canvasOffsetY)


            // No need to draw anything not visible on screen
            /*      val right = left + size
                  val bottom = top + size*/
            val right = getMappedPosition(left, size)
            val bottom = getMappedPosition(top, size)


            // left and top are defined by the zoom level (cell size) multiplied
            // by half the universe size which we start out with at the nw corner which is half the size negated
            // each level in we add half of the size at that to the  create the new size for left and top
            // to that, we add the canvasOffsetX to left and canvasOffsetY to top
            //
            // the size at this level is then added to the left to get the right side of the universe
            // and the size is added to the top to get the bottom of the universe
            // then we see if this universe is inside the canvas by looking to see if
            // in any direction it is outside.
            //
            // if the right side is less than zero it's to the left of the canvas
            // if the bottom is less than zero it's above the canvas
            // if the left is larger than the width then we're to the right of the canvas
            // if the top is larger than the height we're below the canvas
            return !(right < BigDecimal.ZERO || bottom < BigDecimal.ZERO ||
                    left >= canvasWidth || top >= canvasHeight)
        }
    }
}