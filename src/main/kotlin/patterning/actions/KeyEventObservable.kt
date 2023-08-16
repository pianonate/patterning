package patterning.actions

import processing.event.KeyEvent

interface KeyEventObservable {
    fun notifyKeyPressed(event: KeyEvent)
    fun notifyKeyReleased(event: KeyEvent)
}