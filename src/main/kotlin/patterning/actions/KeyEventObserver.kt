package patterning.actions

import processing.event.KeyEvent

interface KeyEventObserver {
    // right now KeyObservers have to check for themselves
    // whether they are getting a key press or a key release as
    // both events are called...
    fun onKeyPress(event: KeyEvent)
    fun onKeyRelease(event: KeyEvent)
}