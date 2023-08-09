package patterning.life

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.math.BigDecimal
import java.math.MathContext
import java.net.URISyntaxException
import kotlin.math.roundToInt
import patterning.Canvas
import patterning.Drawer
import patterning.DrawingInformer
import patterning.Properties
import patterning.RunningState
import patterning.Theme
import patterning.actions.MouseEventManager
import patterning.actions.MovementHandler
import patterning.panel.AlignHorizontal
import patterning.panel.AlignVertical
import patterning.panel.ControlPanel
import patterning.panel.Orientation
import patterning.panel.TextPanel
import patterning.panel.Transition
import patterning.pattern.KeyCallbackFactory
import patterning.pattern.Movable
import patterning.pattern.NumberedPatternLoader
import patterning.pattern.Pasteable
import patterning.pattern.Pattern
import patterning.pattern.PerformanceTestable
import patterning.pattern.Playable
import patterning.pattern.Rewindable
import patterning.pattern.Steppable
import patterning.pattern.Zoomable

import patterning.util.AsyncJobRunner
import patterning.util.FlexibleInteger
import patterning.util.ResourceManager

import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector

class LifePattern(
    pApplet: PApplet,
    canvas: Canvas,
    properties: Properties
) : Pattern(pApplet, canvas, properties),
    Movable,
    NumberedPatternLoader,
    Pasteable,
    PerformanceTestable,
    Playable,
    Rewindable,
    Steppable,
    Zoomable {
    
    private lateinit var life: LifeUniverse
    private lateinit var lifeForm: LifeForm
    private val performanceTest = PerformanceTest(this, properties)
    private var storedLife = properties.getProperty(LIFE_FORM_PROPERTY)
    
    private val asyncNextGeneration: AsyncJobRunner
    private var targetStep = 0
    private val drawingInformer: DrawingInformer
    private val hudInfo: HUDStringBuilder
    private val movementHandler: MovementHandler
    
    // lifeFormPosition is used because we now separate the drawing speed from the framerate
    // we may not draw an image every frame
    // if we haven't drawn an image, we still want to be able to move and drag
    // the image around so this allows us to keep track of the current position
    // for the lifeFormBuffer and move that buffer around regardless of whether we've drawn an image
    // whenever an image is drawn, ths PVector is reset to 0,0 to match the current image state
    // it's a nifty way to handle things - just follow lifeFormPosition through the code
    // to see what i'm talking about
    private var lifeFormPosition = PVector(0f, 0f)
    
    // DrawNodePath is just a helper class extracted into a separate class only for readability
    // it has shared methods so accepting them here
    // lambdas are cool
    private val nodePath: DrawNodePath = DrawNodePath(
        shouldContinue = { node, size, nodeLeft, nodeTop -> shouldContinue(node, size, nodeLeft, nodeTop) },
        updateLargestDimension = { bounds -> updateLargestDimension(bounds) }
    )
    
    private var countdownText: TextPanel? = null
    private val hudText: TextPanel
    
    private lateinit var patternBuffer: PGraphics
    private lateinit var uxBuffer: PGraphics
    private var drawBounds: Boolean
    
    // used for resize detection
    private var prevWidth: Int
    private var prevHeight: Int
    
    private val zoom = Zoom(this)
    
    init {
/*        uxBuffer = canvas.getPGraphics()
        patternBuffer = canvas.getPGraphics()*/
        initBuffers()
        
        drawingInformer = DrawingInformer { uxBuffer }
        // resize trackers
        prevWidth = pApplet.width
        prevHeight = pApplet.height
        
        canvasWidth = pApplet.width.toBigDecimal()
        canvasHeight = pApplet.height.toBigDecimal()
        
        //cell = Cell()
        
        movementHandler = MovementHandler(this)
        drawBounds = false
        hudInfo = HUDStringBuilder()
        
        createTextPanel(null) {
            TextPanel.Builder(drawingInformer, Theme.startupText, AlignHorizontal.RIGHT, AlignVertical.TOP)
                .textSize(Theme.startupTextSize)
                .fadeInDuration(Theme.startupTextFadeInDuration)
                .fadeOutDuration(Theme.startupTextFadeOutDuration)
                .displayDuration(Theme.startupTextDisplayDuration)
        }
        
        val keyCallbackFactory = KeyCallbackFactory(pApplet, this)
        keyCallbackFactory.setupSimpleKeyCallbacks() // the ones that don't need controls
        setupControls(keyCallbackFactory)
        
        asyncNextGeneration = AsyncJobRunner(method = { asyncNextGeneration() }, threadName = "NextGeneration")
        
        // on startup, storedLife may be loaded from the properties file but if it's not
        // just get a random one
        if (storedLife.isEmpty()) {
            getRandomLifeform()
        }
        
        hudText = TextPanel.Builder(
            informer = drawingInformer,
            hAlign = AlignHorizontal.RIGHT,
            vAlign = AlignVertical.BOTTOM
        )
            .textSize(14)
            .wrap()
            .build().also {
                Drawer.add(it)
            }
        
        instantiateLifeform()
    }
    
    val lastId: Int
        get() = life.lastId.get()
    
    /*    private val isWindowResized: Boolean
            get() {
                val widthChanged = prevWidth != pApplet.width
                val heightChanged = prevHeight != pApplet.height
                return widthChanged || heightChanged
            }*/
    
    private fun setupControls(keyCallbackFactory: KeyCallbackFactory) {
        
        val panelLeft: ControlPanel
        val panelTop: ControlPanel
        val panelRight: ControlPanel
        val transitionDuration = Theme.controlPanelTransitionDuration
        panelLeft = ControlPanel.Builder(drawingInformer, AlignHorizontal.LEFT, AlignVertical.CENTER)
            .transition(Transition.TransitionDirection.RIGHT, Transition.TransitionType.SLIDE, transitionDuration)
            .setOrientation(Orientation.VERTICAL)
            .addControl("zoomIn.png", keyCallbackFactory.callbackZoomInCenter)
            .addControl("zoomOut.png", keyCallbackFactory.callbackZoomOutCenter)
            .addControl("fitToScreen.png", keyCallbackFactory.callbackFitUniverseOnScreen)
            .addControl("center.png", keyCallbackFactory.callbackCenterView)
            .addControl("undo.png", keyCallbackFactory.callbackUndoMovement)
            .build()
        panelTop = ControlPanel.Builder(drawingInformer, AlignHorizontal.CENTER, AlignVertical.TOP)
            .transition(Transition.TransitionDirection.DOWN, Transition.TransitionType.SLIDE, transitionDuration)
            .setOrientation(Orientation.HORIZONTAL)
            .addControl("random.png", keyCallbackFactory.callbackRandomPattern)
            .addControl("stepSlower.png", keyCallbackFactory.callbackStepSlower)
            // .addControl("drawSlower.png", keyFactory.callbackDrawSlower)
            .addPlayPauseControl(
                "play.png",
                "pause.png",
                keyCallbackFactory.callbackPause,
            )
            //.addControl("drawFaster.png", keyFactory.callbackDrawFaster)
            .addControl("stepFaster.png", keyCallbackFactory.callbackStepFaster)
            .addControl("rewind.png", keyCallbackFactory.callbackRewind)
            .build()
        panelRight = ControlPanel.Builder(drawingInformer, AlignHorizontal.RIGHT, AlignVertical.CENTER)
            .transition(Transition.TransitionDirection.LEFT, Transition.TransitionType.SLIDE, transitionDuration)
            .setOrientation(Orientation.VERTICAL)
            .addToggleHighlightControl("boundary.png", keyCallbackFactory.callbackDrawBounds)
            .addToggleHighlightControl("darkmode.png", keyCallbackFactory.callbackThemeToggle)
            .addToggleHighlightControl("singleStep.png", keyCallbackFactory.callbackSingleStep)
            .build()
        val panels = listOf(panelLeft, panelTop, panelRight)
        
        MouseEventManager.addAll(panels)
        Drawer.addAll(panels)
    }
    
    // Pattern overrides
    override fun draw() {
        
        if (canvas.resized) {
            updateWindowResized()
        }
        
        performanceTest.execute()
        
        zoom.update()
        
        prevWidth = pApplet.width
        prevHeight = pApplet.height
        
        drawUX(life)
        drawPattern(life)
        
        pApplet.apply {
            image(patternBuffer, lifeFormPosition.x, lifeFormPosition.y)
            image(uxBuffer, 0f, 0f)
        }
        
        goForwardInTime()
    }
    
    override fun move(dx: Float, dy: Float) {
        saveUndoState()
        adjustCanvasOffsets(dx.toBigDecimal(), dy.toBigDecimal())
        lifeFormPosition.add(dx, dy)
    }
    
    override fun updateProperties() {
        properties.setProperty(LIFE_FORM_PROPERTY, storedLife)
    }
    
    // Movable overrides
    override fun center() {
        center(life.rootBounds, fitBounds = false, saveState = true)
    }
    
    override fun fitToScreen() {
        center(life.rootBounds, fitBounds = true, saveState = true)
    }
    
    override fun saveUndoState() {
        undoDeque.add(CanvasState(Cell(cell.size), canvasOffsetX, canvasOffsetY))
    }
    
    override fun toggleDrawBounds() {
        drawBounds = !drawBounds
    }
    
    override fun undoMovement() {
        if (undoDeque.isNotEmpty()) {
            zoom.stopZooming()
            val previous = undoDeque.removeLast()
            cell = previous.cell
            updateCanvasOffsets(previous.canvasOffsetX, previous.canvasOffsetY)
        }
    }
    
    // NumberedPatternLoader overrides
    override fun setRandom() {
        getRandomLifeform()
        instantiateLifeform()
    }
    
    override fun setNumberedPattern(number: Int, testing: Boolean) {
        try {
            storedLife =
                ResourceManager.getResourceAtFileIndexAsString(ResourceManager.RLE_DIRECTORY, number)
            instantiateLifeform(testing)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }
    
    /**
     * Pasteable overrides
     */
    override fun paste() {
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
    
    // Rewindable overrides
    
    override fun rewind() {
        instantiateLifeform()
    }
    
    // Steppable overrides
    override fun handleStep(faster: Boolean) {
        var increment = if (faster) 1 else -1
        if (targetStep + increment < 0) increment = 0
        targetStep += increment
    }
    
    // Zoomable overrides
    override fun zoom(zoomIn: Boolean, x: Float, y: Float) {
        zoom.zoom(zoomIn, x, y)
    }
    
    // Private methods
    
    private fun goForwardInTime() {
        
        if (asyncNextGeneration.isActive) {
            return
        }
        
        with(life) {
            step = when {
                step < targetStep -> step + 1
                step > targetStep -> step - 1
                else -> step
            }
        }
        
        if (RunningState.shouldAdvance())
            asyncNextGeneration.startJob()
    }
    
    private fun getRandomLifeform() {
        try {
            storedLife = ResourceManager.getRandomResourceAsString(ResourceManager.RLE_DIRECTORY)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }
    
    private fun instantiateLifeform(testing: Boolean = false) {
        if (!testing) RunningState.pause()
        zoom.stopZooming()
        asyncNextGeneration.cancelAndWait()
        
        val universe = LifeUniverse()
        targetStep = 0
        universe.step = 0
        
        try {
            
            // instance variables - do they need to be?
            val parser = FileFormat()
            lifeForm = parser.parseRLE(storedLife)
            universe.setupLife(lifeForm.fieldX, lifeForm.fieldY)
        } catch (e: NotLifeException) {
            println(
                """
    get a life - here's what failed:
    
    ${e.message}
    """.trimIndent()
            )
        }
        // new instances only created in instantiateLife to keep things simple
        // lifeForm not made local as it is intended to be used with display functions in the future
        setupNewLife(universe, testing)
        
        life = universe
        
        createTextPanel(null) {
            TextPanel.Builder(drawingInformer, lifeForm.title, AlignHorizontal.LEFT, AlignVertical.TOP)
                .textSize(Theme.startupTextSize)
                .fadeInDuration(Theme.startupTextFadeInDuration)
                .fadeOutDuration(Theme.startupTextFadeOutDuration)
                .displayDuration(Theme.startupTextDisplayDuration)
        }
        System.gc()
        
    }
    
    private fun calcCenterOnResize(dimension: BigDecimal, offset: BigDecimal): BigDecimal {
        return dimension.divide(BigDecimal.TWO, mc) - offset
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
    
    private fun setupNewLife(life: LifeUniverse, testing: Boolean = false) {
        
        undoDeque.clear()
        
        val bounds = life.rootBounds
        updateLargestDimension(bounds)
        center(bounds, fitBounds = true, saveState = false)
        
        if (!testing) countdownText = createTextPanel(countdownText) {
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
                .wrap()
                .textSize(24)
        }
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
        val halfCanvasWidth = canvasWidth.divide(BigDecimal.TWO, mc)
        val halfCanvasHeight = canvasHeight.divide(BigDecimal.TWO, mc)
        val halfDrawingWidth = drawingWidth.divide(BigDecimal.TWO, mc)
        val halfDrawingHeight = drawingHeight.divide(BigDecimal.TWO, mc)
        
        // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
        val offsetX = halfCanvasWidth - halfDrawingWidth + (bounds.left.bigDecimal * -bigCell)
        val offsetY = halfCanvasHeight - halfDrawingHeight + (bounds.top.bigDecimal * -bigCell)
        
        updateCanvasOffsets(offsetX, offsetY)
        
    }
    
    
    private fun getHUDMessage(life: LifeUniverse): String {
        
        return hudInfo.getFormattedString(
            pApplet.frameCount,
            12
        ) {
            hudInfo.addOrUpdate("fps", pApplet.frameRate.roundToInt())
            hudInfo.addOrUpdate("gps", asyncNextGeneration.getRate())
            hudInfo.addOrUpdate("cell", cell.size)
            hudInfo.addOrUpdate("running", RunningState.runMessage())
            //            hudInfo.addOrUpdate("actuals", actualRecursions)
            hudInfo.addOrUpdate("stack saves", startDelta)
            val patternInfo = life.lifeInfo.info
            patternInfo.forEach { (key, value) ->
                hudInfo.addOrUpdate(key, value)
            }
        }
    }
    
    private data class CanvasState(
        val cell: Cell,
        val canvasOffsetX: BigDecimal,
        val canvasOffsetY: BigDecimal
    )
    
    
    override fun handlePlay() {
        Drawer.takeIf { Drawer.isManaging(countdownText!!) }?.let {
            countdownText?.interruptCountdown()
        } ?: RunningState.toggleRunning()
    }
    
    private fun initBuffers() {
        uxBuffer = canvas.getPGraphics()
        patternBuffer = canvas.getPGraphics()
    }
    
    private fun updateWindowResized() {
        
        // create new buffers
        initBuffers()
        
        // Calculate the center of the visible portion before resizing
        val centerXBefore = calcCenterOnResize(canvasWidth, canvasOffsetX)
        val centerYBefore = calcCenterOnResize(canvasHeight, canvasOffsetY)
        
        // Update the canvas size
        canvasWidth = pApplet.width.toBigDecimal()
        canvasHeight = pApplet.height.toBigDecimal()
        
        // Calculate the center of the visible portion after resizing
        val centerXAfter = calcCenterOnResize(canvasWidth, canvasOffsetX)
        val centerYAfter = calcCenterOnResize(canvasHeight, canvasOffsetY)
        
        adjustCanvasOffsets(centerXAfter - centerXBefore, centerYAfter - centerYBefore)
    }
    
    
    private fun fillSquare(
        x: Float,
        y: Float,
        size: Float,
        color: Int = Theme.cellColor
    ) {
        val width = size - cell.cellBorderWidth
        
        // we default the patternBuffer to the cell color so no need to change it
        if (color != Theme.cellColor) patternBuffer.fill(color)
        
        patternBuffer.rect(x,y,width,width)
        
/*        patternBuffer.apply {
            fill(color)
            noStroke()
            rect(x, y, width, width)
        }*/
    }
    
    // Initialize viewPath, also at class level
    private var actualRecursions = FlexibleInteger.ZERO
    private var startDelta = 0
    
    private fun drawPattern(life: LifeUniverse) {
        patternBuffer.beginDraw()
        patternBuffer.clear()
        patternBuffer.noStroke()
        patternBuffer.fill(Theme.cellColor)
        
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
            
            drawNodeRecurse(startingNode, size, offsetX, offsetY)
            
        }
        
        // keep this around - it works - so if your startingNode code has issues, you can resuscitate
        //drawNodeRecurse(life.root, size, -halfSize, -halfSize)
        
        drawBounds(life)
        
        patternBuffer.endDraw()
        // reset the position in case you've had mouse moves
        lifeFormPosition[0f] = 0f
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
            fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), 1f)
        } else if (node is LeafNode && node.population.isOne()) {
            fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), cell.size)
        } else if (node is TreeNode) {
            
            val halfSize = cell.halfUniverseSize(node.level)
            
            val leftHalfSize = left + halfSize
            val topHalfSize = top + halfSize
            
            drawNodeRecurse(node.nw, halfSize, left, top)
            drawNodeRecurse(node.ne, halfSize, leftHalfSize, top)
            drawNodeRecurse(node.sw, halfSize, left, topHalfSize)
            drawNodeRecurse(node.se, halfSize, leftHalfSize, topHalfSize)
        }
    }
    
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
        
        val left = nodeLeft + canvasOffsetX
        val top = nodeTop + canvasOffsetY
        
        
        // No need to draw anything not visible on screen
        val right = left + size
        val bottom = top + size
        
        
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
    
    fun adjustCanvasOffsets(dx: BigDecimal, dy: BigDecimal) {
        updateCanvasOffsets(canvasOffsetX + dx, canvasOffsetY + dy)
    }
    
    private fun updateCanvasOffsets(offsetX: BigDecimal, offsetY: BigDecimal) {
        canvasOffsetX = offsetX
        canvasOffsetY = offsetY
        nodePath.offsetsMoved = true
    }
    
    private fun drawBounds(life: LifeUniverse) {
        if (!drawBounds) return
        
        val bounds = life.rootBounds
        
        // use the bounds of the "living" section of the universe to determine
        // a visible boundary based on the current canvas offsets and cell size
        val boundingBox = BoundingBox(bounds, cell.bigSize)
        boundingBox.draw(patternBuffer)
        
        var currentLevel = life.root.level - 2
        
        while (currentLevel < life.root.level) {
            val halfSize = FlexibleInteger.pow2(currentLevel)
            val universeBox = BoundingBox(Bounds(-halfSize, -halfSize, halfSize, halfSize), cell.bigSize)
            universeBox.draw(patternBuffer, drawCrosshair = true)
            currentLevel++
        }
    }
    
    private fun drawUX(life: LifeUniverse) {
        uxBuffer.apply {
            beginDraw()
            clear()
        }
        
        movementHandler.handleRequestedMovement()
        
        val hudMessage = getHUDMessage(life)
        hudText.message = hudMessage
        Drawer.drawAll()
        
        uxBuffer.endDraw()
    }
    
    
    private suspend fun asyncNextGeneration() {
        life.nextGeneration()
        // targetStep += 1 // use this for testing - later on you can implement lightspeed around this
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
        
        internal var canvasOffsetX = BigDecimal.ZERO
        internal var canvasOffsetY = BigDecimal.ZERO
        internal var canvasWidth: BigDecimal = BigDecimal.ZERO
        internal var canvasHeight: BigDecimal = BigDecimal.ZERO
        
        var cell = Cell()
        
        private val undoDeque = ArrayDeque<CanvasState>()
        
        private const val LIFE_FORM_PROPERTY = "lifeForm"
        
        fun resetMathContext() {
            largestDimension = FlexibleInteger.ZERO
            previousPrecision = 0
        }
        
    }
}