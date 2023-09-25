package patterning.actions

import patterning.state.RunningModeController
import processing.core.PApplet
import processing.event.KeyEvent

object KeyHandler {

    private val _pressedKeys: MutableSet<Int> = mutableSetOf()
    val pressedKeys: Set<Int>
        get() = _pressedKeys

    private var keyCallbacks: MutableMap<Set<KeyCombo>, KeyCallback> = mutableMapOf()
    private val keyCallbackObservers: MutableList<KeyCallbackObserver> = mutableListOf()
    private val lastInvokeTime: MutableMap<KeyCallback, Long> = mutableMapOf()

    var latestKeyCode: Int = 0
        get() = pressedKeys.last()
        private set

    private lateinit var pApplet: PApplet
    private var keyEvent: KeyEvent? = null

    fun addKeyCallbackObserver(observer: KeyCallbackObserver) {
        keyCallbackObservers.add(observer)
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
     * KeyHandler is an object whose primary reason for existence is just to isolate keyboard shortcuts
     * from the main PatterningPApplet class
     *
     * as such, because of Processing's settings/setup initialization process, we provide registerKeyHandler
     * to allow for instantiating and providing method hooks for keyEvent and pre
     *
     * with the P2D and P3D renderers, keyEvent is called once per keypress but sometimes we want behavior
     * to occur while a key is being held down so we also register the "pre" method which is called before
     * each invocation of draw() so that any continually pressed keys can be handled there
     *
     * it feels a bit of a workaround just to separate out the code so documenting it clearly here
     */
    fun registerKeyHandler(pApplet: PApplet) {
        this.pApplet = pApplet.apply {
            registerMethod("keyEvent", this@KeyHandler)
            registerMethod("pre", this@KeyHandler)
        }
    }


    /**
     * in P2D and P3D, keyEvent will be called once per keyPress so we
     * capture the keyEvent so it can be used in the "pre" call repeatedly during draw()
     * most control key callbacks will just be called once, zooming, moving, undo and possibly
     * others may be called every draw
     *
     * suppressing unused because keyEvent is not explicitly invoked - we have to register it with
     * processing via a call to registerMethod
     */
    @Suppress("unused")
    fun keyEvent(event: KeyEvent) {

        if (!pApplet.keyPressed) {
            _pressedKeys.clear() // clear movement keys
        }

        val matchingCallback = keyCallbacks.values.find { it.matches(event) } ?: return

        when (event.action) {
            KeyEvent.PRESS -> handleInitialKeyPress(matchingCallback, event)
            KeyEvent.RELEASE -> handleKeyReleased(event)
        }
    }

    /**
     * "pre" will be called once per draw - if there is a currently active keyEvent then
     * do something with it
     *
     * suppressing unused because pre is not explicitly invoked - we have to register it with
     * processing via a call to registerMethod
     */
    @Suppress("unused")
    fun pre() {

        val localKeyEvent = keyEvent ?: return

        if (pApplet.keyPressed) {

            val matchingCallback = keyCallbacks.values.find { it.matches(localKeyEvent) } ?: return

            // Handle repeated invocation here
            if (matchingCallback.invokeEveryDraw || matchingCallback.invokeAfterDelay) {
                handleKeyPressed(matchingCallback, localKeyEvent)
            }
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

    private fun handleKeyPressed(callback: KeyCallback, event: KeyEvent) {
        val currentTime = System.currentTimeMillis()

        if (callback.invokeAfterDelay) {
            val lastTime = lastInvokeTime[callback] ?: 0L
            if (currentTime - lastTime < 200) {  // 500 ms delay
                return
            }
        }

        invokeFeature(callback, event)

        lastInvokeTime[callback] = currentTime

        // now let all the observers know - especially the UX which will handle
        // given key events can be disabled when isUXAvailable is false
        // we need to be sure to do this last as this is how the UX gets re-enabled
        // in the UX class where it will invoke RunningModeController.play()
        keyCallbackObservers.forEach { it.notifyGlobalKeyPress(event) }
    }

    private fun invokeFeature(callback: KeyCallback, event: KeyEvent) {
        if (RunningModeController.isUXAvailable) {

            // disallow feature invoking during testing
            // println("invoking feature - state:${RunningModeController.runningMode}")
            callback.invokeFeature()

            // Check if matchingCallback is KeyObservable - for callbacks that notify other parts of the system
            if (callback is ControlKeyCallbackObservable) {
                callback.notifyControlOnKeyPress(event)
            }
        }
    }

    private fun handleKeyReleased( event: KeyEvent) {
        val keyCode = event.keyCode
        _pressedKeys.remove(keyCode)
        latestKeyCode = keyCode
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