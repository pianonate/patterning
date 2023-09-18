package patterning.actions

import processing.event.KeyEvent

/**
 * primarily used by the UX to be notified of all key presses
 * especially for situations such as - press any key to continue
 */
interface KeyEventObserver {
    // right now KeyObservers have to check for themselves
    // whether they are getting a key press or a key release as
    // both events are called...
    fun notifyGlobalKeyPress(event: KeyEvent)
    fun notifyGlobalKeyRelease(event: KeyEvent)
}