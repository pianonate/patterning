package patterning.life

class LifeInfo(private val updateFunction: () -> Unit) {
    private val data: MutableMap<String, Any> = LinkedHashMap()

    fun addOrUpdate(key: String, value: Any) {
        data[key] = value
    }

    val info: Map<String, Any>
        get() {
            updateFunction.invoke()  // Update the data before returning it
            return data
        }
}