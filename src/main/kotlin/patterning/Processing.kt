package patterning

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import patterning.actions.KeyHandler
import patterning.actions.MouseEventManager
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
    private lateinit var life: LifeUniverse

    private lateinit var asyncNextGeneration: AsyncCalculationRunner
    private lateinit var drawer: PatternDrawer

    // used to control dragging the image around the screen with the mouse
    private var lastMouseX = 0f
    private var lastMouseY = 0f

    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private var storedLife: String = ""
    private var targetStep = 0

    private var mousePressedOverReceiver = false

    val gps
        get() = asyncNextGeneration.getRate()

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
        Theme.initialize(this)
        KeyHandler.registerKeyHandler(this)

        surface.setResizable(true)
        targetStep = 0
        asyncNextGeneration = AsyncCalculationRunner(RATE_PER_SECOND_WINDOW) { asyncNextGeneration() }

        loadSavedWindowPositions()

        drawer = PatternDrawer(this)
            .also {
                println(KeyHandler.usageText)
            }

        // on startup, storedLife may be loaded from the properties file but if it's not
        // just get a random one
        if (storedLife.isEmpty()) {
            runBlocking { getRandomLifeform(false) }
        }

        // life will have been loaded in prior - either from saved life
        // or from the packaged resources so this doesn't need extra protection
        instantiateLifeform()
    }

    override fun draw() {

        drawer.draw(life)
        goForwardInTime()
    }

    private suspend fun asyncNextGeneration() {
        coroutineScope { life.nextGeneration() }
        // targetStep += 1 // use this for testing - later on you can implement lightspeed around this
    }


    private fun goForwardInTime() {

        if (!asyncNextGeneration.isRunning) {
            life.step = when {
                life.step < targetStep -> life.step + 1
                life.step > targetStep -> life.step - 1
                else -> life.step
            }
        }

        if (RunningState.shouldAdvance())
            asyncNextGeneration.startCalculation()
    }

    override fun mousePressed() {
        lastMouseX += mouseX.toFloat()
        lastMouseY += mouseY.toFloat()
        MouseEventManager.onMousePressed()
        mousePressedOverReceiver = MouseEventManager.isMousePressedOverAnyReceiver

        // If the mouse press is not over any MouseEventReceiver, we consider it as over the drawing.
        draggingDrawing = !mousePressedOverReceiver
    }

    override fun mouseReleased() {
        if (draggingDrawing) {
            draggingDrawing = false
        } else {
            mousePressedOverReceiver = false
            MouseEventManager.onMouseReleased()
        }
        lastMouseX = 0f
        lastMouseY = 0f
    }

    override fun mouseDragged() {
        if (draggingDrawing) {
            val dx = (mouseX - lastMouseX).roundToInt().toFloat()
            val dy = (mouseY - lastMouseY).roundToInt().toFloat()
            drawer.move(dx, dy)
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

    fun getRandomLifeform(reset: Boolean) {
        try {
            storedLife = ResourceManager.instance!!.getRandomResourceAsString(ResourceManager.RLE_DIRECTORY)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
        if (reset) instantiateLifeform()
    }

    fun instantiateLifeform() {
        try {

            RunningState.pause()
            asyncNextGeneration.cancelAndWait()

            life = LifeUniverse()

            // instance variables - do they need to be?
            val parser = LifeFormats()
            val newLife = parser.parseRLE(storedLife)

            targetStep = 0
            life.step = 0
            life.setupLife(newLife.fieldX!!, newLife.fieldY!!)

            // new instances only created in instantiateLife to keep things simple
            // lifeForm not made local as it is intended to be used with display functions in the future
            drawer.setupNewLife(life)
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
        drawer.center(life.rootBounds, fitBounds = true, saveState = true)
    }

    val numberedLifeForm: Unit
        get() {

            // subclasses of PApplet will have a keyCode
            // so this isn't magical
            val number = keyCode - '0'.code
            try {
                storedLife =
                    ResourceManager.instance!!.getResourceAtFileIndexAsString(ResourceManager.RLE_DIRECTORY, number)
                instantiateLifeform()
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }
        }

    fun centerView() {
        drawer.center(life.rootBounds, fitBounds = false, saveState = true)
    }

    companion object {
        private const val PROPERTY_FILE_NAME = "patterning_autosave.json"
        private const val RATE_PER_SECOND_WINDOW = 1 // how big is your window to calculate the rate?

    }
}