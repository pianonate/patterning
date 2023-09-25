package patterning.actions

interface MouseEventObserver {
    fun onMousePressed()
    fun onMouseReleased()
    fun mousePressedOverMe(): Boolean
}