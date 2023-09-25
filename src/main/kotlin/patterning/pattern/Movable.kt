package patterning.pattern

interface Movable {
    fun center()
    fun fitToScreen()
    fun zoom(zoomIn: Boolean, x: Float, y: Float)
}