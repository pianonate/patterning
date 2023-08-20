package patterning.actions

import patterning.Theme
import patterning.state.RunningModeController
import processing.core.PApplet
import processing.event.KeyEvent

object KeyHandler {
    
    private val _pressedKeys: MutableSet<Int> = mutableSetOf()
    val pressedKeys: Set<Int> get() = _pressedKeys
    
    private var keyCallbacks: MutableMap<Set<KeyCombo>, KeyCallback> = mutableMapOf()
    private val keyEventObservers: MutableList<KeyEventObserver> = mutableListOf()
    
    var latestKeyCode: Int = 0
        get() = pressedKeys.last()
        private set
    
    private lateinit var pApplet: PApplet
    private var keyEvent: KeyEvent? = null
    private const val REPEAT_DELAY = 200
    private var repeatTime = System.currentTimeMillis()
    
    fun addKeyObserver(observer: KeyEventObserver) {
        keyEventObservers.add(observer)
    }
    
    fun registerKeyHandler(processing: PApplet) {
        pApplet = processing
        if (Theme.useOpenGL) {
            processing.registerMethod("pre", this)
            processing.registerMethod("keyEvent", this)
            
        } else {
            processing.registerMethod("keyEvent", this)
        }
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
        
        val keyState = when (event.action) {
            KeyEvent.PRESS -> KeyState.PRESSED
            KeyEvent.RELEASE -> KeyState.RELEASED
            else -> KeyState.TYPED
        }
        
        if (Theme.useOpenGL) {
            
            when (event.action) {
                KeyEvent.PRESS -> {
                    println("\nin keyEvent PRESSED key:${event.key} instantiated keyEvent")
                    keyEvent = event
                }
                
                KeyEvent.RELEASE -> {
                    handleKeyEvent(event, KeyState.RELEASED)
                    keyEvent = null
                    println("in keyEvent RELEASED - nulled out keyEvent")
                }
            }
        } else {
            handleKeyEvent(event, keyState)
        }
        
        
    }
    
    @Suppress("unused")
    fun pre() {
        
        if (!pApplet.keyPressed) {
            _pressedKeys.clear() // clear movement keys
        }
        
        if (pApplet.keyPressed && keyEvent != null) {
            if ((System.currentTimeMillis() - REPEAT_DELAY) > repeatTime) {
                println("in pre with action:${keyEvent!!.action} key:${keyEvent!!.key} now call handleKeyEvent with KeyState.PRESSED")
                handleKeyEvent(keyEvent!!, KeyState.PRESSED)
                repeatTime = System.currentTimeMillis()
            }
        }
    }
    
    private fun handleKeyEvent(event: KeyEvent, keyState: KeyState) {
        val keyCode = event.keyCode
        println("in handleKeyEvent action:${event.action} key:${event.key}")
        
        // if not found, bail
        val matchingCallback = keyCallbacks.values.find { it.matches(event) } ?: return
        
        if (keyState == KeyState.PRESSED) {
            _pressedKeys.add(keyCode)
            if (RunningModeController.isUXAvailable) {
                // disallow feature invoking during testing
                println(matchingCallback.usage)
                matchingCallback.invokeFeature()
            }
            // Check if matchingCallback is KeyObservable - for callbacks that notify other parts of the system
            if (matchingCallback is KeyEventObservable) {
                matchingCallback.notifyKeyPressed(event)
            }
            // now let all the observers know - especially the UX which will handle
            // given key events can be disabled when isUXAvailable is false
            // we need to be sure to do this last as this is how the UX gets re-enabled
            // in the UX class where it will invoke RunningModeController.play()
            keyEventObservers.forEach { it.onKeyPress(event) }
        }
        if (keyState == KeyState.RELEASED) {
            _pressedKeys.remove(keyCode)
            latestKeyCode = keyCode
            
            if (matchingCallback is KeyEventObservable) {
                matchingCallback.notifyKeyReleased(event)
            }
            
            keyEventObservers.forEach { it.onKeyRelease(event) }
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