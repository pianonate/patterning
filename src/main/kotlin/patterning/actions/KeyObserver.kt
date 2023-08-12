package patterning.actions

import processing.event.KeyEvent

interface KeyObserver {
    fun onKeyEvent(event: KeyEvent)
}