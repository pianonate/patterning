package patterning.actions

import patterning.RunningMode
import patterning.RunningState
import processing.core.PApplet
import processing.event.KeyEvent

object KeyHandler {

    private val _pressedKeys: MutableSet<Int> = mutableSetOf()
    val pressedKeys: Set<Int> get() = _pressedKeys

    private var keyCallbacks: MutableMap<Set<KeyCombo>, KeyCallback> = mutableMapOf()

    var latestKeyCode: Int = 0
        get() = pressedKeys.last()
        private set

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
            if (RunningState.runningMode != RunningMode.TESTING) {
                // disallow feature invoking during testing
                matchingCallback.invokeFeature()
            }
            // Check if matchingCallback is KeyObservable
            if (matchingCallback is KeyObservable) {
                matchingCallback.notifyKeyObservers()
            }
        }
        if (event.action == KeyEvent.RELEASE) {
            _pressedKeys.remove(keyCode)
            latestKeyCode = keyCode
        }
    }

    val usageText: String
        get() {
            val usageText = StringBuilder("\nKey Usage:\n")

            val maxKeysWidth = keyCallbacks.values
                .filter { it.isValidForCurrentOS }
                .maxOf { callback -> callback.toString().length }

            keyCallbacks.values
                .filter { callback ->
                    callback.isValidForCurrentOS
                }.sortedBy { callback ->
                    callback.usage
                }.forEach { callback ->
                    val keysString = callback.toString()
                    val usageDescription = callback.usage
                    usageText.append("${keysString.padEnd(maxKeysWidth + 1)}: $usageDescription\n")
                }

            return usageText.toString()
        }
}