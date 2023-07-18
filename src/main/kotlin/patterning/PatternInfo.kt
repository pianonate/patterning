package patterning

class PatternInfo(private val updateFunction: () -> Unit) {
    private val data: MutableMap<String, FlexibleInteger> = LinkedHashMap()

    fun addOrUpdate(key: String, value: FlexibleInteger) {
        data[key] = value
    }

    fun getData(): Map<String, FlexibleInteger> {
        updateFunction.invoke()  // Update the data before returning it
        return data
    }
}