package patterning.actions

import processing.event.KeyEvent

/* KeyCallback by keyCallback tells kotlin to delegate all non-overridden behavior to the base class */
class ControlKeyCallbackEvent(
    private val keyCallback: KeyCallback,
    private val primaryObserver: KeyEventObserver
) : KeyCallback by keyCallback, KeyEventObservable {
    
    /**
     * allows KeyCallbacks associated with Controls to notify the Controls also that they have been invoked
     * so the controls can highlight themselves (for one)
     */
    override fun notifyKeyPressed(event: KeyEvent) {
        primaryObserver.onKeyPress(event)
    }
    
    override fun notifyKeyReleased(event: KeyEvent) {
        primaryObserver.onKeyRelease(event)
    }
    
    override fun toString(): String {
        return keyCallback.toString()
    }
}