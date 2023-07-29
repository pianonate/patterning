package patterning.panel

import processing.core.PApplet

enum class AlignVertical {
    TOP,
    CENTER,
    BOTTOM;

    fun toPApplet(): Int {
        return when (this) {
            TOP -> PApplet.TOP
            CENTER -> PApplet.CENTER
            BOTTOM -> PApplet.BOTTOM
        }
    }
}