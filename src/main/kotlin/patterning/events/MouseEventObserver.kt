package patterning.events

interface MouseEventObserver {
    fun onMousePressed()
    fun onMouseReleased()
    fun mousePressedOverMe(): Boolean
}