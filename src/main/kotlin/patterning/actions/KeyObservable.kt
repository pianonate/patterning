package patterning.actions

import processing.event.KeyEvent

interface KeyObservable {
    fun notifyKeyObservers(event: KeyEvent)
}