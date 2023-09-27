package patterning.events

import processing.event.KeyEvent

/**
 * receive a notification if a key is pressed
 */
interface KeyEventObserver {
    fun onKeyPressed(event: KeyEvent)
}