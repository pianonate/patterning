package patterning.actions

import processing.event.KeyEvent

/* KeyCallback by keyCallback tells kotlin to delegate all non-overridden behavior to the base class */
class ControlKeyCallback(
    private val keyCallback: KeyCallback,
    private val primaryObserver: KeyObserver
) : KeyCallback by keyCallback, KeyObservable {
    
    override fun notifyKeyObservers(event: KeyEvent) {
        primaryObserver.onKeyEvent(event)
    }
    
    override fun toString(): String {
        return keyCallback.toString()
    }
    
}