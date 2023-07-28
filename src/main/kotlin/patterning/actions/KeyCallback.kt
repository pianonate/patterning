package patterning.actions

import processing.event.KeyEvent

interface KeyCallback {
    fun invokeFeature()
    fun getUsageText(): String
    fun matches(event: KeyEvent): Boolean
    val keyCombos: Set<KeyCombo>
    val validKeyCombosForCurrentOS: Set<KeyCombo>
    val isValidForCurrentOS: Boolean
}