package patterning.actions

import processing.event.KeyEvent

/**
 * primarily used by the UX to be notified of all key presses
 * especially for situations such as - press any key to continue
 */
interface KeyCallbackObserver {
    fun notifyGlobalKeyPress(event: KeyEvent)
}