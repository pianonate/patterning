package actions

import ux.DrawRateManager

class MouseEventManager private constructor() {
    private val mouseEventReceivers: MutableList<MouseEventReceiver> = ArrayList()
    fun addReceiver(receiver: MouseEventReceiver) {
        mouseEventReceivers.add(receiver)
    }

    fun addAll(receivers: Collection<MouseEventReceiver>?) {
        mouseEventReceivers.addAll(receivers!!)
    }

    var isMousePressedOverAnyReceiver = false
        // kotlin thing - says that isMousePressedOverAnyReceiver can only be read externally but can be written to interanlly
        private set

    private var pressedReceiver: MouseEventReceiver? = null

    fun onMousePressed(mouseX: Int, mouseY: Int) {
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
            DrawRateManager.getInstance().drawImmediately()
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
