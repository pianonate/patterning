package ux.panel

import processing.core.PApplet

enum class AlignHorizontal {
    LEFT,
    CENTER,
    RIGHT;

    fun toPApplet(): Int {
        return when (this) {
            LEFT -> PApplet.LEFT
            CENTER -> PApplet.CENTER
            RIGHT -> PApplet.RIGHT
        }
    }
}