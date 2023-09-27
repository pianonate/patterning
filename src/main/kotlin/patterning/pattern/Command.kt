package patterning.pattern

import patterning.events.KeyboardShortcut
import processing.event.KeyEvent

interface Command {
    fun invokeFeature()
    fun matches(event: KeyEvent): Boolean
    val isEnabled: Boolean
    val isValidForCurrentOS: Boolean
    val invokeEveryDraw: Boolean
    val invokeAfterDelay: Boolean
    val keyboardShortcuts: Set<KeyboardShortcut>
    val usage: String
    val validKeyCombosForCurrentOS: Set<KeyboardShortcut>
}