package patterning

/*
class PatternInfo {
    private val data: MutableMap<String, Number> = LinkedHashMap() // Use LinkedHashMap to maintain the insertion order

    fun addOrUpdate(key: String, value: Number) {
        data[key] = value
    }

    // A method that allows the data map to be read but not modified from outside
    fun getData(): Map<String, Number> = data
}
*/


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