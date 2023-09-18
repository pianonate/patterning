package patterning.actions

import patterning.state.RunningModeController

object MouseEventNotifier {
    private val mouseEventReceivers: MutableSet<MouseEventReceiver> = HashSet()

    fun addReceiver(receiver: MouseEventReceiver) = mouseEventReceivers.add(receiver)
    fun addAll(receivers: Collection<MouseEventReceiver>) = mouseEventReceivers.addAll(receivers)

    val isMousePressedOverAnyReceiver: Boolean
        get() = pressedReceiver != null

    private var pressedReceiver: MouseEventReceiver? = null

    fun onMousePressed() {
        if (!RunningModeController.isUXAvailable) {
            return
        }
        pressedReceiver = null
        mouseEventReceivers.forEach { receiver ->
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