package patterning.actions

import processing.event.KeyEvent

/**
 * receive a notification if a key is pressed
 */
interface KeyCallbackObserver {
    fun onKeyPressed(event: KeyEvent)
}