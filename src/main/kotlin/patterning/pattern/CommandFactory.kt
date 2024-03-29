package patterning.pattern

import kotlinx.coroutines.runBlocking
import patterning.Canvas
import patterning.events.KeyEventNotifier
import patterning.events.KeyboardShortcut
import patterning.events.ValidOS
import patterning.state.RunningModeController
import patterning.util.hudFormatted
import processing.core.PApplet
import processing.core.PVector
import processing.event.KeyEvent

class CommandFactory(
    private val pApplet: PApplet,
    private val pattern: Pattern,
    private val canvas: Canvas,
) {
    /**
     * these are provided just for readability when instantiating Simplecommand
     * if you come up with different ways that you want to create KeyCombos than these, just add another extension
     * right now these are all invoked from commandFactory so they're marked private so as not to pollute the
     * namespace where they can't otherwise be used
     *
     * these are all private and i don't know if they would cause clashes with .toSet() that exists in the
     * kotlin library - i really don't think so but even if so, these are scoped to this class only
     */
    private fun Char.toSet(): LinkedHashSet<KeyboardShortcut> = linkedSetOf(KeyboardShortcut(this.code))

    private fun CharRange.toSet(): LinkedHashSet<KeyboardShortcut> =
        this.map { KeyboardShortcut(it.code) }.toCollection(LinkedHashSet())

    private fun KeyboardShortcut.toSet(): LinkedHashSet<KeyboardShortcut> = linkedSetOf(this)

    private fun Pair<KeyboardShortcut, KeyboardShortcut>.toSet(): LinkedHashSet<KeyboardShortcut> =
        linkedSetOf(first, second)

    private fun Set<Int>.toSet(): LinkedHashSet<KeyboardShortcut> = this.mapTo(LinkedHashSet()) { KeyboardShortcut(it) }

    private val visuals = pattern.visuals

    fun setupSimpleCommands() {
        with(KeyEventNotifier) {
            addCommand(commandCenterViewResetRotations)
            addCommand(commandGhostModeKeyFrame)
            addCommand(commandGhostFadeAwayMode)
            addCommand(commandInvokeGC)
            addCommand(commandPaste)
            addCommand(commandPerfTest)
            addCommand(commandMovePattern)
            addCommand(commandNextScreen)
            addCommand(commandNumberedPattern)
            addCommand(commandPrintMemory)
            addCommand(commandZoomIn)
            addCommand(commandZoomOut)
        }
    }

    val commandAlwaysRotate = SimpleCommand(
        keyboardShortcuts = SHORTCUT_ALWAYS_ROTATE.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.AlwaysRotate) },
        isEnabledLambda = { visuals requires Visual.AlwaysRotate },
        usage = "always rotate - even if paused"
    )

    val commandCenter = SimpleCommand(
        keyboardShortcuts = SHORTCUT_CENTER.toSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).center()
            }
        },
        usage = "center the view"
    )

    val commandCenterAndFit = SimpleCommand(
        keyboardShortcuts = SHORTCUT_CENTER_AND_FIT.toSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).fitToScreen()
            }
        },
        usage = "fit the pattern to the screen"
    )

    private val commandCenterViewResetRotations = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_CENTER, KeyEvent.SHIFT).toSet(),
        invokeFeatureLambda = {
            if (pattern is ThreeDimensional) {
                (pattern as ThreeDimensional).centerAndResetRotations()
            }
        },
        usage = "center the view and stop rotations"
    )

    val commandColorful = SimpleCommand(
        keyboardShortcuts = SHORTCUT_COLORFUL.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.Colorful) },
        isEnabledLambda = { visuals requires Visual.Colorful },
        usage = "ooo, ahhh - so pretty!"
    )

    val commandDrawBoundary = SimpleCommand(
        keyboardShortcuts = SHORTCUT_DRAW_BOUNDARY.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.Boundary) },
        isEnabledLambda = { visuals requires Visual.Boundary },
        usage = "draw a border around the pattern",
    )
    val commandDrawSlower = SimpleCommand(
        keyboardShortcuts = SHORTCUT_DRAW_SLOWER.toSet(),
        invokeFeatureLambda = { pattern.drawSlower() },
        usage = "draw less frequently",
    )
    val commandDrawFaster = SimpleCommand(
        keyboardShortcuts = SHORTCUT_DRAW_FASTER.toSet(),
        invokeFeatureLambda = { pattern.drawFaster() },
        usage = "draw more frequently",
    )

    val commandFadeAway = SimpleCommand(
        keyboardShortcuts = SHORTCUT_FADE_AWAY.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.FadeAway) },
        isEnabledLambda = { visuals requires Visual.FadeAway },
        usage = "fade away the pattern - aka Joshua mode"
    )

    val commandGhostMode = SimpleCommand(
        keyboardShortcuts = SHORTCUT_GHOST_MODE.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.GhostMode) },
        isEnabledLambda = { visuals requires Visual.GhostMode },
        usage = "ghost mode. Also try ${KeyboardShortcut.META_KEY}${SHORTCUT_GHOST_MODE.uppercaseChar()} to stamp out a key frame while in ghost mode. Try me!"
    )

    private val commandGhostFadeAwayMode = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_GHOST_MODE, KeyEvent.SHIFT).toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.GhostFadeAwayMode) },
        isEnabledLambda = { visuals requires Visual.GhostFadeAwayMode },
        usage = "ghost fade away mode - hidden capability"
    )

    private val commandGhostModeKeyFrame = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_GHOST_MODE, KeyEvent.META).toSet(),
        invokeFeatureLambda = { pattern.stampGhostModeKeyFrame() },
        usage = "ghost mode emit a key frame - while ghosting - try me!"
    )

    val commandHidePattern = SimpleCommand(
        keyboardShortcuts = SHORTCUT_HIDE_PATTERN.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.HidePattern) },
        isEnabledLambda = { visuals requires Visual.HidePattern },
        usage = "hide the pattern",
    )

    private val commandInvokeGC = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_INVOKE_GC, KeyEvent.SHIFT).toSet(),
        invokeFeatureLambda = {
            System.gc()
            println("manual gc")
        },
        usage = "manually invoke gc - for debugging purposes only"
    )

    private val commandMovePattern = SimpleCommand(
        keyboardShortcuts = setOf(WEST, EAST, NORTH, SOUTH).toSet(),
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

    private val commandNextScreen = SimpleCommand(
        keyboardShortcuts = SHORTCUT_NEXT_SCREEN.toSet(),
        invokeFeatureLambda = { canvas.nextScreen() },
        usage = "move the screen"
    )

    private val commandNumberedPattern = SimpleCommand(
        keyboardShortcuts = ('1'..'9').toSet(),
        invokeFeatureLambda = {
            if (pattern is NumberedPatternLoader) {
                val number = KeyEventNotifier.latestKeyCode - '0'.code
                (pattern as NumberedPatternLoader).setNumberedPattern(number)
            }
        },
        usage = "load one of the first 9 patterns by pressing one of the # keys"
    )

    private val commandPaste = SimpleCommand(
        keyboardShortcuts = Pair(
            KeyboardShortcut(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC),
            KeyboardShortcut(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ).toSet(),
        invokeFeatureLambda = {
            if (pattern is Pasteable) {
                (pattern as Pasteable).paste()
            }
        },
        usage = "paste a new pattern into the app - currently only supports RLE encoded lifeforms"
    )

    private val commandPerfTest = SimpleCommand(
        keyboardShortcuts = Pair(
            KeyboardShortcut(SHORTCUT_PERFTEST.code, KeyEvent.META, ValidOS.MAC),
            KeyboardShortcut(SHORTCUT_PERFTEST.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ).toSet(),
        invokeFeatureLambda = {
            if (pattern is PerformanceTestable) {
                RunningModeController.test()
            }
        },
        usage = "performance test"
    )

    val commandPlayPause = SimpleCommand(
        keyboardShortcuts = SHORTCUT_PLAY_PAUSE.toSet(),
        invokeFeatureLambda = { RunningModeController.togglePlayPause() },
        usage = "play and pause",
        invokeAfterDelay = true
    )

    private val commandPrintMemory = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_PRINT_MEMORY, KeyEvent.SHIFT).toSet(),
        invokeFeatureLambda = {
            println(
                "memory: ${
                    (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).hudFormatted()
                }"
            )
        },
        usage = "print out memory used to console"
    )

    val commandRandomPattern = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_RANDOM_FILE, KeyEvent.META).toSet(),
        invokeFeatureLambda = {
            runBlocking {
                if (pattern is NumberedPatternLoader) {
                    (pattern as NumberedPatternLoader).setRandom()
                }
            }
        },
        usage = "random pattern from from the built-in library"
    )

    val commandRewind = SimpleCommand(
        keyboardShortcuts = SHORTCUT_REWIND.toSet(),
        invokeFeatureLambda = {
            if (pattern is Rewindable) {
                (pattern as Rewindable).rewind()
            }
        },
        usage = "rewind the current life form back to generation 0"
    )

    val commandSaveImage = SimpleCommand(
        keyboardShortcuts = SHORTCUT_SAVE_IMAGE.toSet(),
        invokeFeatureLambda = { pattern.saveImage() },
        usage = "screenshot to desktop!"
    )

    val commandSingleStep = SimpleCommand(
        keyboardShortcuts = SHORTCUT_SINGLE_STEP.toSet(),
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                RunningModeController.toggleSingleStep()
            }
        },
        isEnabledLambda = { RunningModeController.isSingleStep },
        usage = "toggle single step mode where which advances one generation at a time"
    )

    val commandStepFaster = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_STEP_FASTER, KeyEvent.SHIFT).toSet(),
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                (pattern as Steppable).handleStep(true)
            }
        },
        usage = "step faster - double the generations each step"
    )

    val commandStepSlower = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_STEP_SLOWER, KeyEvent.SHIFT).toSet(),
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                (pattern as Steppable).handleStep(false)
            }
        },
        usage = "step slower - halve the generations per step"
    )

    val commandThemeToggle = SimpleCommand(
        keyboardShortcuts = SHORTCUT_THEME_TOGGLE.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.DarkMode) },
        isEnabledLambda = { visuals requires Visual.DarkMode },
        usage = "toggle between dark and light themes",
        invokeAfterDelay = true
    )

    val commandThreeDBoxes = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_THREE_D_BOXES, KeyEvent.SHIFT).toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.ThreeDBoxes) },
        isEnabledLambda = { visuals requires Visual.ThreeDBoxes },
        usage = "three dimensional mode - try me!"
    )

    val commandThreeDMousePosition = SimpleCommand(
        keyboardShortcuts = SHORTCUT_THREE_D_MOUSE_POSITION.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.ThreeDMousePosition) },
        isEnabledLambda = { visuals requires Visual.ThreeDMousePosition },
        usage = "show the mouse where it is, where it would be translated by rotation, and also where the 'unrotated' position would be. connected by lines. use with ghost mode, rainbow mode and josh mode for creative effects. only really works when you're rotating the image"
    )

    val commandThreeDYaw = SimpleCommand(
        keyboardShortcuts = SHORTCUT_THREE_D_YAW.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.ThreeDYaw) },
        isEnabledLambda = { visuals requires Visual.ThreeDYaw },
        usage = "rotate on the y axis (yaw)"
    )

    val commandThreeDPitch = SimpleCommand(
        keyboardShortcuts = SHORTCUT_THREE_D_PITCH.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.ThreeDPitch) },
        isEnabledLambda = { visuals requires Visual.ThreeDPitch },
        usage = "rotate on the x axis (pitch)"
    )

    val commandThreeDRoll = SimpleCommand(
        keyboardShortcuts = SHORTCUT_THREE_D_ROLL.toSet(),
        invokeFeatureLambda = { visuals.toggleState(Visual.ThreeDRoll) },
        isEnabledLambda = { visuals requires Visual.ThreeDRoll },
        usage = "rotate on the z axis (roll)"
    )

    val commandUndoMovement = SimpleCommand(
        keyboardShortcuts = Pair(
            KeyboardShortcut(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC),
            KeyboardShortcut(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ).toSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                canvas.undoMovement()
            }
        },
        usage = "undo  movements / actions such as centering or fitting to screen",
        invokeEveryDraw = true
    )

    private val commandZoomIn = SimpleCommand(
        // we want it to handle both = and shift= (+) the same way
        keyboardShortcuts = Pair(
            KeyboardShortcut(SHORTCUT_ZOOM_IN.code),
            KeyboardShortcut(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)
        ).toSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).zoom(true, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat())
            }
        },
        usage = "zoom in centered on the mouse",
        invokeEveryDraw = true,
    )

    private val commandZoomOut = SimpleCommand(
        keyboardShortcuts = SHORTCUT_ZOOM_OUT.toSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).zoom(false, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat())
            }
        },
        usage = "zoom out centered on the mouse",
        invokeEveryDraw = true,
    )

    val commandZoomInCenter = SimpleCommand(
        keyboardShortcuts = SHORTCUT_ZOOM_CENTERED.toSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).zoom(true, pApplet.width.toFloat() / 2, pApplet.height.toFloat() / 2)
            }
        },
        usage = "zoom in centered on the middle of the screen",
        invokeEveryDraw = true,
    )

    val commandZoomOutCenter = SimpleCommand(
        keyboardShortcuts = KeyboardShortcut(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT).toSet(),
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

        pattern.move(PVector(moveX, moveY))
    }

    companion object {
        private const val SHORTCUT_THREE_D_BOXES = '3'
        private const val SHORTCUT_THREE_D_MOUSE_POSITION = 'm'
        private const val SHORTCUT_THREE_D_PITCH = 'p'
        private const val SHORTCUT_THREE_D_YAW = 'y'
        private const val SHORTCUT_THREE_D_ROLL = 'r'

        private const val SHORTCUT_CENTER = 'c'
        private const val SHORTCUT_DRAW_BOUNDARY = 'b'
        private const val SHORTCUT_DRAW_SLOWER = ','
        private const val SHORTCUT_DRAW_FASTER = '.'

        private const val SHORTCUT_CENTER_AND_FIT = 'f'
        private const val SHORTCUT_FADE_AWAY = 'j'
        private const val SHORTCUT_GHOST_MODE = 'g'
        private const val SHORTCUT_ALWAYS_ROTATE = 'e'
        private const val SHORTCUT_HIDE_PATTERN = 'h'
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