package patterning.actions

import processing.event.KeyEvent

interface ControlKeyEventObservable {
    fun notifyControlOnKeyPress(event: KeyEvent)
    fun notifyControlOnKeyRelease(event: KeyEvent)
}