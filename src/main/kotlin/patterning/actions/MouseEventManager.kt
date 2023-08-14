package patterning.actions

import patterning.state.RunningModeController

object MouseEventManager {
    private val mouseEventReceivers: MutableList<MouseEventReceiver> = ArrayList()
    
    fun addReceiver(receiver: MouseEventReceiver) {
        mouseEventReceivers.add(receiver)
    }
    
    fun addAll(receivers: Collection<MouseEventReceiver>) {
        mouseEventReceivers.addAll(receivers)
    }
    
    var isMousePressedOverAnyReceiver = false
        private set
    
    private var pressedReceiver: MouseEventReceiver? = null
    
    fun onMousePressed() {
        if (!RunningModeController.isUXAvailable) {
            return
        }
        isMousePressedOverAnyReceiver = false
        pressedReceiver = null
        for (receiver in mouseEventReceivers) {
            if (!isMousePressedOverAnyReceiver && receiver.mousePressedOverMe()) {
                isMousePressedOverAnyReceiver = true
                pressedReceiver = receiver
            }
            receiver.onMousePressed()
        }
    }
    
    fun onMouseReleased() {
        if (!RunningModeController.isUXAvailable) {
            return
        }
        if (pressedReceiver != null) {
            pressedReceiver!!.onMouseReleased()
            pressedReceiver = null
        }
    }
}