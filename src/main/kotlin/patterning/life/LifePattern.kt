package patterning.life

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.net.URISyntaxException
import kotlin.math.roundToInt
import patterning.Canvas
import patterning.GraphicsReference
import patterning.Properties
import patterning.RunningState
import patterning.Theme
import patterning.pattern.Movable
import patterning.pattern.NumberedPatternLoader
import patterning.pattern.Pasteable
import patterning.pattern.Pattern
import patterning.pattern.PerformanceTestable
import patterning.pattern.Rewindable
import patterning.pattern.Steppable
import patterning.util.AsyncJobRunner
import patterning.util.FlexibleDecimal
import patterning.util.FlexibleInteger
import patterning.util.ResourceManager
import processing.core.PApplet
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
    Rewindable,
    Steppable {
    
    private lateinit var life: LifeUniverse
    private lateinit var lifeForm: LifeForm
    
    private val universeSize = UniverseSize(canvas)
    private var biggestDimension: FlexibleInteger = FlexibleInteger.ZERO
    
    
    private val performanceTest = PerformanceTest(this, properties)
    private var storedLife = properties.getProperty(LIFE_FORM_PROPERTY)
    
    private val asyncNextGeneration: AsyncJobRunner
    private var targetStep = 0
    private val hudInfo: HUDStringBuilder
    
    // used to move the pattern around the screen
    private var lifeFormPosition = PVector(0f, 0f)
    
    // DrawNodePath is just a helper class extracted into a separate class only for readability
    // it has shared methods so accepting them here
    // lambdas are cool
    private val nodePath: DrawNodePath = DrawNodePath(
        shouldContinue = { node, size, nodeLeft, nodeTop -> shouldContinue(node, size, nodeLeft, nodeTop) },
        universeSize = universeSize,
        canvas = canvas
    )
    
    private var pattern: GraphicsReference
    private var drawBounds = false
    private var isGhosting = false
    
    init {
        
        pattern = canvas.getNamedGraphicsReference(Theme.patternGraphics)
        hudInfo = HUDStringBuilder()
        
        asyncNextGeneration = AsyncJobRunner(method = { asyncNextGeneration() })
        
        // on startup, storedLife may be loaded from the properties file but if it's not
        // just get a random one
        if (storedLife.isEmpty()) {
            getRandomLifeform()
        }
    }
    
    val lastId: Int
        get() = life.lastId.get()
    
    /**
     * Pattern overrides
     */
    override fun draw() {
        
        performanceTest.execute()
        
        drawPattern(life)
        
        pApplet.apply {
            image(pattern.graphics, lifeFormPosition.x, lifeFormPosition.y)
        }
        
        goForwardInTime()
    }
    
    override fun getHUDMessage(): String {
        return getHUDMessage(life)
    }
    
    override fun handleGhost() {
        isGhosting = !isGhosting
    }
    
    override fun handlePlay() {
        RunningState.toggleRunning()
    }
    
    override fun loadPattern() {
        instantiateLifeform()
    }
    
    override fun move(dx: Float, dy: Float) {
        canvas.saveUndoState()
        canvas.adjustCanvasOffsets(FlexibleDecimal.create(dx), FlexibleDecimal.create(dy))
        lifeFormPosition.add(dx, dy)
    }
    
    override fun updateProperties() {
        properties.setProperty(LIFE_FORM_PROPERTY, storedLife)
    }
    
    // as nodes are created their bounds are calculated
    // when the bounds get larger, we need a math context to draw with that
    // allows the pattern to be drawn with the necessary precision
    private fun updateBoundsChanged(bounds: Bounds) {
        val dimension = bounds.width.max(bounds.height)
        if (dimension > biggestDimension) {
            biggestDimension = dimension
            onBiggestDimensionChanged(biggestDimension)
        }
    }
    
    /**
     * Movable overrides
     */
    override fun center() {
        center(life.rootBounds, fitBounds = false, saveState = true)
    }
    
    override fun fitToScreen() {
        center(life.rootBounds, fitBounds = true, saveState = true)
    }
    
    override fun toggleDrawBounds() {
        drawBounds = !drawBounds
    }
    
    override fun zoom(zoomIn: Boolean, x: Float, y: Float) {
        canvas.zoom(zoomIn, x, y)
    }
    
    private fun center(bounds: Bounds, fitBounds: Boolean, saveState: Boolean) {
        if (saveState) canvas.saveUndoState()
        
        // remember, bounds are inclusive - if you want the count of discrete items, then you need to add one back to it
        val patternWidth = bounds.width.toFlexibleDecimal()
        val patternHeight = bounds.height.toFlexibleDecimal()
        
        if (fitBounds) {
            val widthRatio =
                patternWidth.takeIf { it > FlexibleDecimal.ZERO }?.let { canvas.width.divide(it, canvas.mc) }
                    ?: FlexibleDecimal.ONE
            val heightRatio =
                patternHeight.takeIf { it > FlexibleDecimal.ZERO }?.let { canvas.height.divide(it, canvas.mc) }
                    ?: FlexibleDecimal.ONE
            
            canvas.zoomLevel = widthRatio.coerceAtMost(heightRatio).multiply(FlexibleDecimal.create(.9), canvas.mc)
        }
        
        val level = canvas.zoomLevel
        
        val drawingWidth = patternWidth.multiply(level, canvas.mc)
        val drawingHeight = patternHeight.multiply(level, canvas.mc)
        val halfCanvasWidth = canvas.width.divide(FlexibleDecimal.TWO, canvas.mc)
        val halfCanvasHeight = canvas.height.divide(FlexibleDecimal.TWO, canvas.mc)
        val halfDrawingWidth = drawingWidth.divide(FlexibleDecimal.TWO, canvas.mc)
        val halfDrawingHeight = drawingHeight.divide(FlexibleDecimal.TWO, canvas.mc)
        
        // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
        val offsetX = halfCanvasWidth - halfDrawingWidth + (bounds.left.toFlexibleDecimal().multiply(-level, canvas.mc))
        val offsetY =
            halfCanvasHeight - halfDrawingHeight + (bounds.top.toFlexibleDecimal().multiply(-level, canvas.mc))
        
        canvas.updateCanvasOffsets(offsetX, offsetY)
        
    }
    
    /**
     * NumberedPatternLoader overrides
     */
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
    
    private fun getRandomLifeform() {
        try {
            storedLife = ResourceManager.getRandomResourceAsString(ResourceManager.RLE_DIRECTORY)
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
    
    /**
     * Rewindable overrides
     */
    override fun rewind() {
        instantiateLifeform()
    }
    
    /**
     * Steppable overrides
     */
    override fun handleStep(faster: Boolean) {
        var increment = if (faster) 1 else -1
        if (targetStep + increment < 0) increment = 0
        targetStep += increment
    }
    
    /**
     * private fun
     */
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
    
    private fun instantiateLifeform(testing: Boolean = false) {
        
        if (!testing) RunningState.pause()
        asyncNextGeneration.cancelAndWait()
        targetStep = 0
        
        val universe = LifeUniverse()
        universe.step = 0
        
        parseStoredLife()
        
        universe.setupLife(lifeForm.fieldX, lifeForm.fieldY)
        
        setupNewLife(universe)
        
        life = universe
        
        onNewPattern(lifeForm.title)
        
        System.gc()
        
    }
    
    private fun parseStoredLife() {
        try {
            val parser = FileFormat()
            lifeForm = parser.parseRLE(storedLife)
        } catch (e: NotLifeException) {
            println(
                """
                get a life - here's what failed:
                
                ${e.message}
                """.trimIndent()
            )
        }
    }
    
    private fun setupNewLife(life: LifeUniverse) {
        
        val bounds = life.rootBounds
        
        biggestDimension = FlexibleInteger.ZERO
        center(bounds, fitBounds = true, saveState = false)
    }
    
    
    private fun getHUDMessage(life: LifeUniverse): String {
        
        return hudInfo.getFormattedString(
            pApplet.frameCount,
            12
        ) {
            hudInfo.addOrUpdate("fps", pApplet.frameRate.roundToInt())
            hudInfo.addOrUpdate("gps", asyncNextGeneration.getRate())
            hudInfo.addOrUpdate("cell", canvas.zoomLevel.toNumber())
            hudInfo.addOrUpdate("mc", canvas.mc.precision)
            
            hudInfo.addOrUpdate("running", RunningState.runMessage())
            //            hudInfo.addOrUpdate("actuals", actualRecursions)
            hudInfo.addOrUpdate("stack saves", startDelta)
            val patternInfo = life.lifeInfo.info
            patternInfo.forEach { (key, value) ->
                hudInfo.addOrUpdate(key, value)
            }
        }
    }
    
    private fun fillSquare(
        x: Float,
        y: Float,
        size: Float,
    ) {
        
        fun roundMe(value: Float): Float {
            return (if (size > 2)
                (value.roundToInt() - 1).toFloat()
            else
                value)
        }
        
        val width = roundMe(size)
        
        pattern.graphics.rect(roundMe(x), roundMe(y), width, width)
    }
    
    private var actualRecursions = FlexibleInteger.ZERO
    private var startDelta = 0
    
    private fun drawPattern(life: LifeUniverse) {
        
        val graphics = pattern.graphics
        graphics.beginDraw()
        if (isGhosting) {
            graphics.fill(Theme.ghostColor)
        } else {
            graphics.clear()
            graphics.fill(Theme.cellColor)
        }
        graphics.noStroke()
        
        updateBoundsChanged(life.root.bounds)
        
        // getStartingEntry returns a DrawNodePathEntry - which is precalculated
        // to traverse to the first node that has children visible on screen
        // for very large drawing this can save hundreds of stack calls
        // making debugging (at least) easier
        //
        // there may be some performance gain to this although i doubt it's a lot
        // this is more for the thrill of solving a complicated problem and it's
        // no small thing that stack traces become much smaller
        with(nodePath.getLowestEntryFromRoot(life.root)) {
            actualRecursions = FlexibleInteger.ZERO
            
            
            val startingNode = node
            val size = size
            val offsetX = left
            val offsetY = top
            
            startDelta = life.root.level - startingNode.level
            
            drawNodeRecurse(startingNode, size, offsetX, offsetY)
            
        }
        
        drawBounds(life)
        
        graphics.endDraw()
        
        // reset the position in case you've had mouse moves
        lifeFormPosition[0f] = 0f
    }
    
    private fun drawNodeRecurse(
        node: Node,
        size: FlexibleDecimal,
        left: FlexibleDecimal,
        top: FlexibleDecimal
    ) {
        ++actualRecursions
        
        // Check if we should continue
        if (!shouldContinue(node, size, left, top)) {
            return
        }
        
        val leftWithOffset = left + canvas.offsetX
        val topWithOffset = top + canvas.offsetY
        
        
        // If we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size <= FlexibleDecimal.ONE && node.population.isNotZero()) {
            fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), 1f)
        } else if (node is LeafNode && node.population.isOne()) {
            fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), canvas.zoomLevelAsFloat)
        } else if (node is TreeNode) {
            
            val halfSize = universeSize.getHalf(node.level, canvas.zoomLevel)
            
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
        size: FlexibleDecimal,
        nodeLeft: FlexibleDecimal,
        nodeTop: FlexibleDecimal
    ): Boolean {
        if (node.population.isZero()) {
            return false
        }
        
        val left = nodeLeft + canvas.offsetX
        val top = nodeTop + canvas.offsetY
        
        
        // No need to draw anything not visible on screen
        val right = left + size
        val bottom = top + size
        
        
        // left and top are defined by the zoom level (cell size) multiplied
        // by half the universe size which we start out with at the nw corner which is half the size negated
        // each level in we add half of the size at that to the  create the new size for left and top
        // to that, we add the canvas.offsetX to left and canvasOffsetY to top
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
        return !(right < FlexibleDecimal.ZERO || bottom < FlexibleDecimal.ZERO ||
                left >= canvas.width || top >= canvas.height)
    }
    
    private fun drawBounds(life: LifeUniverse) {
        if (!drawBounds) return
        
        val bounds = life.rootBounds
        
        // use the bounds of the "living" section of the universe to determine
        // a visible boundary based on the current canvas offsets and cell size
        val boundingBox = BoundingBox(bounds, canvas.zoomLevel, canvas)
        boundingBox.draw(pattern.graphics)
        
        var currentLevel = life.root.level - 2
        
        while (currentLevel < life.root.level) {
            val halfSize = LifeUniverse.pow2(currentLevel)
            val universeBox = BoundingBox(Bounds(-halfSize, -halfSize, halfSize, halfSize), canvas.zoomLevel, canvas)
            universeBox.draw(pattern.graphics, drawCrosshair = true)
            currentLevel++
        }
    }
    
    private fun asyncNextGeneration() {
        life.nextGeneration()
        // targetStep += 1 // use this for testing - later on you can implement lightspeed around this
    }
    
    companion object {
        private const val LIFE_FORM_PROPERTY = "lifeForm"
    }
}