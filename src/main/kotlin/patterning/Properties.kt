package patterning

import processing.core.PApplet
import processing.data.JSONObject
import java.io.File

class Properties(private val pApplet: PApplet) {

    private val dataPath = pApplet.dataPath(FILE_NAME)

    private val propertiesFile = File(dataPath)

    private val properties: JSONObject = if (propertiesFile.exists() && propertiesFile.length() > 0) {
        pApplet.loadJSONObject(dataPath)
    } else {
        JSONObject()
    }

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
        pApplet.saveJSONObject(properties, dataPath)
    }

    companion object {
        private const val FILE_NAME = "patterning_properties.json"
    }
}