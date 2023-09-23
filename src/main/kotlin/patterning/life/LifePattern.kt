package patterning.life

import kotlin.math.PI
import kotlin.math.roundToInt
import patterning.Canvas
import patterning.GraphicsReference
import patterning.Properties
import patterning.Theme
import patterning.ThreeD
import patterning.pattern.Colorful
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
import patterning.util.isNotZero
import patterning.util.isOne
import patterning.util.isZero
import patterning.util.quadPlus
import patterning.util.roundToIntIfGreaterThanReference
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PVector
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.net.URISyntaxException


class LifePattern(
    pApplet: PApplet,
    canvas: Canvas,
    properties: Properties
) : Pattern(pApplet, canvas, properties),
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

    // used to move the pattern around the screen
    private var lifeFormPosition = PVector(0f, 0f)

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

    private var pattern: GraphicsReference
    private var drawBounds = false
    private var isThreeD = false

    private var threeD = ThreeD(canvas)

    init {

        // pattern = GraphicsReference(pApplet.graphics, Theme.patternGraphics, isResizable = true, useOpenGL = true)
        pattern = canvas.getNamedGraphicsReference(Theme.patternGraphics, useOpenGL = true)
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

    /**
     *  Colorful overrides
     */

    private var rainbowMode = false
    override fun toggleRainbow() {
        rainbowMode = !rainbowMode
    }

    /**
     * Pattern overrides
     */
    override fun draw() {

        performanceTest.execute()

        val shouldAdvance = RunningModeController.shouldAdvance()

        drawPattern(life, shouldAdvance)

        pApplet.image(pattern.graphics, lifeFormPosition.x, lifeFormPosition.y)

        goForwardInTime(shouldAdvance)

    }

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

    override fun handlePlayPause() {
        RunningModeController.togglePlayPause()
    }

    override fun loadPattern() {
        instantiateLifeform()
    }

    override fun move(dx: Float, dy: Float) {
        canvas.saveUndoState()
        canvas.moveCanvasOffsets(dx, dy)
        lifeFormPosition.add(dx, dy)
    }

    /**
     * okay - this is hacked in for now so you can at least et something out of it but ou really need to pop the
     * system dialog on non-mobile devices.  mobile - probably sharing
     */
    override fun saveImage() {

        val newGraphics = pApplet.createGraphics(pApplet.width, pApplet.height)
        newGraphics.beginDraw()
        newGraphics.background(Theme.backGroundColor)
        val img = pattern.graphics.get()
        newGraphics.image(img, 0f, 0f)
        newGraphics.endDraw()

        val desktopDirectory = System.getProperty("user.home") + "/Desktop/"
        newGraphics.save("$desktopDirectory${pApplet.frameCount}.png")
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

    override fun toggleDrawBounds() {
        drawBounds = !drawBounds
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
    override fun toggle3D() {
        isThreeD = !isThreeD
    }

    override fun toggleYaw() {
        threeD.isYawing = !threeD.isYawing
    }

    override fun togglePitch() {
        threeD.isPitching = !threeD.isPitching
    }

    override fun toggleRoll() {
        threeD.isRolling = !threeD.isRolling
    }

    override fun centerAndResetRotations() {
        center()
        reset3DAndStopRotations()
    }

    private fun reset3DAndStopRotations() {

        threeD.reset()
        onResetRotations()

        // used for individual boxes
        isThreeD = false


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

    private fun fillSquare(
        x: Float,
        y: Float,
        size: Float,
    ) {


        val width = size.roundToIntIfGreaterThanReference(size)
        val posX = x.roundToIntIfGreaterThanReference(size)
        val posY = y.roundToIntIfGreaterThanReference(size)

        setFill(posX, posY)

        if (width > 4 && isThreeD) {
            draw3DBox(posX, posY, width)
        } else {
            val corners = threeD.rectCorners
            val transformedCorners = corners.map { corner ->
                PVector(
                    corner.x.roundToIntIfGreaterThanReference(size),
                    corner.y.roundToIntIfGreaterThanReference(size)
                )
            }

            pattern.graphics.quadPlus(transformedCorners, shrinkEdges = true)
        }
    }

    private fun draw3DBox(x: Float, y: Float, width: Float) {
        with(pattern.graphics) {
            push()
            translate(x + width / 2, y + width / 2)
            strokeWeight(1f)
            stroke(Theme.boxOutlineColor)

            if (threeD.isPitching)
                rotateX(rotationAngle)
            if (threeD.isYawing)
                rotateY(rotationAngle)
            if (threeD.isRolling)
                rotateZ(rotationAngle)
            box(width)
            pop()
        }
    }

    private fun setFill(x: Float, y: Float) {

        with(pattern.graphics) {
            val color = if (rainbowMode) {
                colorMode(PConstants.HSB, 360f, 100f, 100f, 255f)
                val hue = PApplet.map(x + y, 0f, canvas.width + canvas.height, 0f, 360f)
                color(hue, 100f, 100f, 255f)
            } else {
                Theme.cellColor
            }
            fill(ghostState.applyAlpha(color))
            pattern.graphics.stroke(ghostState.applyAlpha(color))
        }
    }


    private var rotationAngle = 0f

    private fun updateBoxRotationAngle() {
        // Increment the angle based on time, you can adjust the speed by changing the 0.01f
        rotationAngle += 0.01f

        // Keep the angle in the range of 0 to TWO_PI
        if (rotationAngle >= TWO_PI) {
            rotationAngle -= TWO_PI
        }
    }


    private var actualRecursions = 0L
    private var startDelta = 0

    private fun drawPattern(life: LifeUniverse, shouldAdvance: Boolean) {


        with(pattern.graphics) {

            beginDraw()
            push()

            // handle3D(shouldAdvance)

            ghostState.prepareGraphics(this)

            updateBoxRotationAngle()

            updateStroke()

            if (shouldAdvance)
                threeD.rotateActiveRotations()

            val shouldDraw = when {
                ghostState !is Ghosting && RunningModeController.isPaused -> true
                ghostState !is Ghosting -> true
                ghostState is Ghosting && shouldAdvance -> true
                else -> false
            }

            if (shouldDraw) {
                drawBounds(life)
                drawVisibleNodes(life)
            }

            pop()
            endDraw()
        }

        // reset the position in case you've had mouse moves
        lifeFormPosition[0f] = 0f
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
        with(nodePath.getLowestEntryFromRoot(life.root)) {
            actualRecursions = 0L


            val startingNode = node
            val size = size
            val offsetX = left
            val offsetY = top

            startDelta = life.root.level - startingNode.level

            drawNodeRecurse(startingNode, size, offsetX, offsetY)
        }
    }

    private fun updateStroke() {
        if (canvas.zoomLevel > 4f) {
            pattern.graphics.stroke(Theme.boxOutlineColor)
        } else {
            pattern.graphics.noStroke()
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

        val leftWithOffset = left + canvas.offsetX
        val topWithOffset = top + canvas.offsetY

        // If we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size <= 1f && node.population.isNotZero()) {
            fillSquare(leftWithOffset, topWithOffset, 1f)
        } else if (node is LeafNode && node.population.isOne()) {
            fillSquare(leftWithOffset, topWithOffset, canvas.zoomLevel)
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
        if (!drawBounds) return

        val bounds = life.rootBounds

        val setFillLambda: (Float, Float) -> Unit = { x, y -> setFill(x, y) }


        // use the bounds of the "living" section of the universe to determine
        // a visible boundary based on the current canvas offsets and cell size
        val boundingBox = BoundingBox(bounds, canvas, threeD, setFillLambda)
        boundingBox.draw(pattern.graphics)

        var currentLevel = life.root.level - 2

        while (currentLevel < life.root.level) {
            val halfSize = LifeUniverse.pow2(currentLevel)
            val universeBox =
                BoundingBox(Bounds(-halfSize, -halfSize, halfSize, halfSize), canvas, threeD, setFillLambda)
            universeBox.draw(pattern.graphics, drawCrossHair = true)
            currentLevel++
        }
    }

    private fun asyncNextGeneration() {
        life.nextGeneration()
        // targetStep += 1 // use this for testing - later on you can implement lightspeed around this
    }

    companion object {
        private const val LIFE_FORM_PROPERTY = "lifeForm"
        private const val TWO_PI = (PI * 2).toFloat()
    }
}