package patterning.actions

import processing.event.KeyEvent

interface KeyCallback {
    fun invokeFeature()
    fun matches(event: KeyEvent): Boolean
    val isValidForCurrentOS: Boolean
    val invokeEveryDraw: Boolean
    val keyCombos: Set<KeyCombo>
    val usage: String
    val validKeyCombosForCurrentOS: Set<KeyCombo>
}