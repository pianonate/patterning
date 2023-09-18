package patterning.actions

import processing.event.KeyEvent

/**
 *  used by ControlKeyCallback to notify controls associated with KeyEvent that a key has been
 *  pressed or released, so they can adjust their appearance accordingly
 */
interface ControlKeyEventObservable {
    fun notifyControlOnKeyPress(event: KeyEvent)
    fun notifyControlOnKeyRelease(event: KeyEvent)
}