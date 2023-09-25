package patterning.life

import patterning.util.hudFormatted

class HUDStringBuilder {
    private val hudInfo: MutableMap<String, Any>
    private var cachedFormattedString = ""
    private var lastUpdateFrame = 0

    init {
        hudInfo = LinkedHashMap() // Use LinkedHashMap to maintain the insertion order
    }

    fun addOrUpdate(key: String, value: Any?) {
        when (value) {
            is Number -> hudInfo[key] = value
            is String -> hudInfo[key] = value
            else -> throw IllegalArgumentException("value must be a Number or a String.")
        }
    }

    fun getFormattedString(
        frameCount: Int,
        updateFrequency: Int,
        updateFn: () -> Unit
    ): String {
        if (frameCount - lastUpdateFrame >= updateFrequency || cachedFormattedString.isEmpty()) {
            // lambda passed into update hudInfo - this is convoluted - it could be improved...
            updateFn()
            val formattedString = StringBuilder()
            for ((key, value) in hudInfo) {

                val formattedValue = when (value) {

                    is Number -> "$key ${value.hudFormatted()}"

                    is String -> value
                    else -> "unknown thing"
                }

                formattedString.append(formattedValue).append(DELIMITER)
            }
            // Remove the last delimiter
            if (formattedString.isNotEmpty()) {
                formattedString.setLength(formattedString.length - DELIMITER.length)
            }
            cachedFormattedString = formattedString.toString()
            lastUpdateFrame = frameCount
        }
        return cachedFormattedString
    }

    companion object {
        private const val DELIMITER = " | "
    }
}