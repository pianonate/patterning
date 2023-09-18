package patterning.actions

import patterning.state.RunningModeController
import processing.core.PApplet
import processing.event.KeyEvent

object KeyHandler {

    private val _pressedKeys: MutableSet<Int> = mutableSetOf()
    val pressedKeys: Set<Int>
        get() = _pressedKeys

    private var keyCallbacks: MutableMap<Set<KeyCombo>, KeyCallback> = mutableMapOf()
    private val keyEventObservers: MutableList<KeyEventObserver> = mutableListOf()

    var latestKeyCode: Int = 0
        get() = pressedKeys.last()
        private set

    private lateinit var pApplet: PApplet
    private var keyEvent: KeyEvent? = null

    fun addKeyObserver(observer: KeyEventObserver) {
        keyEventObservers.add(observer)
    }

    fun registerKeyHandler(pApplet: PApplet) {
        this.pApplet = pApplet.apply {
            registerMethod("pre", this@KeyHandler)
            registerMethod("keyEvent", this@KeyHandler)
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

    /**
     * in P2D and P3D, keyEvent will be called once per keyPress so we
     * capture the keyEvent so it can be used in the pre call repeatedly
     * most control key callbacks will just be called once, zooming and moving will be called
     * every draw
     */
    @Suppress("unused")
    fun keyEvent(event: KeyEvent) {

        if (!pApplet.keyPressed) {
            _pressedKeys.clear() // clear movement keys
        }

        val matchingCallback = keyCallbacks.values.find { it.matches(event) } ?: return

        when (event.action) {
            KeyEvent.PRESS -> handleInitialKeyPress(matchingCallback, event)
            KeyEvent.RELEASE -> handleKeyReleased(matchingCallback, event)
        }
    }

    private fun handleInitialKeyPress(callback: KeyCallback, event: KeyEvent) {
        _pressedKeys.add(event.keyCode)
        keyEvent = event

        // Handle one-time invocation here
        // repeated invocation callbacks are handled in "pre"
        if (!callback.invokeEveryDraw) {
            handleKeyPressed(callback, event)
        }
    }

    @Suppress("unused")
    fun pre() {

        val localKeyEvent = keyEvent ?: return
        val matchingCallback = keyCallbacks.values.find { it.matches(localKeyEvent) } ?: return

        if (pApplet.keyPressed) {
            // Handle repeated invocation here
            if (matchingCallback.invokeEveryDraw) {
                handleKeyPressed(matchingCallback, localKeyEvent)
            }
        }
    }

    private fun invokeFeature(callback: KeyCallback, event: KeyEvent) {
        if (RunningModeController.isUXAvailable) {

            // disallow feature invoking during testing
            // println("invoking feature - state:${RunningModeController.runningMode}")
            callback.invokeFeature()

            // Check if matchingCallback is KeyObservable - for callbacks that notify other parts of the system
            if (callback is ControlKeyEventObservable) {
                callback.notifyControlOnKeyPress(event)
            }
        }
    }

    private fun handleKeyPressed(callback: KeyCallback, event: KeyEvent) {

        invokeFeature(callback, event)

        // now let all the observers know - especially the UX which will handle
        // given key events can be disabled when isUXAvailable is false
        // we need to be sure to do this last as this is how the UX gets re-enabled
        // in the UX class where it will invoke RunningModeController.play()
        keyEventObservers.forEach { it.notifyGlobalKeyPress(event) }
    }

    private fun handleKeyReleased(callback: KeyCallback, event: KeyEvent) {
        val keyCode = event.keyCode
        _pressedKeys.remove(keyCode)
        latestKeyCode = keyCode

        if (callback is ControlKeyEventObservable && RunningModeController.isUXAvailable) {
            callback.notifyControlOnKeyRelease(event)
        }

        keyEventObservers.forEach { it.notifyGlobalKeyRelease(event) }

        keyEvent = null
    }

    val usageText: String
        get() {
            val maxKeysWidth = keyCallbacks.values
                .filter { it.isValidForCurrentOS }
                .maxOf { it.toString().length }

            return buildString {
                append("\nKey Usage:\n")
                append(
                    keyCallbacks.values
                        .filter { it.isValidForCurrentOS }
                        .sortedBy { it.usage }
                        .joinToString(separator = "\n") { callback ->
                            "${callback.toString().padEnd(maxKeysWidth + 1)}: ${callback.usage}"
                        }
                )
            }
        }


}