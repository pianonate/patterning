package patterning

import java.awt.Component
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.File
import processing.core.PApplet
import processing.data.JSONObject

class Properties(private val pApplet: PApplet) {
    
    private val dataPath = pApplet.dataPath(FILE_NAME)
    
    private val propertiesFile = File(dataPath)
    
    private val properties: JSONObject = if (propertiesFile.exists() && propertiesFile.length() > 0) {
        pApplet.loadJSONObject(dataPath)
    } else {
        JSONObject()
    }
    
    private val frame: Frame
        get() {
            var comp = pApplet.surface.native as Component
            while (comp !is Frame) {
                comp = comp.parent
            }
            return comp
        }
    
    val width: Int
        get() = properties.getInt("width", 800)
    
    val height: Int
        get() = properties.getInt("height", 800)
    
    fun getProperty(propertyName: String, defaultValue: String = ""): String {
        return properties.getString(propertyName, defaultValue)
    }
    
    fun setProperty(propertyName: String, value: String) {
        properties.setString(propertyName, value)
    }
    
    var performanceTestResults: JSONObject
        get() = if (properties.hasKey("performanceTestResults")) {
            properties.getJSONObject("performanceTestResults")
        } else {
            JSONObject()
        }
        set(value) {
            properties.setJSONObject("performanceTestResults", value)
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
        properties.setInt("width", pApplet.width)
        properties.setInt("height", pApplet.height)
        properties.setInt("screen", screenIndex)
        pApplet.saveJSONObject(properties, dataPath)
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
        private const val FILE_NAME = "patterning_properties.json"
    }
}