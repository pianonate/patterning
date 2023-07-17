package patterning

class PatternInfo(private val updateFunction: () -> Unit) {
    private val data: MutableMap<String, Number> = LinkedHashMap()

    fun addOrUpdate(key: String, value: Number) {
        data[key] = value
    }

    fun getData(): Map<String, Number> {
        updateFunction.invoke()  // Update the data before returning it
        return data
    }
}