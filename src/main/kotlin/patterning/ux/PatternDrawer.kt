package patterning.ux

import patterning.*
import patterning.actions.KeyFactory
import patterning.actions.MouseEventManager.Companion.instance
import patterning.actions.MovementHandler
import patterning.util.FlexibleInteger
import patterning.util.StatMap
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

    private data class CanvasState(
        val cell: Cell,
        val canvasOffsetX: BigDecimal,
        val canvasOffsetY: BigDecimal
    )

    private data class DrawNodePathEntry(
        val node: Node,
        val size: BigDecimal,
        val left: BigDecimal,
        val top: BigDecimal,
        val direction: Direction
    )

    private data class DrawNodePath(
        var shouldUpdate: Boolean,
        val path: MutableList<DrawNodePathEntry> = mutableListOf(),
        var level: Int
    ) {
        fun updateRoot(root: DrawNodePathEntry) {
            path[0] = root
        }

        fun clear() {
            if (path.size > 1) {
                path.subList(1, path.size).clear()
            }
        }

        fun lowestEntry(root: InternalNode): DrawNodePathEntry {
            var currentNode: Node = root
            var lastEntry = path[0]

            if (path.size == 1) {
                return lastEntry
            }

            for (entry in path) {
                lastEntry = entry
                currentNode = when (entry.direction) {
                    Direction.ROOT -> currentNode
                    Direction.NW -> (currentNode as InternalNode).nw
                    Direction.NE -> (currentNode as InternalNode).ne
                    Direction.SW -> (currentNode as InternalNode).sw
                    Direction.SE -> (currentNode as InternalNode).se
                }
            }

            return DrawNodePathEntry(
                node = currentNode,
                size = lastEntry.size,
                left = lastEntry.left,
                top = lastEntry.top,
                direction = lastEntry.direction
            )
        }

    }

    private enum class Direction {
        NW, NE, SW, SE, ROOT
    }

    private val cellBorderWidthRatio = .05f
    private val drawingInformer: DrawingInfoSupplier
    private val patterning: Processing = processing as Processing
    private val hudInfo: HUDStringBuilder
    private val movementHandler: MovementHandler

    // ain't no way to do drawing without a singleton drawables manager
    private val drawables = Drawer.instance
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

    private val nodePath = DrawNodePath(shouldUpdate = true, level = 0).apply {
        path.add(DrawNodePathEntry(Node.deadNode, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Direction.NW))
    }

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
                .textWidth(Optional.of(IntSupplier { processing.width / 2 }))
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

            // at level 160 or just above, the cell.size becomes zero in the following
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
        val offsetX = halfCanvasWidth - halfDrawingWidth + (bounds.left.toBigDecimal() * -bigCell)
        val offsetY = halfCanvasHeight - halfDrawingHeight + (bounds.top.toBigDecimal() * -bigCell)

        updateCanvasOffsets(offsetX, offsetY)

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
            hudInfo.addOrUpdate("frames", processing.frameCount)
            hudInfo.addOrUpdate("dps", Governor.currentDrawRate.roundToInt())
            hudInfo.addOrUpdate("gps", patterning.gps)
            hudInfo.addOrUpdate("cell", cell.size)
            hudInfo.addOrUpdate("running", "running".takeIf { patterning.isRunning } ?: "stopped")
            hudInfo.addOrUpdate("actuals", actualRecursions)
            hudInfo.addOrUpdate("stack saves", startDelta)
            val patternInfo = life.patternInfo.getData()
            patternInfo.forEach { (key, value) ->
                hudInfo.addOrUpdate(key, value)
            }
        }
    }

    fun saveUndoState() {
        undoDeque.add(CanvasState(Cell(cell.size), canvasOffsetX, canvasOffsetY))
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

        adjustCanvasOffsets(centerXAfter - centerXBefore, centerYAfter - centerYBefore)
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

    private fun updateNodePath(
        node: Node,
        size: BigDecimal,
        left: BigDecimal,
        top: BigDecimal
    ) {
        if (node.population.isZero()) {
            return
        }

        if (node is InternalNode) {
            val halfSize = cell.halfUniverseSize(node.level)
            val leftHalfSize = left + halfSize
            val topHalfSize = top + halfSize

            // Check all children at each level, and their relative position adjustments
            val childrenAndOffsets = listOf(
                DrawNodePathEntry(node.nw, halfSize, left, top, Direction.NW),
                DrawNodePathEntry(node.ne, halfSize, leftHalfSize, top, Direction.NE),
                DrawNodePathEntry(node.sw, halfSize, left, topHalfSize, Direction.SW),
                DrawNodePathEntry(node.se, halfSize, leftHalfSize, topHalfSize, Direction.SE)
            )

            val intersectingChildrenAndOffsets = childrenAndOffsets.filter { child ->
                shouldContinue(
                    child.node,
                    child.size,
                    child.left,
                    child.top
                )
            }

            if (intersectingChildrenAndOffsets.size == 1) {

                val intersectingChild = intersectingChildrenAndOffsets.first()
                nodePath.path.add(intersectingChild)
                updateNodePath(
                    intersectingChild.node,
                    intersectingChild.size,
                    intersectingChild.left,
                    intersectingChild.top
                )
            }
        }
    }

    // Initialize viewPath, also at class level
    private var actualRecursions = FlexibleInteger.ZERO
    private var startDelta = 0


    private fun drawNode(life: LifeUniverse) {
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
        with(getStartingEntry(life)) {
            actualRecursions = FlexibleInteger.ZERO

            val startingNode = node
            val size = size
            val offsetX = left
            val offsetY = top

            startDelta = life.root.level - startingNode.level

            drawNodeRecurse(startingNode, size, offsetX, offsetY)
        }

        // keep this around - it works - so if your startingNode code has issues, you can resuscitate
        //drawNodeRecurse(life.root, size, -halfSize, -halfSize)

        drawBounds(life)

        lifeFormBuffer.endDraw()
        // reset the position in case you've had mouse moves
        lifeFormPosition[0f] = 0f
    }

    private fun getStartingEntry(life: LifeUniverse): DrawNodePathEntry {
        val halfSize = cell.halfUniverseSize(life.root.level)
        val universeSize = cell.universeSize(life.root.level)

        nodePath.updateRoot(
            DrawNodePathEntry(
                life.root,
                universeSize,
                -halfSize,
                -halfSize,
                Direction.ROOT
            )
        )

        // i don't like that this object relies on two properties to determine
        // moving forward - and having to reset the starting root is necessary but also seems to lack
        // encapsulation - it's necessary because the life.root is likely to change each generation
        // and we always have to start traversing the nodePath to get to the lowestNode from the
        // root - it also makes path management a little easier in the nodePath
        // take a look at refactoring this for clarity - at least extract a function here.
        if (nodePath.shouldUpdate || nodePath.level != life.root.level) {
            nodePath.clear()
            updateNodePath(life.root, universeSize, -halfSize, -halfSize)
            nodePath.shouldUpdate = false
            nodePath.level = life.root.level
        }

        return nodePath.lowestEntry(life.root)
    }

    private fun shouldContinue(
        node: Node,
        size: BigDecimal,
        nodeLeft: BigDecimal,
        nodeTop: BigDecimal
    ): Boolean {
        if (node.population.isZero()) {
            return false
        }

        val left = nodeLeft + canvasOffsetX
        val top = nodeTop + canvasOffsetY

        // No need to draw anything not visible on screen
        val right = left + size
        val bottom = top + size

        /*val shouldContinue = DebugLowest(node.id, top, left, bottom, right)
        println("recurse: $shouldContinue")*/

        // left and top are defined by the zoom level (cell size) multiplied
        // by half the universe size which we start out with at the nw corner which is half the size negated
        // each level in we add half of the size at that to the  create the new size for left and top
        // to that, we add the canvasOffsetX to left and canvasOffsetY to top - and these
        // are what is passed in to this function
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


    private fun drawNodeRecurse(
        node: Node,
        size: BigDecimal,
        left: BigDecimal,
        top: BigDecimal
    ) {
        ++actualRecursions

        // Check if we should continue
        if (!shouldContinue(node, size, left, top)) {
            return
        }

        val leftWithOffset = left + canvasOffsetX
        val topWithOffset = top + canvasOffsetY

        // If we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size <= BigDecimal.ONE && node.population.isNotZero()) {
            fillSquare(leftWithOffset.toInt().toFloat(), topWithOffset.toInt().toFloat(), 1f)
        } else if (node is LeafNode && node.population.isOne()) {
            fillSquare(leftWithOffset.toInt().toFloat(), topWithOffset.toInt().toFloat(), cell.size)
        } else if (node is InternalNode) {

            val halfSize = cell.halfUniverseSize(node.level)
            val leftHalfSize = left + halfSize
            val topHalfSize = top + halfSize

            drawNodeRecurse(node.nw, halfSize, left, top)
            drawNodeRecurse(node.ne, halfSize, leftHalfSize, top)
            drawNodeRecurse(node.sw, halfSize, left, topHalfSize)
            drawNodeRecurse(node.se, halfSize, leftHalfSize, topHalfSize)
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
        nodePath.shouldUpdate = true
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
        val boundingBox = calculateBoundingBox(bounds)
        val halfSize = FlexibleInteger.pow2(life.root.level - 1)
        val universeBox = calculateBoundingBox(Bounds(-halfSize, -halfSize, halfSize, halfSize))

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
            rect(

                universeBox.left,
                universeBox.top,
                universeBox.width,
                universeBox.height
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
        val cellSize = cell.bigSize
        val leftBD = bounds.left.toBigDecimal()
        val topBD = bounds.top.toBigDecimal()

        val leftDecimal = leftBD.multiply(cellSize, mc).add(canvasOffsetX)
        val topDecimal = topBD.multiply(cellSize, mc).add(canvasOffsetY)

        val widthDecimal = bounds.width.toBigDecimal().multiply(cellSize, mc)
        val heightDecimal = bounds.height.toBigDecimal().multiply(cellSize, mc)

        // doesn't matter if it's arbitrarily (huge universe) far away from the canvas, we just need to push it off screen
        val drawingLeft = if (leftDecimal < BigDecimal.ZERO) -1.0f else leftDecimal.toFloat()
        val drawingTop = if (topDecimal < BigDecimal.ZERO) -1.0f else topDecimal.toFloat()

        val calculatedRight = (leftDecimal + widthDecimal).toFloat()
        val calculatedBottom = (topDecimal + heightDecimal).toFloat()

        val drawingRight = if (leftDecimal + widthDecimal > canvasWidth) canvasWidth.toFloat() + 1 else calculatedRight
        val drawingBottom =
            if (topDecimal + heightDecimal > canvasHeight) canvasHeight.toFloat() + 1 else calculatedBottom

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

            drawNode(life)
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
        private var bigSizeCached: BigDecimal = BigDecimal.ZERO // Cached value initialized with initial size

        // surprisingly caching the result of the half size calculation provides
        // a remarkable speed boost - added CachedMap to track the results of getOrPut()
        // it's pretty profound how many calls to BigDecimal.multiply we can avoid in
        // universeSizeImpl - the cache hit rate gets to 99.99999% pretty quickly
        // private val sizeMap: MutableMap<Int, BigDecimal> = HashMap()
        private val sizeMap = StatMap(mutableMapOf<Int, BigDecimal>())

        var size: Float = initialSize
            set(value) {
                field = when {
                    value > CELL_WIDTH_ROUNDING_THRESHOLD && !zoomingIn -> value.toInt().toFloat()
                    value > CELL_WIDTH_ROUNDING_THRESHOLD -> value.toInt().plus(1).toFloat()
                    value == 0.0f -> Float.MIN_VALUE // at very large levels, fit to screen will calculate a cell size of 0 - we need it to have a minimum value in this case
                    else -> value
                }
                bigSizeCached = size.toBigDecimal() // Update the cached value
                sizeMap.clear()
            }

        init {
            size = initialSize
        }

        var bigSize: BigDecimal = BigDecimal.ZERO
            get() = bigSizeCached
            private set // Make the setter private to disallow external modification

        fun universeSize(level: Int): BigDecimal {
            return universeSizeImpl(level)
        }

        fun halfUniverseSize(level: Int): BigDecimal {
            return universeSizeImpl(level - 1)
        }

        // todo also cache this value the way you cache getHalfSize()
        private fun universeSizeImpl(level: Int): BigDecimal {
            if (level < 0) return BigDecimal.ZERO

            // these values are calculated so often that caching really seems to help
            // cell size as a big decimal times the requested size of universe at a given level
            // using MathContext to make sure we don't lose precision
            return sizeMap.getOrPut(level) { bigSizeCached.multiply(FlexibleInteger.pow2(level).toBigDecimal(), mc) }

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

    companion object {
        // without this precision on the MathContext, small imprecision propagates at
        // large levels on the LifeUniverse - sometimes this will cause the image to jump around or completely
        // off the screen.  don't skimp on precision!
        // Bounds.mathContext is kept up to date with the largest dimension of the universe
        val mc
            get() = Bounds.mathContext
        private val BigTWO = BigDecimal(2)
        private val undoDeque = ArrayDeque<CanvasState>()
        private const val DEFAULT_CELL_WIDTH = 4.0f
    }
}