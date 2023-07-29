package patterning.ux

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import patterning.*
import patterning.actions.KeyFactory
import patterning.actions.MouseEventManager
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
import java.math.MathContext
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

    private class DrawNodePath {
        var offsetsMoved: Boolean = true
        private var level: Int = 0
        private val path: MutableList<DrawNodePathEntry> = mutableListOf()

        init {
            path.add(DrawNodePathEntry(Node.deadNode, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Direction.NW))
        }

        fun getLowestEntryFromRoot(root: TreeNode): DrawNodePathEntry {

            val newLevel = root.level
            updateLargestDimension(root.bounds)

            val halfSizeOffset = -cell.halfUniverseSize(newLevel)
            val universeSize = cell.universeSize(newLevel)

            // every generation the root changes so we have to use the latest root
            // to walk through the nodePath to find the lowest node that has children visible on screen
            if (root.level != path[0].node.level
                || offsetsMoved
                || childrenSizesChanged(root)
            ) {

                // if the offsets moved, or the root.level doesn't match
                // or the sizes of the children in the path don't match the sizes of the children
                // in the new root, then we need to update the path
                clear()
                updateNodePath(root, halfSizeOffset, halfSizeOffset)
                offsetsMoved = false
                level = newLevel
            }

            path[0] = DrawNodePathEntry(
                root,
                universeSize,
                halfSizeOffset,
                halfSizeOffset,
                Direction.ROOT
            )

            return lowestEntry(root)
        }

        private fun childrenSizesChanged(new: TreeNode): Boolean {
            // Traverse down the quadtree from the newRoot
            var newTreeNode: TreeNode = new

            for (entry in path) {
                val existingPathNode = entry.node as TreeNode

                // Traverse down to the child node according to the direction in the path entry
                newTreeNode = when (entry.direction) {
                    Direction.ROOT -> newTreeNode
                    Direction.NW -> newTreeNode.nw as TreeNode
                    Direction.NE -> newTreeNode.ne as TreeNode
                    Direction.SW -> newTreeNode.sw as TreeNode
                    Direction.SE -> newTreeNode.se as TreeNode
                }

                // Compare the count of children with population in the currentNode and in the entry from the path
                if (newTreeNode.populatedChildrenCount != existingPathNode.populatedChildrenCount) {
                    return true
                }
            }

            // If no changes were found, return false
            return false

        }

        private fun clear() {
            if (path.size > 1) {
                path.subList(1, path.size).clear()
            }
        }

        private fun lowestEntry(root: TreeNode): DrawNodePathEntry {
            var currentNode: Node = root
            var lastEntry = path[0]

            if (path.size == 1) {
                return lastEntry
            }

            for (entry in path) {
                lastEntry = entry
                currentNode = when (entry.direction) {
                    Direction.ROOT -> currentNode
                    Direction.NW -> (currentNode as TreeNode).nw
                    Direction.NE -> (currentNode as TreeNode).ne
                    Direction.SW -> (currentNode as TreeNode).sw
                    Direction.SE -> (currentNode as TreeNode).se
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

        private fun updateNodePath(
            node: Node,
            left: BigDecimal,
            top: BigDecimal
        ) {
            if (node.population.isZero()) {
                return
            }

            if (node is TreeNode) {
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
                    path.add(intersectingChild)
                    updateNodePath(
                        intersectingChild.node,
                        /* intersectingChild.size,*/
                        intersectingChild.left,
                        intersectingChild.top
                    )
                }
            }
        }

    }

    private enum class Direction {
        NW, NE, SW, SE, ROOT
    }

    private val drawingInformer: DrawingInfoSupplier
    private val patterning: Processing = processing as Processing
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

    private val nodePath = DrawNodePath()

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
            .addControl("random.png", keyFactory.callbackRandomLife)
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

    fun setupNewLife(life: LifeUniverse) {

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


    fun center(bounds: Bounds, fitBounds: Boolean, saveState: Boolean) {
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
            hudInfo.addOrUpdate("gps", patterning.gps)
            hudInfo.addOrUpdate("cell", cell.size)
            hudInfo.addOrUpdate("running", RunningState.runMessage())
/*            hudInfo.addOrUpdate("actuals", actualRecursions)
            hudInfo.addOrUpdate("stack saves", startDelta)*/
            val patternInfo = life.patternInfo.info
            patternInfo.forEach { (key, value) ->
                hudInfo.addOrUpdate(key, value)
            }
            hudInfo.addOrUpdate("posHits", positionMap.hitRate)
        }
    }

    fun saveUndoState() {
        undoDeque.add(CanvasState(Cell(cell.size), canvasOffsetX, canvasOffsetY))
    }

    fun handlePlay() {
        Drawer.takeIf { it.isManaging(countdownText!!) }?.let {
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


    fun draw(life: LifeUniverse) {

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
    }

    // the cell width times 2 ^ level will give you the size of the whole universe
// you'll need it it to draw the viewport on screen
    private class Cell(initialSize: Float = DEFAULT_CELL_WIDTH) {
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
            return sizeMap.getOrPut(level) { bigSizeCached.multiply(FlexibleInteger.pow2(level).bigDecimal, mc) }

        }

        private var zoomingIn: Boolean = false

        fun zoom(zoomIn: Boolean) {
            zoomingIn = zoomIn
            val factor = if (zoomIn) 1.25f else 0.8f
            size *= factor
        }

        override fun toString() = "Cell{size=$size}"

        companion object {
            private const val DEFAULT_CELL_WIDTH = 4.0f
            private const val CELL_WIDTH_ROUNDING_THRESHOLD = 1.6f
            const val WIDTH_RATIO = .05f
        }
    }

    companion object {
        // without this precision on the MathContext, small imprecision propagates at
        // large levels on the LifeUniverse - sometimes this will cause the image to jump around or completely
        // off the screen.  don't skimp on precision!
        // Bounds.mathContext is kept up to date with the largest dimension of the universe
        private var mc = MathContext(0)

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
        private fun updateLargestDimension(bounds: Bounds) {
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
        private lateinit var cell: Cell
        private val positionMap = StatMap(mutableMapOf<PositionKey, BigDecimal>())

        private fun getMappedPosition(pos: BigDecimal, offset: BigDecimal): BigDecimal =
            positionMap.getOrPut(PositionKey(pos, offset)) { pos + offset }


        // used by PatternDrawer a well as DrawNodePath
        // maintains no state so it can live here as a utility function
        private fun shouldContinue(
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