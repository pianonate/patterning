package actions

import processing.core.PApplet
import processing.event.KeyEvent
import ux.DrawRateManager

class KeyHandler private constructor(builder: Builder) {
    private val keyCallbacks: Map<Set<KeyCombo>, KeyCallback>

    init {
        keyCallbacks = builder.keyCallbacks
        // this registration is what keeps a reference of KeyHandler around
        // for the duration of the program even though you don't explicitly
        // store it anywhere else...
        builder.processing.registerMethod("keyEvent", this)
    }

    @Suppress("unused")
    fun keyEvent(event: KeyEvent) {
        val keyCode = event.keyCode
        // elvis operator - if not null, assign to matchingCallback - if null, return
        val matchingCallback = keyCallbacks.values.find { it.matches(event) } ?: return

        if (event.action == KeyEvent.PRESS) {
            _pressedKeys.add(keyCode)
            matchingCallback.invokeFeature()
            matchingCallback.notifyKeyObservers()
            DrawRateManager.instance!!.drawImmediately()
        }
        if (event.action == KeyEvent.RELEASE) {
            _pressedKeys.remove(keyCode)
            matchingCallback.cleanupFeature()
        }
    }

    val usageText: String
        get() {
            val usageText = StringBuilder("\nKey Usage:\n")
            val processedCallbacks: MutableSet<KeyCallback> = mutableSetOf()

            val maxKeysWidth = keyCallbacks.values.filter { it.isValidForCurrentOS }.maxOfOrNull { callback ->
                callback.validKeyCombosForCurrentOS.joinToString(", ").length
            } ?: 0

            keyCallbacks.values.filter { callback ->
                callback.isValidForCurrentOS && !processedCallbacks.contains(callback)
            }.forEach { callback ->
                val keysString = callback.validKeyCombosForCurrentOS.joinToString(", ")
                val usageDescription = callback.getUsageText()
                usageText.append("${keysString.padEnd(maxKeysWidth + 1)}: $usageDescription\n")
                processedCallbacks.add(callback)
            }

            return usageText.toString()
        }

    class Builder(val processing: PApplet) {
        val keyCallbacks: MutableMap<Set<KeyCombo>, KeyCallback> = mutableMapOf()

        fun addKeyCallback(callback: KeyCallback): Builder {
            val keyCombos = callback.keyCombos
            val duplicateKeyCombos = findDuplicateKeyCombos(keyCombos)
            require(duplicateKeyCombos.isEmpty()) {
                "The following key combos are already associated with another callback: ${duplicateKeyCombos.joinToString(", ")}"
            }
            keyCallbacks[keyCombos] = callback
            return this
        }

        private fun findDuplicateKeyCombos(keyCombos: Set<KeyCombo>): Set<KeyCombo> {
            return keyCallbacks.keys.flatten().intersect(keyCombos)
        }

        fun build(): KeyHandler = KeyHandler(this)
    }

    companion object {
        private val _pressedKeys: MutableSet<Int> = mutableSetOf()
        val pressedKeys: Set<Int> get() = _pressedKeys
    }
}