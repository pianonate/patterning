package patterning.life

import kotlin.math.abs
import kotlin.math.roundToInt
import patterning.Canvas
import patterning.GraphicsReference
import patterning.Properties
import patterning.Theme
import patterning.ThreeD
import patterning.pattern.BoundaryMode
import patterning.pattern.Colorful
import patterning.pattern.Behavior
import patterning.pattern.DisplayState
import patterning.pattern.Movable
import patterning.pattern.NumberedPatternLoader
import patterning.pattern.Pasteable
import patterning.pattern.Pattern
import patterning.pattern.PerformanceTestable
import patterning.pattern.Rewindable
import patterning.pattern.Steppable
import patterning.pattern.ThreeDimensional
import patterning.state.RunningModeController
import patterning.util.AsyncJobRunner
import patterning.util.ResourceManager
import patterning.util.applyAlpha
import patterning.util.isNotZero
import patterning.util.isOne
import patterning.util.isZero
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PVector
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.net.URISyntaxException


class LifePattern(
    pApplet: PApplet,
    canvas: Canvas,
    properties: Properties,
    displayState: DisplayState,
   // fadeShader: PShader
) : Pattern(pApplet, canvas, properties, displayState/*, fadeShader*/),
    Colorful,
    Movable,
    NumberedPatternLoader,
    Pasteable,
    PerformanceTestable,
    Rewindable,
    Steppable,
    ThreeDimensional {

    private lateinit var life: LifeUniverse
    private lateinit var lifeForm: LifeForm

    private val universeSize = UniverseSize()
    private var biggestDimension: Long = 0L


    private val performanceTest = PerformanceTest(this, properties)
    private var storedLife = properties.getProperty(LIFE_FORM_PROPERTY)

    private val asyncNextGenerationJob: AsyncJobRunner
    private var targetStep = 0
    private val hudInfo: HUDStringBuilder

    // DrawNodePath is just a helper class extracted into a separate class only for readability
    // it has shared methods so accepting them here
    // lambdas are cool
    private val nodePath: DrawNodePath = DrawNodePath(
        shouldContinue = { node, size, nodeLeft, nodeTop ->
            shouldContinue(node, size, nodeLeft, nodeTop)
        },
        universeSize = universeSize,
        canvas = canvas
    )

    private val pattern: GraphicsReference

    init {

        pattern = canvas.getNamedGraphicsReference(Theme.PATTERN_GRAPHICS, useOpenGL = true)
        hudInfo = HUDStringBuilder()

        asyncNextGenerationJob = AsyncJobRunner(method = { asyncNextGeneration() })

        // on startup, storedLife may be loaded from the properties file but if it's not
        // just get a random one
        if (storedLife.isEmpty()) {
            getRandomLifeform()
        }

    }

    val lastId: Int
        get() = life.lastId.get()

    override fun getHUDMessage(): String {
        return hudInfo.getFormattedString(
            pApplet.frameCount,
            80
        ) {
            hudInfo.addOrUpdate("fps", pApplet.frameRate.roundToInt())
            hudInfo.addOrUpdate("gps", asyncNextGenerationJob.getRate())
            hudInfo.addOrUpdate("zoom", canvas.zoomLevel)
            hudInfo.addOrUpdate("running", RunningModeController.runningMode.toString())
            hudInfo.addOrUpdate("stack saves", startDelta)
            val patternInfo = life.lifeInfo.info
            patternInfo.forEach { (key, value) ->
                hudInfo.addOrUpdate(key, value)
            }
        }
    }

    override fun loadPattern() {
        instantiateLifeform()
    }


    override fun updateProperties() {
        properties.setProperty(LIFE_FORM_PROPERTY, storedLife)
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

    override fun zoom(zoomIn: Boolean, x: Float, y: Float) {
        canvas.zoom(zoomIn, x, y)
    }

    private fun center(bounds: Bounds, fitBounds: Boolean, saveState: Boolean) {
        if (saveState) canvas.saveUndoState()

        // remember, bounds are inclusive - if you want the count of discrete items, then you need to add one back to it
        val patternWidth = bounds.width
        val patternHeight = bounds.height

        if (fitBounds) {
            val widthRatio =
                patternWidth.takeIf { it > 0f }?.let { canvas.width / it }
                    ?: 1f
            val heightRatio =
                patternHeight.takeIf { it > 0f }?.let { canvas.height / it }
                    ?: 1f

            canvas.zoomLevel = widthRatio.coerceAtMost(heightRatio)

            reset3DAndStopRotations()

        }

        val level = canvas.zoomLevel

        val drawingWidth = patternWidth * level
        val drawingHeight = patternHeight * level
        val halfCanvasWidth = canvas.width / 2f
        val halfCanvasHeight = canvas.height / 2f
        val halfDrawingWidth = drawingWidth / 2f
        val halfDrawingHeight = drawingHeight / 2f

        // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
        val offsetX = halfCanvasWidth - halfDrawingWidth + (bounds.left * -level)
        val offsetY = halfCanvasHeight - halfDrawingHeight + (bounds.top * -level)

        canvas.updateCanvasOffsets(offsetX, offsetY)

    }

    /**
     * NumberedPatternLoader overrides
     */
    override fun setRandom() {
        getRandomLifeform()
        instantiateLifeform()
    }

    override fun setNumberedPattern(number: Int) {
        try {
            storedLife =
                ResourceManager.getResourceAtFileIndexAsString(ResourceManager.RLE_DIRECTORY, number)
            instantiateLifeform()
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
     * ThreeDimensional overrides
     */
    override fun centerAndResetRotations() {
        center()
        reset3DAndStopRotations()
    }

    private fun reset3DAndStopRotations() {

        threeD.reset()
        displayState.disable(Behavior.ThreeDYaw)
        displayState.disable(Behavior.ThreeDPitch)
        displayState.disable(Behavior.ThreeDRoll)

        // used for individual boxes
        displayState.disable(Behavior.ThreeDBoxes)
        displayState.disable(Behavior.AlwaysRotate)
    }

    /**
     * private fun
     */
    private fun goForwardInTime(shouldAdvance: Boolean) {

        if (asyncNextGenerationJob.isActive) {
            return
        }

        with(life) {
            step = when {
                step < targetStep -> step + 1
                step > targetStep -> step - 1
                else -> step
            }
        }

        if (shouldAdvance)
            asyncNextGenerationJob.start()
    }

    private fun instantiateLifeform() {

        if (!RunningModeController.isTesting) RunningModeController.load()

        asyncNextGenerationJob.cancelAndWait()
        targetStep = 0

        reset3DAndStopRotations()

        val universe = LifeUniverse()
        universe.step = 0

        parseStoredLife()

        universe.newLife(lifeForm.fieldX, lifeForm.fieldY)

        center(universe.rootBounds, fitBounds = true, saveState = false)

        biggestDimension = 0L

        life = universe

        onNewPattern(lifeForm.title)

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

    /**
     * i don't love it that rectCorners has the latest value from "shouldContinue"
     * but it's done for efficiency given all the matrix multiplication going on
     * i.e., why ask for the corners more than once?
     */
    private fun fillSquare(
        size: Float,
    ) {
        with(pattern.graphics) {

            val getFillColorsLambda =
                { x: Float, y: Float, applyCubeAlpha: Boolean -> getFillColor(x, y, applyCubeAlpha) }
            val corners = threeD.rectCorners

            when {
                size > 4F && (displayState expects Behavior.ThreeDBoxes) -> {
                    boxPlus(
                        frontCorners = corners,
                        threeD = threeD,
                        depth = size,
                        getFillColor = getFillColorsLambda
                    )
                }

                else -> {
                    quadPlus(corners = corners, getFillColor = getFillColorsLambda)
                }
            }
        }
    }

    private fun getFillColor(x: Float, y: Float, applyCubeAlpha: Boolean = true): Int {

        val cubeAlpha = if (
            displayState expects Behavior.ThreeDBoxes &&
            canvas.zoomLevel >= 4F && applyCubeAlpha
        )
            Theme.cubeAlpha else 255

        return with(pattern.graphics) {
            val color = if (displayState expects Behavior.Colorful) {
                colorMode(PConstants.HSB, 360f, 100f, 100f, 255f)
                val mappedColor = PApplet.map(x + y, 0f, canvas.width + canvas.height, 0f, 360f)
                color(mappedColor, 100f, 100f, cubeAlpha.toFloat())
            } else {
                Theme.cellColor.applyAlpha(cubeAlpha)
            }

            ghostState.applyAlpha(color)
        }
    }


    private var actualRecursions = 0L
    private var startDelta = 0

    override fun drawPattern(shouldAdvancePattern: Boolean) {

        performanceTest.execute()

        with(pattern.graphics) {

            beginDraw()
            ghostState.prepareGraphics(this)
            stroke(ghostState.applyAlpha(Theme.backgroundColor))

            val patternIsDrawable = shouldAdvancePattern || (displayState expects Behavior.AlwaysRotate)
            if (patternIsDrawable)
                threeD.rotateActiveRotations()

            val shouldDraw = when {
                ghostState !is Ghosting && RunningModeController.isPaused -> true
                ghostState is Ghosting && patternIsDrawable -> true
                ghostState !is Ghosting -> true
                else -> false
            }

            if (shouldDraw) {
                drawVisibleNodes(life)
                drawBounds(life)
            }

            endDraw()
        }

        goForwardInTime(shouldAdvancePattern)

    }

    // getLowestEntryFromRoot returns a DrawNodePathEntry - which contains the node
    // found by traversing to the first node that has children visible on screen
    // for very large drawing this can save hundreds of stack calls
    // making debugging (at least) easier
    //
    // there may be some performance gain to this although i doubt it's a lot
    // this is more for the thrill of solving a complicated problem and it's
    // no small thing that stack traces become much smaller
    private fun drawVisibleNodes(life: LifeUniverse) {

        if (displayState.boundaryMode == BoundaryMode.BoundaryOnly) return

        with(nodePath.getLowestEntryFromRoot(life.root)) {
            actualRecursions = 0L

            val startingNode = node
            val size = size
            val offsetX = left
            val offsetY = top

            startDelta = life.root.level - startingNode.level
            pattern.graphics.beginShape(PConstants.QUADS)
            drawNodeRecurse(startingNode, size, offsetX, offsetY)
            pattern.graphics.endShape()
        }
    }

    private fun drawNodeRecurse(
        node: Node,
        size: Float,
        left: Float,
        top: Float
    ) {
        ++actualRecursions

        if (!shouldContinue(node, size, left, top)) {
            return
        }

        // If we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size <= 1f && node.population.isNotZero()) {
            fillSquare(1f)
        } else if (node is LeafNode && node.population.isOne()) {
            fillSquare(canvas.zoomLevel)
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

    /**
     * used by PatternDrawer a well as DrawNodePath
     * maintains no state so it can live here as a utility function
     *
     * left and top are defined by the zoom level (cell size) multiplied
     * by half the universe size which we start out with at the nw corner which is half the size negated
     *
     * each level in we add half of that level size to create the new size for left and top
     * and then to that we add the canvas.offsetX to left and canvas.offsetY to top
     *
     * the size at this level is then added to the left to get the right side of the universe
     * and the size is added to the top to get the bottom of the universe
     * then we see if this universe is inside the canvas by looking to see if
     * in any direction it is outside.
     *
     * if the right side is less than zero it's to the left of the canvas
     * if the bottom is less than zero it's above the canvas
     * if the left is larger than the width then we're to the right of the canvas
     * if the top is larger than the height we're below the canvas
     *
     * updated to use ThreeD to determine whether a rotated image will fit
     */
    private fun shouldContinue(
        node: Node,
        size: Float,
        nodeLeft: Float,
        nodeTop: Float
    ): Boolean {
        if (node.population.isZero()) {
            return false
        }

        val left = nodeLeft + canvas.offsetX
        val top = nodeTop + canvas.offsetY

        return threeD.isRectInView(left, top, size, size)
    }

    private fun drawBounds(life: LifeUniverse) {
        if (displayState.boundaryMode == BoundaryMode.PatternOnly) return

        val bounds = life.rootBounds

        val getFillColor: (Float, Float) -> Int = { x, y -> getFillColor(x, y) }

        var currentLevel = life.root.level - 2

        while (currentLevel < life.root.level) {
            val drawOnlyBiggest = currentLevel == (life.root.level - 1)
            val halfSize = LifeUniverse.pow2(currentLevel)
            val levelBounds = Bounds(-halfSize, -halfSize, halfSize, halfSize)
            val universeBox = BoundingBox(levelBounds, canvas, threeD, getFillColor)
            universeBox.draw(graphics = pattern.graphics, drawCrossHair = drawOnlyBiggest)
            currentLevel++
        }

        // use the bounds of the "living" section of the universe to determine
        // a visible boundary based on the current canvas offsets and cell size
        val patternBox = BoundingBox(bounds, canvas, threeD, getFillColor)
        patternBox.draw(pattern.graphics)

    }

    private fun asyncNextGeneration() {
        life.nextGeneration()
        // targetStep += 1 // use this for testing - later on you can implement lightspeed around this
    }

    /**
     * PGraphics extension functions as a helper for drawing the quads...
     *
     * we pass in a list of PVectors 4 at time so chunked can work to draw boxes correctly
     *
     * no stroke handling - as we are rotating, if we're large enough to have a stroke on the box outline
     * we _don't_ want to show it because it will eliminate too many frames as it gets edge on to
     * the camera - this is because the strokeColor is set to the background color
     * so that's all we see - is background color
     *
     * so edge on, we turn off the stroke temporarily
     */
    private fun PGraphics.quadPlus(corners: List<PVector>, getFillColor: (Float, Float, Boolean) -> Int) {
        this.beginShape(PConstants.QUADS)

        corners.chunked(4).forEachIndexed { index, quadCorners ->
            if (quadCorners.size == 4) {

                if ((abs(corners[2].y - corners[0].y) <= 2) || (abs(corners[1].x - corners[0].x) <= 2)) {
                    this.noStroke()
                }

                // if we're drawing a cube there are 6 faces chunked into 4 corners
                // don't let apply the cube alpha to the first face drawn - which is the background face
                // so one of the faces will always be full strength and the rest will be semi-transparent
                // because it looks cool
                val applyCubeAlpha = (index > 0)
                val fillColor = getFillColor(quadCorners[0].x, quadCorners[0].y, applyCubeAlpha)
                this.fill(fillColor)


                quadCorners.forEach { vertex(it.x, it.y) }
            }
        }

        this.endShape()
    }

    private fun PGraphics.boxPlus(
        frontCorners: List<PVector>,
        threeD: ThreeD,
        depth: Float,
        getFillColor: (Float, Float, Boolean) -> Int
    ) {
        val allCorners = mutableListOf<PVector>()

        val backCorners = threeD.getBackCornersAtDepth(depth)

        allCorners.addAll(backCorners)

        for (i in 0 until 4) {
            val j = (i + 1) % 4
            allCorners.add(frontCorners[i])
            allCorners.add(frontCorners[j])
            allCorners.add(backCorners[j])
            allCorners.add(backCorners[i])
        }

        allCorners.addAll(frontCorners)

        quadPlus(allCorners, getFillColor)

    }

    companion object {
        private const val LIFE_FORM_PROPERTY = "lifeForm"
    }
}