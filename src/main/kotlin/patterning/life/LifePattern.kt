package patterning.life

import patterning.Canvas
import patterning.GraphicsReference
import patterning.Properties
import patterning.Theme
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
import patterning.util.FlexibleDecimal
import patterning.util.FlexibleInteger
import patterning.util.ResourceManager
import patterning.util.roundToIntIfGreaterThanReference
import processing.core.PApplet
import processing.core.PConstants.TWO_PI
import processing.core.PGraphics
import processing.core.PVector
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.net.URISyntaxException
import kotlin.math.roundToInt


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
    Steppable,
    ThreeDimensional {

    private lateinit var life: LifeUniverse
    private lateinit var lifeForm: LifeForm

    private val universeSize = UniverseSize(canvas)
    private var biggestDimension: FlexibleInteger = FlexibleInteger.ZERO


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
        shouldContinue = { node, size, nodeLeft, nodeTop -> shouldContinue(node, size, nodeLeft, nodeTop) },
        universeSize = universeSize,
        canvas = canvas
    )

    private var pattern: GraphicsReference
    private var drawBounds = false
    private var is3D = false
    private var isYawing = false
    private var isPitching = false
    private var isRolling = false
    private var yawCount = 0
    private var pitchCount = 0
    private var rollCount = 0
    private var currentYawAngle = 0f
    private var currentPitchAngle = 0f
    private var currentRollAngle = 0f


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
     * Pattern overrides
     */
    override fun draw() {

        performanceTest.execute()

        drawPattern(life)

        pApplet.image(pattern.graphics, lifeFormPosition.x, lifeFormPosition.y)

        goForwardInTime()
    }

    override fun getHUDMessage(): String {
        return hudInfo.getFormattedString(
            pApplet.frameCount,
            80
        ) {
            hudInfo.addOrUpdate("fps", pApplet.frameRate.roundToInt())
            hudInfo.addOrUpdate("gps", asyncNextGenerationJob.getRate())
            hudInfo.addOrUpdate("zoom", canvas.zoomLevel.toFloat())
            hudInfo.addOrUpdate("mc", canvas.mc.precision)

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
        canvas.moveCanvasOffsets(FlexibleDecimal.create(dx), FlexibleDecimal.create(dy))
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

            canvas.zoomLevel = widthRatio.coerceAtMost(heightRatio)

            reset3DParams()
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
        is3D = !is3D
    }

    override fun toggleYaw() {
        isYawing = !isYawing
    }

    override fun togglePitch() {
        isPitching = !isPitching
    }

    override fun toggleRoll() {
        isRolling = !isRolling
    }

    private fun reset3DParams() {
        yawCount = 0
        pitchCount = 0
        rollCount = 0
        currentYawAngle = 0f
        currentPitchAngle = 0f
        currentRollAngle = 0f
    }

    private fun reset3DAndStopRotations() {

        reset3DParams()

        is3D = false
        isYawing = false
        isPitching = false
        isRolling = false
        pattern.graphics.camera()
    }

    /**
     * for yaw
     *
     * fov:  defines the extent of the observable world that can be seen from the camera's position. A common value is
     * π / 3 or 60 degrees, which gives a good balance between a wide view and realistic perspective.
     * You can think of the FOV as the "zoom level" of the camera.
     *
     * cameraZ: This calculates the Z position of the camera based on the height of the canvas and the FOV. It ensures that the objects maintain their apparent size as you move the camera.
     *
     * radius: The radius is the distance from the camera to the center of rotation. By setting it equal to the cameraZ, the objects maintain their apparent size.
     *
     * angle: This calculates the angle of rotation based on the current yawCount. It ranges from 0 to 2π, or 360 degrees. representing a full circle.  We want to start at 90 degrees, so we add 90 to the yawCount.
     *
     * eyeX and eyeZ: calculate the X and Z positions of the camera based on the angle and radius. By using sine and cosine, we ensure that the camera moves in a circle around the object.
     *
     * perspective: sets up the perspective projection with the defined FOV, aspect ratio, near and far clipping planes.
     *
     * camera: Here, the camera is set up with its position (eyeX, pApplet.height / 2f, eyeZ), the center of the scene that it's looking at (pApplet.width / 2f, pApplet.height / 2f, 0f), and the up vector (0f, 1f, 0f), which defines which direction is "up" from the camera's point of view.
     *
     * this code is not currently in use - but it may be the basis for being able to draw an infinite screen by controlling perspective...
     * so that while rotating the drawing can disappear into the distance
     */
    /* private fun handlePerspective3D() {

         if (isYawing) {
             val fov = (PI / 3.0).toFloat() // You may adjust this value to get the desired field of view
             val cameraZ = (pApplet.height / 2.0f) / tan(fov / 2.0f)

             val radius = cameraZ // Setting the radius equal to cameraZ to keep the apparent size consistent

             val angle = TWO_PI * ((yawCount + 90) % 360) / 360f
             val eyeX = cos(angle) * radius + pApplet.width / 2f
             val eyeZ = sin(angle) * radius

             // Set the perspective
             pApplet.perspective(
                 fov,
                 pApplet.width.toFloat() / pApplet.height.toFloat(),
                 cameraZ / 10.0f,
                 cameraZ * 10.0f
             )

             // Set the camera parameters: eye position, center position (looking at the center of the screen), and up vector
             pattern.graphics.camera(
                 eyeX,
                 pApplet.height / 2f,
                 eyeZ,
                 pApplet.width / 2f,
                 pApplet.height / 2f,
                 0f,
                 0f,
                 1f,
                 0f
             )

             yawCount++
         }
     }*/

    /**
     * private fun
     */
    private fun goForwardInTime() {

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

        if (RunningModeController.shouldAdvance())
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

        biggestDimension = FlexibleInteger.ZERO

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

        if (width > 4 && is3D) {
            pattern.graphics.push()
            pattern.graphics.translate(posX, posY)
            pattern.graphics.strokeWeight(1f)
            pattern.graphics.stroke(Theme.boxOutlineColor)
            pattern.graphics.rotateX(lerpRotate())
            pattern.graphics.box(width)
            pattern.graphics.pop()
        } else {

            pattern.graphics.rect(
                posX,
                posY,
                width, width
            )
            //println("($posX,$posY) - $size")

        }
    }

    private fun lerpRotate(): Float {

        val counter = pApplet.frameCount % Theme.threeDBoxRotationCount

        // Calculate the lerp value
        val t = counter.toFloat() / Theme.threeDBoxRotationCount.toFloat()

        return PApplet.lerp(0f, PApplet.TWO_PI, t)
    }

    private var actualRecursions = FlexibleInteger.ZERO
    private var startDelta = 0

    private fun drawPattern(life: LifeUniverse) {

        val graphics = pattern.graphics
        graphics.beginDraw()

        // Save the current transformation matrix
        pattern.graphics.push()

        updateGhost(pattern.graphics)

        updateStroke(graphics)

        updateBoundsChanged(life.root.bounds)

        handle3D()

        drawVisibleNodes(life)

        drawBounds(life)

        pattern.graphics.pop()

        graphics.endDraw()

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
            actualRecursions = FlexibleInteger.ZERO


            val startingNode = node
            val size = size
            val offsetX = left
            val offsetY = top

            startDelta = life.root.level - startingNode.level

            drawNodeRecurse(startingNode, size, offsetX, offsetY)

        }
    }

    private fun handle3D() {
        // Move to the center of the object
        pattern.graphics.translate(pApplet.width / 2f, pApplet.height / 2f)

        if (isYawing) {
            // Rotate the object around the Y-axis
            currentYawAngle = TWO_PI * (yawCount % 360) / 360f
            yawCount++
        }

        if (isPitching) {
            // Rotate the object around the X-axis
            currentPitchAngle = TWO_PI * (pitchCount % 360) / 360f
            pitchCount++
        }

        if (isRolling) {
            // Rotate the object around the Z-axis
            currentRollAngle = TWO_PI * (rollCount % 360) / 360f
            rollCount++
        }

        // Apply the current rotation angles
        pattern.graphics.rotateY(currentYawAngle)
        pattern.graphics.rotateX(currentPitchAngle)
        pattern.graphics.rotateZ(currentRollAngle)


        // Move back by half the object's size
        pattern.graphics.translate(-pApplet.width / 2f, -pApplet.height / 2f)
    }

    private fun updateStroke(graphics: PGraphics) {
        if (canvas.zoomLevelAsFloat > 4f) {
            graphics.stroke(Theme.boxOutlineColor)
        } else {
            graphics.noStroke()
        }
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
            try {
                fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), 1f)
            } catch (e: Exception) {
                println("size:$size leftWithOffset:$leftWithOffset topWithOffset:$topWithOffset")
                throw e
            }
        } else if (node is LeafNode && node.population.isOne()) {
            try {
                fillSquare(leftWithOffset.toFloat(), topWithOffset.toFloat(), canvas.zoomLevelAsFloat)
            } catch (e: Exception) {
                println("size:$size leftWithOffset:$leftWithOffset topWithOffset:$topWithOffset")
                throw e
            }
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
        // each level in we add half of the size at that to create the new size for left and top
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
        val boundingBox = BoundingBox(bounds, canvas)
        boundingBox.draw(pattern.graphics)

        var currentLevel = life.root.level - 2

        while (currentLevel < life.root.level) {
            val halfSize = LifeUniverse.pow2(currentLevel)
            val universeBox = BoundingBox(Bounds(-halfSize, -halfSize, halfSize, halfSize), canvas)
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
    }
}