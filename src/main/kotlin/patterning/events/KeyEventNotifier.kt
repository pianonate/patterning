package patterning.events

import patterning.pattern.Command
import patterning.state.RunningModeController
import processing.core.PApplet
import processing.event.KeyEvent

object KeyEventNotifier {

    private val _pressedKeys: MutableSet<Int> = mutableSetOf()
    val pressedKeys: Set<Int>
        get() = _pressedKeys

    private var behaviors: MutableMap<Set<KeyboardShortcut>, Command> = mutableMapOf()

    private val globalKeyEventObservers: MutableList<KeyEventObserver> = mutableListOf()
    private val controlKeyEventObservers = mutableMapOf<Command, MutableList<KeyEventObserver>>()

    private val lastInvokeTime: MutableMap<Command, Long> = mutableMapOf()

    var latestKeyCode: Int = 0
        get() = pressedKeys.last()
        private set

    private lateinit var pApplet: PApplet
    private var keyEvent: KeyEvent? = null

    /**
     * right now the UX is the only thing listening to any key press
     */
    fun addGlobalCommandObserver(observer: KeyEventObserver) {
        globalKeyEventObservers.add(observer)
    }

    /**
     * controls register themselves to be mapped to a specific keyCallback this way
     */
    fun addControlCommandObserver(controlCommand: Command, observer: KeyEventObserver) {
        addCommand(controlCommand)
        controlKeyEventObservers.getOrPut(controlCommand) { mutableListOf() }.add(observer)
    }

    /**
     * whether it is a keyCallback associated with a control or an unattached keyCallback
     * (simple keyboard shortcut) - they all have to be added to the keyCallbacks list
     * so they can be matched at runtime and then invoked as appropriate
     */
    fun addCommand(simpleCommand: Command) {
        val keyCombos = simpleCommand.keyboardShortcuts
        val duplicateKeyCombos = behaviors.keys.flatten().intersect(keyCombos)
        require(duplicateKeyCombos.isEmpty()) {
            "The following key combos are already associated with another callback: ${
                duplicateKeyCombos.joinToString(
                    ", "
                )
            }"
        }
        behaviors[keyCombos] = simpleCommand
    }

    /**
     * KeyEventNotifier is an object whose primary reason for existence is just to isolate keyboard shortcuts
     * from the main PatterningPApplet class
     *
     * as such, because of Processing's settings/setup initialization process, we provide registerPAppletEventHandlers
     * to allow for instantiating and providing method hooks for keyEvent and pre
     *
     * with the P2D and P3D renderers, keyEvent is called once per keypress but sometimes we want behavior
     * to occur while a key is being held down so we also register the "pre" method which is called before
     * each invocation of draw() so that any continually pressed keys can be handled there
     *
     * it feels a bit of a workaround just to separate out the code so documenting it clearly here
     */
    fun registerPAppletEventHandlers(pApplet: PApplet) {
        this.pApplet = pApplet.apply {
            registerMethod("keyEvent", this@KeyEventNotifier)
            registerMethod("pre", this@KeyEventNotifier)
        }
    }

    /**
     * when using P2D and P3D renderers, keyEvent will be called once per keyPress so we
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

        val matchingCallback = behaviors.values.find { it.matches(event) } ?: return

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

            val matchingCallback = behaviors.values.find { it.matches(localKeyEvent) } ?: return

            // Handle repeated invocation here
            if (matchingCallback.invokeEveryDraw || matchingCallback.invokeAfterDelay) {
                handleKeyPressed(matchingCallback, localKeyEvent)
            }
        }
    }

    private fun handleInitialKeyPress(command: Command, event: KeyEvent) {
        _pressedKeys.add(event.keyCode)
        keyEvent = event

        // Handle one-time invocation here
        // repeated invocation callbacks are handled in "pre"
        if (!command.invokeEveryDraw) {
            handleKeyPressed(command, event)
        }
    }

    private fun handleKeyPressed(callback: Command, event: KeyEvent) {
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
        globalKeyEventObservers.forEach { it.onKeyPressed(event) }
    }

    private fun invokeFeature(callback: Command, event: KeyEvent) {
        if (RunningModeController.isUXAvailable) {

            // disallow feature invoking during testing
            // println("invoking feature - state:${RunningModeController.runningMode}")
            callback.invokeFeature()

            // Check if the callback is associated with a control
            // then it will have a list of control(s) that it notifies
            controlKeyEventObservers[callback]?.forEach {it.onKeyPressed(event)}
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
            val maxKeysWidth = behaviors.values
                .filter { it.isValidForCurrentOS }
                .maxOf { it.toString().length }

            return buildString {
                append("\nKey Usage:\n")
                append(
                    behaviors.values
                        .filter { it.isValidForCurrentOS }
                        .sortedBy { it.usage }
                        .joinToString(separator = "\n") { callback ->
                            "${callback.toString().padEnd(maxKeysWidth + 1)}: ${callback.usage}"
                        }
                )
            }
        }
}