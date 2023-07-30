package patterning.actions

import kotlinx.coroutines.runBlocking
import patterning.RunningState
import patterning.Theme
import patterning.ThemeType
import patterning.life.LifePattern
import processing.core.PApplet
import processing.event.KeyEvent

class KeyFactory(private val pApplet: PApplet, private val drawer: LifePattern) {

    fun setupSimpleKeyCallbacks() {
        with(KeyHandler) {
            addKeyCallback(callbackPerfTest)
            addKeyCallback(callbackPaste)
            addKeyCallback(callbackNumberedPattern)
            addKeyCallback(callbackMovement)
            addKeyCallback(callbackZoomIn)
            addKeyCallback(callbackZoomOut)
        }
    }

    // callbacks all constructed with factory methods to make them easy to read and write
    val callbackPause = SimpleKeyCallback(
        key = SHORTCUT_PAUSE,
        invokeFeatureLambda = { drawer.handlePlay() },
        usage = "play and pause"
    )
    private val callbackNumberedPattern = SimpleKeyCallback(
        keys = setOf('1', '2', '3', '4', '5', '6', '7', '8', '9'),
        invokeFeatureLambda = {
            val number = KeyHandler.latestKeyCode - '0'.code
            drawer.setNumberedLifeForm(number)
        },
        usage = "load one of the first 9 life forms by pressing one of the # keys"
    )

    val callbackStepFaster = SimpleKeyCallback(
        key = SHORTCUT_STEP_FASTER,
        invokeFeatureLambda = { drawer.handleStep(true) },
        usage = "step faster - double the generations each step"
    )

    val callbackStepSlower = SimpleKeyCallback(
        key = SHORTCUT_STEP_SLOWER,
        invokeFeatureLambda = { drawer.handleStep(false) },
        usage = "step slower - halve the generations per step"
    )

    val callbackRewind = SimpleKeyCallback(
        key = SHORTCUT_REWIND,
        invokeFeatureLambda = { drawer.rewind() },
        usage = "rewind the current life form back to generation 0"
    )

    val callbackRandomPattern = SimpleKeyCallback(
        key = SHORTCUT_RANDOM_FILE,
        invokeFeatureLambda = {
            runBlocking {
                drawer.getRandomLifeform()
                drawer.instantiateLifeform()
            }
        },
        usage = "random life form from the built-in library"
    )

    private val callbackZoomIn = SimpleKeyCallback(
        // we want it to handle both = and shift= (+) the same way
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_IN.code), KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoom(true, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat()) },
        usage = "zoom in centered on the mouse"
    )

    val callbackZoomInCenter = SimpleKeyCallback(
        key = SHORTCUT_ZOOM_CENTERED,
        invokeFeatureLambda = { drawer.zoom(true, pApplet.width.toFloat() / 2, pApplet.height.toFloat() / 2) },
        usage = "zoom in centered on the middle of the screen"
    )

    val callbackZoomOutCenter = SimpleKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoom(false, pApplet.width.toFloat() / 2, pApplet.height.toFloat() / 2) },
        usage = "zoom out centered on the middle of the screen"
    )

    private val callbackZoomOut = SimpleKeyCallback(
        key = SHORTCUT_ZOOM_OUT,
        invokeFeatureLambda = { drawer.zoom(false, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat()) },
        usage = "zoom out centered on the mouse"
    )

    val callbackDrawBounds = SimpleKeyCallback(
        key = SHORTCUT_DISPLAY_BOUNDS,
        invokeFeatureLambda = { drawer.toggleDrawBounds() },
        usage = "border drawn around the part of the universe containing living cells"
    )

    val callbackCenterView = SimpleKeyCallback(
        key = SHORTCUT_CENTER,
        invokeFeatureLambda = { drawer.centerView() },
        usage = "center the view on the universe - regardless of its size"
    )

    val callbackUndoMovement = SimpleKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = { drawer.undoMovement() },
        usage = "undo  movements / actions such as centering or fitting to screen"
    )

    val callbackFitUniverseOnScreen = SimpleKeyCallback(
        key = SHORTCUT_FIT_UNIVERSE,
        invokeFeatureLambda = { drawer.fitUniverseOnScreen() },
        usage = "fit the visible universe on screen"
    )

    val callbackThemeToggle = SimpleKeyCallback(
        key = SHORTCUT_THEME_TOGGLE,
        invokeFeatureLambda = {
            Theme.setTheme(
                when (Theme.currentThemeType) {
                    ThemeType.DEFAULT -> ThemeType.DARK
                    else -> ThemeType.DEFAULT
                }
            )
        },
        usage = "toggle between dark and light themes"
    )
    val callbackPerfTest = SimpleKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORtCUT_PERFTEST.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORtCUT_PERFTEST.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = {
            drawer.runPerformanceTest()
        },
        usage = "performance test"
    )

    private val callbackPaste = SimpleKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = { drawer.pasteLifeForm() },
        usage = "paste a new lifeform into the app - currently only supports RLE encoded lifeforms"
    )

    private val callbackMovement = SimpleKeyCallback(
        keyCombos = setOf(
            MovementHandler.WEST,
            MovementHandler.EAST,
            MovementHandler.NORTH,
            MovementHandler.SOUTH
        ).map { KeyCombo(it) }.toSet(),
        invokeFeatureLambda = {
            if (!KeyHandler.pressedKeys.any {
                    it in listOf(
                        MovementHandler.WEST,
                        MovementHandler.EAST,
                        MovementHandler.NORTH,
                        MovementHandler.SOUTH
                    )
                }) {
                drawer.saveUndoState()
            }
        },
        usage = "move pattern with arrow. hold down two keys to move diagonally"
    )

    val callbackSingleStep = SimpleKeyCallback(
        key = SHORTCUT_SINGLE_STEP,
        invokeFeatureLambda = { RunningState.toggleRunnningMode() },
        usage = "toggle single step mode where play advances one step at a time"
    )

    companion object {
        private const val SHORTCUT_CENTER = 'c'
        private const val SHORTCUT_DISPLAY_BOUNDS = 'b'
        private const val SHORTCUT_FIT_UNIVERSE = 'f'
        private const val SHORTCUT_PASTE = 'v'
        private const val SHORTCUT_PAUSE = ' '
        private const val SHORtCUT_PERFTEST = 't'
        private const val SHORTCUT_RANDOM_FILE = 'r'
        private const val SHORTCUT_REWIND = 'w'
        private const val SHORTCUT_STEP_FASTER = ']'
        private const val SHORTCUT_STEP_SLOWER = '['
        private const val SHORTCUT_ZOOM_IN = '='
        private const val SHORTCUT_ZOOM_OUT = '-'
        private const val SHORTCUT_UNDO = 'z'
        private const val SHORTCUT_ZOOM_CENTERED = 'z'

        // private const val SHORTCUT_DRAW_SPEED = 's'
        private const val SHORTCUT_THEME_TOGGLE = 'd'
        private const val SHORTCUT_SINGLE_STEP = 't'
    }
}