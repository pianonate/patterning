package patterning.pattern

interface Movable {
    fun center()
    fun fitToScreen()
    fun saveUndoState()
    fun toggleDrawBounds()
    fun undoMovement()
}