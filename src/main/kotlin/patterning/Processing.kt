package patterning

import kotlinx.coroutines.runBlocking
import patterning.ComplexCalculationHandler.Companion.lock
import patterning.ComplexCalculationHandler.Companion.unlock
import patterning.actions.MouseEventManager
import patterning.ux.DrawRateManager
import patterning.ux.PatternDrawer
import patterning.ux.Theme
import processing.core.PApplet
import processing.data.JSONObject
import java.awt.Component
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import kotlin.math.roundToInt

class Processing : PApplet() {
    var draggingDrawing = false
    private var life: LifeUniverse? = null
    private var drawRateManager: DrawRateManager? = null
    private var complexCalculationHandlerSetStep: ComplexCalculationHandler<Int>? = null
    private var complexCalculationHandlerNextGeneration: ComplexCalculationHandler<Int>? = null
    private val mouseEventManager = MouseEventManager.instance
    private var drawer: PatternDrawer? = null

    // used to control dragging the image around the screen with the mouse
    private var lastMouseX = 0f
    private var lastMouseY = 0f

    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private var storedLife: String? = null
    private var targetStep = 0
    var isRunning = false
        private set
    private var mousePressedOverReceiver = false
    private var singleStepMode = false
    fun toggleRun() {
        isRunning = !isRunning
    }

    fun run() {
        isRunning = true
    }

    override fun settings() {
        // on startup read the size from the json file
        // eventually find a better place for it - by default it is putting it in
        // documents/data - maybe you hide it somewhere useful
        val properties: JSONObject
        val propertiesFileName = PROPERTY_FILE_NAME
        // this dataPath() was apparently the required way to save and find it
        val propertiesFile = File(dataPath(propertiesFileName))
        var width = 800
        var height = 800
        if (propertiesFile.exists() && propertiesFile.length() > 0) {
            // Load window size from JSON file
            properties = loadJSONObject(dataPath(propertiesFileName))
            width = properties.getInt("width", width)
            height = properties.getInt("height", height)
            storedLife = properties.getString("lifeForm", "")
        }

        // Set the window size
        size(width, height)
    }

    override fun setup() {
        try {
            Theme.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        surface.setResizable(true)
        frameRate(DrawRateManager.MAX_FRAME_RATE)
        targetStep = 0
        complexCalculationHandlerSetStep = ComplexCalculationHandler { p: Int ->
            performComplexCalculationSetStep(p)
        }

        complexCalculationHandlerNextGeneration = ComplexCalculationHandler { _ ->
            performComplexCalculationNextGeneration()
        }

        loadSavedWindowPositions()
        drawRateManager = DrawRateManager.instance
        drawer = PatternDrawer(this, drawRateManager!!)

        // on startup, storedLife may be loaded from the properties file but if it's not
        // just get a random one
        if (null == storedLife || storedLife!!.isEmpty()) {
            runBlocking {getRandomLifeform(false) }
        }

        // life will have been loaded in prior - either from saved life
        // or from the packaged resources so this doesn't need extra protection
        instantiateLifeform()
    }

    override fun draw() {


        // we want independent control over how often we update and display
        // the next generation of drawing
        // the frameRate can and should run faster so the user experience is responsive
        drawRateManager!!.adjustDrawRate(frameRate)
        val shouldDraw = drawRateManager!!.shouldDraw()
        val shouldDrawLifeForm = shouldDraw && lifeIsThreadSafe()

        // goForwardInTime (below) requests a nextGeneration and or a step change
        // both of which can take a while
        // given they operate on a separate thread for performance reasons
        // we don't want to put them before the draw - it will almost always be showing
        // that it is updatingLife().  so we need to put goForwardInTime _after_ the request to draw
        // and we tell the drawer whether it is still updating life since the last frame
        // we also tell the drawer whether the drawRateController thinks that it's time to draw the life form
        // in case the user has slowed it down a lot to see what's going on, it's okay for it to be going slow
        drawer!!.draw(life!!, shouldDrawLifeForm)


        // as mentioned above - this runs on a separate thread
        // and we don't want it to go any faster than the draw rate throttling mechanism
        if (shouldDraw) {
            goForwardInTime()
        }
    }

    private fun lifeIsThreadSafe(): Boolean {

        // don't start if either of these calculations are currently running
        val setStepRunning = complexCalculationHandlerSetStep?.isCalculationInProgress ?: false
        val nextGenerationRunning = complexCalculationHandlerNextGeneration?.isCalculationInProgress ?: false
        return !nextGenerationRunning && !setStepRunning
    }

    private fun goForwardInTime() {
        if (shouldStartComplexCalculationSetStep()) {
            var step = life!!.step
            step += if (step < targetStep) 1 else -1
            complexCalculationHandlerSetStep!!.startCalculation(step)
            return
        }

        // don't run generations if we're not running
        if (!isRunning) return
        if (shouldStartComplexCalculationNextGeneration()) {
            val dummy = 0
            complexCalculationHandlerNextGeneration!!.startCalculation(dummy)
        }
        if (isRunning && singleStepMode) toggleRun()
    }

    private fun shouldStartComplexCalculationSetStep(): Boolean {

        // if we're not running a complex task, and we're expecting to change the step
        return lifeIsThreadSafe() && life!!.step != targetStep
    }

    // only start these if you're not running either one
    private fun shouldStartComplexCalculationNextGeneration(): Boolean {
        return lifeIsThreadSafe()
    }

    override fun mousePressed() {
        lastMouseX += mouseX.toFloat()
        lastMouseY += mouseY.toFloat()
        assert(mouseEventManager != null)
        mouseEventManager!!.onMousePressed()
        mousePressedOverReceiver = mouseEventManager.isMousePressedOverAnyReceiver

        // If the mouse press is not over any MouseEventReceiver, we consider it as over the drawing.
        draggingDrawing = !mousePressedOverReceiver
    }

    override fun mouseReleased() {
        if (draggingDrawing) {
            draggingDrawing = false
            // if the DPS is slow then the screen won't update correctly
            // so tell the system to draw immediately
            // we used to have drawImmediately() called in move
            // but it had a negative effect of freezing the screen while drawing
            // this is a good enough compromise as most of the time DPS is not really low
            drawRateManager!!.drawImmediately()
        } else {
            mousePressedOverReceiver = false
            assert(mouseEventManager != null)
            mouseEventManager!!.onMouseReleased()
        }
        lastMouseX = 0f
        lastMouseY = 0f
    }

    override fun mouseDragged() {
        if (draggingDrawing) {
            val dx = (mouseX - lastMouseX).roundToInt().toFloat()
            val dy = (mouseY - lastMouseY).roundToInt().toFloat()
            drawer!!.move(dx, dy)
            lastMouseX += dx
            lastMouseY += dy
        }
    }

    // Override the exit() method to save window properties before closing
    override fun exit() {
        saveWindowProperties()
        super.exit()
    }

    private fun saveWindowProperties() {
        val properties = JSONObject()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = ge.screenDevices

        // this took a lot of chatting with GPT 4.0 to finally land on something that
        // would work
        val frame = frame

        // Find the screen where the window is located
        var screenIndex = 0
        for (i in screens.indices) {
            val screen = screens[i]
            if (screen.defaultConfiguration.bounds.contains(frame.location)) {
                screenIndex = i
                break
            }
        }
        val screen = screens[screenIndex]
        val screenBounds = screen.defaultConfiguration.bounds
        properties.setInt("x", frame.x - screenBounds.x)
        properties.setInt("y", frame.y - screenBounds.y)
        properties.setInt("width", width)
        properties.setInt("height", height)
        properties.setInt("screen", screenIndex)
        properties.setString("lifeForm", storedLife)
        saveJSONObject(properties, dataPath(PROPERTY_FILE_NAME))
    }

    private fun performComplexCalculationSetStep(step: Int) {
        life!!.step = step
        // todo for some reason this needs to exist or maximum volatility gun goes nuts if you step too quickly
        drawer!!.clearUndoDeque()
    }

    private fun performComplexCalculationNextGeneration() {
        life!!.nextGeneration()
    }

    private fun loadSavedWindowPositions() {
        val properties: JSONObject
        val propertiesFileName = PROPERTY_FILE_NAME
        val propertiesFile = File(dataPath(propertiesFileName))
        var x = 100
        var y = 100
        var screenIndex = 0
        if (propertiesFile.exists() && propertiesFile.length() > 0) {
            // Load window position, and screen from JSON file
            properties = loadJSONObject(dataPath(propertiesFileName))
            x = properties.getInt("x", x)
            y = properties.getInt("y", y)
            screenIndex = properties.getInt("screen", screenIndex)
        }

        // Set the window location based on the screen index
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = ge.screenDevices
        screenIndex = screenIndex.coerceAtMost(screens.size - 1)
        val screen = screens[screenIndex]
        val screenBounds = screen.defaultConfiguration.bounds
        x += screenBounds.x
        y += screenBounds.y

        // use the chatGPT way to get a thing that can do you what you want
        val frame = frame
        frame.setLocation(x, y)
    }

    suspend fun getRandomLifeform(reset: Boolean) {
        try {
            storedLife = ResourceManager.instance!!.getRandomResourceAsString(ResourceManager.RLE_DIRECTORY)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
        if (reset) destroyAndCreate()
    }

    private fun instantiateLifeform() {
        try {
            // static on patterning.Patterning
            isRunning = false
            life = LifeUniverse()

            // instance variables - do they need to be?
            val parser = LifeFormats()
            val newLife = parser.parseRLE(storedLife!!)
            targetStep = 0
            life!!.step = 0
            life!!.setupField(newLife.fieldX!!, newLife.fieldY!!)

            // new instances only created in instantiateLife to keep things simple
            // lifeForm not made local as it is intended to be used with display functions in the future
            drawer!!.setupNewLife(life!!)
        } catch (e: NotLifeException) {
            // todo: on failure you need to
            println(
                """
    get a life - here's what failed:
    
    ${e.message}
    """.trimIndent()
            )
        }
    }

    private val frame: Frame
        get() {

            // chatGPT thinks that this will only work with processing 4 so...
            // probably it would be helpful to have a non-processing mechanism
            // you're also a little bit reliant on the processing environment so...
            var comp = getSurface().native as Component
            while (comp !is Frame) {
                comp = comp.parent
            }
            return comp
        }

    // either bring us back to the start on the current life form
    // or get a random one from the well...
    suspend fun destroyAndCreate() {
        lock()
        try {
            instantiateLifeform()
        } finally {
            unlock()
        }
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

    fun handleStep(faster: Boolean) {
        var increment = if (faster) 1 else -1
        if (targetStep + increment < 0) increment = 0
        targetStep += increment
    }

    fun fitUniverseOnScreen() {
        drawer!!.center(life!!.rootBounds, fitBounds = true, saveState = true)
    }

    val numberedLifeForm: Unit
        get() {

            // subclasses of PApplet will have a keyCode
            // so this isn't magical
            val number = keyCode - '0'.code
            try {
                storedLife =
                    ResourceManager.instance!!.getResourceAtFileIndexAsString(ResourceManager.RLE_DIRECTORY, number)
               runBlocking{ destroyAndCreate() }
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }
        }

    fun centerView() {
        drawer!!.center(life!!.rootBounds, fitBounds = false, saveState = true)
    }

    fun toggleSingleStep() {
        if (isRunning && !singleStepMode) isRunning = false
        singleStepMode = !singleStepMode
    }

    companion object {
        private const val PROPERTY_FILE_NAME = "patterning_autosave.json"

    }
}