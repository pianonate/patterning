package patterning

import java.nio.IntBuffer

// contains the results of a parsed lifeform
// currently only RLE formats are parsed - either pasted in or loaded from a prior
// exit/load of the app - a long way to go here...
class LifeForm internal constructor() {
    @JvmField
    var width = 0
    @JvmField
    var height = 0
    @JvmField
    var rule_s = 0
    @JvmField
    var rule_b = 0
    @JvmField
    var title = ""
    @JvmField
    var author = ""
    @JvmField
    var rule = ""
    @JvmField
    val comments: ArrayList<String>
    @JvmField
    var instructions = ""
    @JvmField
    var field_x: IntBuffer? = null
    @JvmField
    var field_y: IntBuffer? = null

    init {
        comments = ArrayList()
    }
}