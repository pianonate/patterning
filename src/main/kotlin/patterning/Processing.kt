package patterning

// import patterning.util.AsyncCalculationRunner
import java.awt.Component
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.File

import kotlin.math.roundToInt

import patterning.actions.KeyHandler
import patterning.actions.MouseEventManager
import patterning.life.LifeDrawer

import processing.core.PApplet
import processing.data.JSONObject

class Processing : PApplet() {
    var draggingDrawing = false

    private lateinit var drawer: LifeDrawer

    // used to control dragging the image around the screen with the mouse
    private var lastMouseX = 0f
    private var lastMouseY = 0f

    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private var storedLife: String = ""

    private var mousePressedOverReceiver = false

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

        loadSavedWindowPositions()

        drawer = LifeDrawer(
            processing = this,
            storedLife,
        ).also {
            println(KeyHandler.usageText)
        }

    }

    override fun draw() {
        drawer.draw()
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
        properties.setString("lifeForm", drawer.storedLife)
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

    companion object {
        private const val PROPERTY_FILE_NAME = "patterning_autosave.json"
    }
}