package patterning.actions

import processing.event.KeyEvent

/* KeyCallback by keyCallback tells kotlin to delegate all non-overridden behavior to the base class */
class ControlKeyCallback(
    private val keyCallback: KeyCallback,
    private val primaryObserver: KeyCallbackObserver
) : KeyCallback by keyCallback, ControlKeyCallbackObservable {

    /**
     * allows KeyCallbacks associated with Controls to notify the Controls also that they have been invoked
     * so the controls can highlight themselves (for one)
     */
    override fun notifyControlOnKeyPress(event: KeyEvent) {
        primaryObserver.notifyGlobalKeyPress(event)
    }

    override fun toString(): String {
        return keyCallback.toString()
    }
}