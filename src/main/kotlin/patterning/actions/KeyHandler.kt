package patterning.actions

import patterning.state.RunningModeController
import processing.core.PApplet
import processing.event.KeyEvent

object KeyHandler {
    
    private val _pressedKeys: MutableSet<Int> = mutableSetOf()
    val pressedKeys: Set<Int> get() = _pressedKeys
    
    private var keyCallbacks: MutableMap<Set<KeyCombo>, KeyCallback> = mutableMapOf()
    private val keyObservers: MutableList<KeyObserver> = mutableListOf()
    
    
    var latestKeyCode: Int = 0
        get() = pressedKeys.last()
        private set
    
    fun addKeyObserver(observer: KeyObserver) {
        keyObservers.add(observer)
    }
    
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
        
        // if not found, bail
        val matchingCallback = keyCallbacks.values.find { it.matches(event) } ?: return
        
        if (event.action == KeyEvent.PRESS) {
            _pressedKeys.add(keyCode)
            if (RunningModeController.isUXAvailable) {
                // disallow feature invoking during testing
                matchingCallback.invokeFeature()
            }
            // Check if matchingCallback is KeyObservable - for callbacks that notify other parts of the system
            if (matchingCallback is KeyObservable) {
                matchingCallback.notifyKeyObservers(event)
            }
        }
        if (event.action == KeyEvent.RELEASE) {
            _pressedKeys.remove(keyCode)
            latestKeyCode = keyCode
        }
        
        // now let all the observers know - especially the UX which will handle
        // playing things while the UX is disabled from key presses during LoadingState()
        // while loading
        keyObservers.forEach { it.onKeyEvent(event) }
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