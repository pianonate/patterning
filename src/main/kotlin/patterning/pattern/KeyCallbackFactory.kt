package patterning.pattern

import kotlinx.coroutines.runBlocking
import patterning.Canvas
import patterning.actions.KeyCombo
import patterning.actions.KeyEventNotifier
import patterning.actions.SimpleKeyCallback
import patterning.actions.ValidOS
import patterning.state.RunningModeController
import patterning.util.hudFormatted
import processing.core.PApplet
import processing.event.KeyEvent

class KeyCallbackFactory(
    private val pApplet: PApplet,
    private val pattern: Pattern,
    private val canvas: Canvas,
) {
    /**
     * these are provided just for readability when instantiating SimpleKeyCallback
     * if you come up with different ways that you want to create KeyCombos than these, just add another extension
     * right now these are all invoked from KeyCallbackFactory so they're marked private so as not to pollute the
     * namespace where they can't otherwise be used
     */
    private fun Char.toKeyComboSet(): LinkedHashSet<KeyCombo> = linkedSetOf(KeyCombo(this.code))

    private fun CharRange.toKeyComboSet(): LinkedHashSet<KeyCombo> =
        this.map { KeyCombo(it.code) }.toCollection(LinkedHashSet())

    private fun KeyCombo.toKeyComboSet(): LinkedHashSet<KeyCombo> = linkedSetOf(this)

    private fun Pair<KeyCombo, KeyCombo>.toKeyComboSet(): LinkedHashSet<KeyCombo> = linkedSetOf(first, second)

    private fun Set<Int>.toKeyComboSet(): LinkedHashSet<KeyCombo> = this.mapTo(LinkedHashSet()) { KeyCombo(it) }

    private val displayState = pattern.displayState

    fun setupSimpleKeyCallbacks() {
        with(KeyEventNotifier) {
            addKeyCallback(callbackCenterViewResetRotations)
            addKeyCallback(callbackGhostModeKeyFrame)
            addKeyCallback(callbackInvokeGC)
            addKeyCallback(callbackPaste)
            addKeyCallback(callbackPerfTest)
            addKeyCallback(callbackMovePattern)
            addKeyCallback(callbackNextScreen)
            addKeyCallback(callbackNumberedPattern)
            addKeyCallback(callbackPrintMemory)
            addKeyCallback(callbackZoomIn)
            addKeyCallback(callbackZoomOut)
        }
    }

    val callbackAlwaysRotate = SimpleKeyCallback(
        keyCombos = SHORTCUT_ALWAYS_ROTATE.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.AlwaysRotate) },
        usage = "always rotate - even if paused"
    )

    val callbackCenter = SimpleKeyCallback(
        keyCombos = SHORTCUT_CENTER.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).center()
            }
        },
        usage = "center the view"
    )

    val callbackCenterAndFit = SimpleKeyCallback(
        keyCombos = SHORTCUT_CENTER_AND_FIT.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).fitToScreen()
            }
        },
        usage = "fit the pattern to the screen"
    )

    private val callbackCenterViewResetRotations = SimpleKeyCallback(
        keyCombos = KeyCombo(SHORTCUT_CENTER, KeyEvent.SHIFT).toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is ThreeDimensional) {
                (pattern as ThreeDimensional).centerAndResetRotations()
            }
        },
        usage = "center the view and stop rotations"
    )

    val callbackColorful = SimpleKeyCallback(
        keyCombos = SHORTCUT_COLORFUL.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.Colorful) },
        usage = "ooo, ahhh - so pretty!"
    )

    val callbackDrawBoundary = SimpleKeyCallback(
        keyCombos = SHORTCUT_DISPLAY_BOUNDS.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.Boundary) },
        usage = "draw a border around the pattern",
    )

    val callbackDrawBoundaryOnly = SimpleKeyCallback(
        keyCombos = KeyCombo(SHORTCUT_DISPLAY_BOUNDS, KeyEvent.SHIFT).toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.BoundaryOnly) },
        usage = "draw a border around the pattern - exclude the pattern",
    )

    val callbackGhostMode = SimpleKeyCallback(
        keyCombos = SHORTCUT_GHOST_MODE.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.GhostMode) },
        usage = "ghost mode. Also try ${KeyCombo.META_KEY}${SHORTCUT_GHOST_MODE.uppercaseChar()} to stamp out a key frame while in ghost mode. Try me!"
    )

    private val callbackGhostModeKeyFrame = SimpleKeyCallback(
        keyCombos = KeyCombo(SHORTCUT_GHOST_MODE, KeyEvent.META).toKeyComboSet(),
        invokeFeatureLambda = { pattern.stampGhostModeKeyFrame() },
        usage = "ghost mode emit a key frame - while ghosting - try me!"
    )

    private val callbackInvokeGC = SimpleKeyCallback(
        keyCombos = KeyCombo(SHORTCUT_INVOKE_GC, KeyEvent.SHIFT).toKeyComboSet(),
        invokeFeatureLambda = { System.gc() },
        usage = "manually invoke gc - for debugging purposes only"
    )

    private val callbackMovePattern = SimpleKeyCallback(
        keyCombos = setOf(WEST, EAST, NORTH, SOUTH).toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                val movementKeys = KeyEventNotifier.pressedKeys.intersect(
                    setOf(
                        WEST,
                        EAST,
                        NORTH,
                        SOUTH
                    )
                )

                if (movementKeys.isNotEmpty()) {
                    handleMovementKeys(movementKeys)
                }
            }
        },
        usage = "move pattern with arrows - hold down two keys to move diagonally",
        invokeEveryDraw = true,
    )

    private val callbackNextScreen = SimpleKeyCallback(
        keyCombos = SHORTCUT_NEXT_SCREEN.toKeyComboSet(),
        invokeFeatureLambda = { canvas.nextScreen() },
        usage = "move the screen"
    )

    private val callbackNumberedPattern = SimpleKeyCallback(
        keyCombos = ('1'..'9').toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is NumberedPatternLoader) {
                val number = KeyEventNotifier.latestKeyCode - '0'.code
                (pattern as NumberedPatternLoader).setNumberedPattern(number)
            }
        },
        usage = "load one of the first 9 patterns by pressing one of the # keys"
    )

    private val callbackPaste = SimpleKeyCallback(
        keyCombos = Pair(
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ).toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Pasteable) {
                (pattern as Pasteable).paste()
            }
        },
        usage = "paste a new pattern into the app - currently only supports RLE encoded lifeforms"
    )

    private val callbackPerfTest = SimpleKeyCallback(
        keyCombos = Pair(
            KeyCombo(SHORTCUT_PERFTEST.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_PERFTEST.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ).toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is PerformanceTestable) {
                RunningModeController.test()
            }
        },
        usage = "performance test"
    )

    val callbackPlayPause = SimpleKeyCallback(
        keyCombos = SHORTCUT_PLAY_PAUSE.toKeyComboSet(),
        invokeFeatureLambda = { RunningModeController.togglePlayPause() },
        usage = "play and pause",
        invokeAfterDelay = true
    )

    private val callbackPrintMemory = SimpleKeyCallback(
        keyCombos = SHORTCUT_PRINT_MEMORY.toKeyComboSet(),
        invokeFeatureLambda = {
            println(
                "memory: ${
                    (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).hudFormatted()
                }"
            )
        },
        usage = "print out memory used to console"
    )

    val callbackRandomPattern = SimpleKeyCallback(
        keyCombos = KeyCombo(SHORTCUT_RANDOM_FILE, KeyEvent.META).toKeyComboSet(),
        invokeFeatureLambda = {
            runBlocking {
                if (pattern is NumberedPatternLoader) {
                    (pattern as NumberedPatternLoader).setRandom()
                }
            }
        },
        usage = "random pattern from from the built-in library"
    )

    val callbackRewind = SimpleKeyCallback(
        keyCombos = SHORTCUT_REWIND.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Rewindable) {
                (pattern as Rewindable).rewind()
            }
        },
        usage = "rewind the current life form back to generation 0"
    )

    val callbackSaveImage = SimpleKeyCallback(
        keyCombos = SHORTCUT_SAVE_IMAGE.toKeyComboSet(),
        invokeFeatureLambda = { pattern.saveImage() },
        usage = "screenshot to desktop!"
    )

    val callbackSingleStep = SimpleKeyCallback(
        keyCombos = SHORTCUT_SINGLE_STEP.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                RunningModeController.toggleSingleStep()
            }
        },
        usage = "toggle single step mode where which advances one generation at a time"
    )

    val callbackStepFaster = SimpleKeyCallback(
        keyCombos = SHORTCUT_STEP_FASTER.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                (pattern as Steppable).handleStep(true)
            }
        },
        usage = "step faster - double the generations each step"
    )

    val callbackStepSlower = SimpleKeyCallback(
        keyCombos = SHORTCUT_STEP_SLOWER.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                (pattern as Steppable).handleStep(false)
            }
        },
        usage = "step slower - halve the generations per step"
    )

    val callbackThemeToggle = SimpleKeyCallback(
        keyCombos = SHORTCUT_THEME_TOGGLE.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.DarkMode) },
        usage = "toggle between dark and light themes",
        invokeAfterDelay = true
    )

    val callbackThreeDBoxes = SimpleKeyCallback(
        keyCombos = KeyCombo(SHORTCUT_THREE_D_BOXES, KeyEvent.SHIFT).toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.ThreeDBoxes) },
        usage = "three dimensional mode - try me!"
    )

    val callbackThreeDYaw = SimpleKeyCallback(
        keyCombos = SHORTCUT_THREE_D_YAW.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.ThreeDYaw) },
        usage = "rotate on the y axis (yaw)"
    )

    val callbackThreeDPitch = SimpleKeyCallback(
        keyCombos = SHORTCUT_THREE_D_PITCH.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.ThreeDPitch) },
        usage = "rotate on the x axis (pitch)"
    )

    val callbackThreeDRoll = SimpleKeyCallback(
        keyCombos = SHORTCUT_THREE_D_ROLL.toKeyComboSet(),
        invokeFeatureLambda = { displayState.toggleState(DisplayMode.ThreeDRoll) },
        usage = "rotate on the z axis (roll)"
    )

    private val callbackZoomIn = SimpleKeyCallback(
        // we want it to handle both = and shift= (+) the same way
        keyCombos = Pair(KeyCombo(SHORTCUT_ZOOM_IN.code), KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)).toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).zoom(true, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat())
            }
        },
        usage = "zoom in centered on the mouse",
        invokeEveryDraw = true,
    )

    val callbackUndoMovement = SimpleKeyCallback(
        keyCombos = Pair(
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ).toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                canvas.undoMovement()
            }
        },
        usage = "undo  movements / actions such as centering or fitting to screen",
        invokeEveryDraw = true
    )

    private val callbackZoomOut = SimpleKeyCallback(
        keyCombos = SHORTCUT_ZOOM_OUT.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).zoom(false, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat())
            }
        },
        usage = "zoom out centered on the mouse",
        invokeEveryDraw = true,
    )

    val callbackZoomInCenter = SimpleKeyCallback(
        keyCombos = SHORTCUT_ZOOM_CENTERED.toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).zoom(true, pApplet.width.toFloat() / 2, pApplet.height.toFloat() / 2)
            }
        },
        usage = "zoom in centered on the middle of the screen",
        invokeEveryDraw = true,
    )

    val callbackZoomOutCenter = SimpleKeyCallback(
        keyCombos = KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT).toKeyComboSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).zoom(false, pApplet.width.toFloat() / 2, pApplet.height.toFloat() / 2)
            }
        },
        usage = "zoom out centered on the middle of the screen",
        invokeEveryDraw = true,
    )

    private fun handleMovementKeys(movementKeys: Set<Int>) {
        var moveX = 0f
        var moveY = 0f

        directions.forEach { direction ->
            if (movementKeys.contains(direction.key)) {
                moveX += direction.moveX * MOVE_AMOUNT / movementKeys.size
                moveY += direction.moveY * MOVE_AMOUNT / movementKeys.size
            }
        }

        pattern.move(moveX, moveY)
    }

    companion object {
        private const val SHORTCUT_THREE_D_BOXES = '3'
        private const val SHORTCUT_THREE_D_PITCH = 'p'
        private const val SHORTCUT_THREE_D_YAW = 'y'
        private const val SHORTCUT_THREE_D_ROLL = 'r'

        private const val SHORTCUT_CENTER = 'c'
        private const val SHORTCUT_DISPLAY_BOUNDS = 'b'
        private const val SHORTCUT_CENTER_AND_FIT = 'f'
        private const val SHORTCUT_GHOST_MODE = 'g'
        private const val SHORTCUT_ALWAYS_ROTATE = 'e'
        private const val SHORTCUT_INVOKE_GC = 'i'
        private const val SHORTCUT_PRINT_MEMORY = 'm'
        private const val SHORTCUT_NEXT_SCREEN = 'n'
        private const val SHORTCUT_PASTE = 'v'
        private const val SHORTCUT_PLAY_PAUSE = ' '
        private const val SHORTCUT_PERFTEST = 't'
        private const val SHORTCUT_COLORFUL = 'a'
        private const val SHORTCUT_RANDOM_FILE = 'r'
        private const val SHORTCUT_REWIND = 'w'
        private const val SHORTCUT_STEP_FASTER = '.'
        private const val SHORTCUT_STEP_SLOWER = ','
        private const val SHORTCUT_UNDO = 'z'

        private const val SHORTCUT_SAVE_IMAGE = 's'
        private const val SHORTCUT_THEME_TOGGLE = 'd'
        private const val SHORTCUT_SINGLE_STEP = 't'

        private const val SHORTCUT_ZOOM_IN = '='
        private const val SHORTCUT_ZOOM_CENTERED = 'z'
        private const val SHORTCUT_ZOOM_OUT = '-'

        private const val WEST = PApplet.LEFT
        private const val EAST = PApplet.RIGHT
        private const val NORTH = PApplet.UP
        private const val SOUTH = PApplet.DOWN

        private const val MOVE_AMOUNT = 5f

        data class Direction(val key: Int, val moveY: Float, val moveX: Float)

        val directions = arrayOf(
            Direction(WEST, 0f, -1f),
            Direction(NORTH, -1f, 0f),
            Direction(EAST, 0f, 1f),
            Direction(SOUTH, 1f, 0f),
            Direction(NORTH + WEST, -1f, -1f),
            Direction(NORTH + EAST, -1f, 1f),
            Direction(SOUTH + WEST, 1f, -1f),
            Direction(SOUTH + EAST, 1f, 1f)
        )
    }
}