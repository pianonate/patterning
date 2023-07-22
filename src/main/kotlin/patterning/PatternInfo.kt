package patterning

class PatternInfo(private val updateFunction: () -> Unit) {
    private val data: MutableMap<String, Any> = LinkedHashMap()

    fun addOrUpdate(key: String, value: Any) {
        data[key] = value
    }

    fun getData(): Map<String, Any> {
        updateFunction.invoke()  // Update the data before returning it
        return data
    }
}