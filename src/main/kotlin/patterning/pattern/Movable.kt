package patterning.pattern

interface Movable {
    fun center()
    fun fitToScreen()
    fun toggleDrawBounds()
    fun toggleDrawBoundaryOnly()
    fun zoom(zoomIn: Boolean, x: Float, y: Float)
}