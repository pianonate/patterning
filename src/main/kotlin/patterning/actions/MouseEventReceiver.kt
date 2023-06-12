package patterning.actions

interface MouseEventReceiver {
    fun onMousePressed()
    fun onMouseReleased()
    fun mousePressedOverMe(): Boolean
}