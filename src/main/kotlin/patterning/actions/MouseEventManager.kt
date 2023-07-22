package patterning.actions

import patterning.ux.Governor

class MouseEventManager private constructor() {
    private val mouseEventReceivers: MutableList<MouseEventReceiver> = ArrayList()
    fun addReceiver(receiver: MouseEventReceiver) {
        mouseEventReceivers.add(receiver)
    }

    fun addAll(receivers: Collection<MouseEventReceiver>?) {
        mouseEventReceivers.addAll(receivers!!)
    }

    var isMousePressedOverAnyReceiver = false
        private set

    private var pressedReceiver: MouseEventReceiver? = null

    fun onMousePressed() {
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
        if (pressedReceiver != null) {
            pressedReceiver!!.onMouseReleased()
            Governor.drawImmediately()
            pressedReceiver = null
        }
    }

    companion object {
        @JvmStatic
        var instance: MouseEventManager? = null
            get() {
                if (field == null) {
                    field = MouseEventManager()
                }
                return field
            }
            private set
    }
}