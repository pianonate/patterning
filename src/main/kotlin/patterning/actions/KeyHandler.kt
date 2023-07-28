package patterning.actions

import processing.core.PApplet
import processing.event.KeyEvent

object KeyHandler {

    private val _pressedKeys: MutableSet<Int> = mutableSetOf()
    val pressedKeys: Set<Int> get() = _pressedKeys

    private var keyCallbacks: MutableMap<Set<KeyCombo>, KeyCallback> = mutableMapOf()

    fun registerKeyHandler(processing: PApplet) {
        processing.registerMethod("keyEvent", this)
    }

    fun addKeyCallback(callback: KeyCallback) {
        val keyCombos = callback.keyCombos
        val duplicateKeyCombos = keyCallbacks.keys.flatten().intersect(keyCombos)
        require(duplicateKeyCombos.isEmpty()) {
            "The following key combos are already associated with another callback: ${
                duplicateKeyCombos.joinToString(
                    ", "
                )
            }"
        }
        keyCallbacks[keyCombos] = callback
    }


    @Suppress("unused")
    fun keyEvent(event: KeyEvent) {
        val keyCode = event.keyCode
        // elvis operator - if not null, assign to matchingCallback - if null, return
        val matchingCallback = keyCallbacks.values.find { it.matches(event) } ?: return

        if (event.action == KeyEvent.PRESS) {
            _pressedKeys.add(keyCode)
            matchingCallback.invokeFeature()
            // Check if matchingCallback is KeyObservable
            if (matchingCallback is KeyObservable) {
                matchingCallback.notifyKeyObservers()
            }
        }
        if (event.action == KeyEvent.RELEASE) {
            _pressedKeys.remove(keyCode)
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
}