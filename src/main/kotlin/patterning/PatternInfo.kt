package patterning

class PatternInfo {
    private val data: MutableMap<String, Number> = LinkedHashMap() // Use LinkedHashMap to maintain the insertion order

    fun addOrUpdate(key: String, value: Number) {
        data[key] = value
    }

    // A method that allows the data map to be read but not modified from outside
    fun getData(): Map<String, Number> = data
}