package patterning

import processing.core.PApplet

class Patterning {
    companion object {
        @JvmStatic // do not remove - at this point, this is how gradle finds main
        fun main(args: Array<String>) {
            PApplet.main("patterning.PatterningPApplet")
        }
    }
}