package patterning.life

import patterning.util.hudFormatted

class HUDStringBuilder {
    private val data // Changed from Number to Object
            : MutableMap<String, Any>
    private var cachedFormattedString = ""
    private var lastUpdateFrame = 0

    init {
        data = LinkedHashMap() // Use LinkedHashMap to maintain the insertion order
    }

    fun addOrUpdate(key: String, value: Any?) {
        when (value) {
            is Number -> data[key] = value
            is String -> data[key] = value
            else -> throw IllegalArgumentException("value must be a Number or a String.")
        }
    }

    fun getFormattedString(
        frameCount: Int,
        updateFrequency: Int,
        updateFn: () -> Unit
    ): String {
        if (frameCount - lastUpdateFrame >= updateFrequency || cachedFormattedString.isEmpty()) {
            updateFn()
            val formattedString = StringBuilder()
            for ((key, value) in data) {

                val formattedValue = when (value) {

                    is Number -> "$key ${value.hudFormatted()}"

                    is String -> value
                    else -> "unknown thing"
                }

                formattedString.append(formattedValue).append(delimiter)
            }
            // Remove the last delimiter
            if (formattedString.isNotEmpty()) {
                formattedString.setLength(formattedString.length - delimiter.length)
            }
            cachedFormattedString = formattedString.toString()
            lastUpdateFrame = frameCount
        }
        return cachedFormattedString
    }

    companion object {
        private const val delimiter = " | "
    }
}