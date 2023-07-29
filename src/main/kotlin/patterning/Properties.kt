package patterning

import java.awt.Component
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.File
import processing.core.PApplet
import processing.data.JSONObject

class Properties(private val parent: PApplet) {

    private val dataPath = parent.dataPath(FILE_NAME)

    private val propertiesFile = File(dataPath)

    private val properties: JSONObject = if (propertiesFile.exists() && propertiesFile.length() > 0) {
        parent.loadJSONObject(dataPath)
    } else {
        JSONObject()
    }

    private val frame: Frame
        get() {
            var comp = parent.surface.native as Component
            while (comp !is Frame) {
                comp = comp.parent
            }
            return comp
        }

    val width: Int
        get() = properties.getInt("width", 800)

    val height: Int
        get() = properties.getInt("height", 800)

    var storedLife: String
        get() = properties.getString("lifeForm", "")
        set(value) {
            properties.setString("lifeForm", value)
        }

    fun saveProperties() {
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
        properties.setInt("width", parent.width)
        properties.setInt("height", parent.height)
        properties.setInt("screen", screenIndex)
        parent.saveJSONObject(properties, dataPath)
    }

    fun setWindowPosition() {
        var x = properties.getInt("x", 100)
        var y = properties.getInt("y", 100)
        var screenIndex = properties.getInt("screen", 0)

        // Set the window location based on the screen index
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = ge.screenDevices
        screenIndex = screenIndex.coerceAtMost(screens.size - 1)
        val screen = screens[screenIndex]
        val screenBounds = screen.defaultConfiguration.bounds
        x += screenBounds.x
        y += screenBounds.y

        val frame = frame
        frame.setLocation(x, y)
    }

    companion object {
        private const val FILE_NAME = "patterning_autosave.json"
    }
}