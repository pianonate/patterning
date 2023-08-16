package patterning

import processing.core.PApplet

class Patterning {
    companion object {
        @JvmStatic // do not remove - at this point, this is how gradle finds main
        fun main(args: Array<String>) {
            Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                // Handle the exception here
                PApplet.println("Exception in thread: ${thread.name} - ${exception.message}")
            }
            PApplet.main("patterning.PatterningPApplet")
        }
    }
}