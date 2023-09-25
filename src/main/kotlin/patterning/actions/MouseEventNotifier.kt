package patterning.actions

import patterning.state.RunningModeController

object MouseEventNotifier {
    private val mouseEventObservers: MutableSet<MouseEventObserver> = HashSet()

    fun addMouseEventObserver(receiver: MouseEventObserver) = mouseEventObservers.add(receiver)
    fun addAll(receivers: Collection<MouseEventObserver>) = mouseEventObservers.addAll(receivers)

    val isMousePressedOverAnyReceiver: Boolean
        get() = pressedReceiver != null

    private var pressedReceiver: MouseEventObserver? = null

    fun onMousePressed() {
        if (!RunningModeController.isUXAvailable) {
            return
        }
        pressedReceiver = null
        mouseEventObservers.forEach { receiver ->
            if (!isMousePressedOverAnyReceiver && receiver.mousePressedOverMe()) {
                pressedReceiver = receiver
                receiver.onMousePressed()
                return@forEach  // Early exit
            }
        }
    }

    fun onMouseReleased() {
        if (!RunningModeController.isUXAvailable) {
            return
        }
        if (pressedReceiver != null) {
            pressedReceiver?.onMouseReleased()
            pressedReceiver = null
        }
    }
}