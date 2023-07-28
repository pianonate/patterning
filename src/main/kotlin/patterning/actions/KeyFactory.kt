package patterning.actions

import kotlinx.coroutines.runBlocking
import patterning.Processing
import patterning.RunningState
import patterning.ux.PatternDrawer
import patterning.ux.Theme
import patterning.ux.ThemeType
import processing.event.KeyEvent

class KeyFactory(private val processing: Processing, private val drawer: PatternDrawer) {

    fun setupSimpleKeyCallbacks() {
        with(KeyHandler) {
            addKeyCallback(callbackPerfTest)
            addKeyCallback(callbackPaste)
            addKeyCallback(callbackLoadLifeForm)
        }
    }

    // callbacks all constructed with factory methods to make them easy to read and write
    val callbackPause = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_PAUSE,
        invokeFeatureLambda = { drawer.handlePause() },
        getUsageTextLambda = { "pause and play" }
    )
    private val callbackLoadLifeForm = SimpleKeyCallback.createKeyCallback(
        keys = setOf('1', '2', '3', '4', '5', '6', '7', '8', '9'),
        invokeFeatureLambda = { processing.numberedLifeForm },
        getUsageTextLambda = { "press a # key to load one of the first 9 embedded RLE resource files" }
    )

    val callbackStepFaster = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_STEP_FASTER,
        invokeFeatureLambda = { processing.handleStep(true) },
        getUsageTextLambda = { "double the generations per draw" }
    )

    val callbackStepSlower = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_STEP_SLOWER,
        invokeFeatureLambda = { processing.handleStep(false) },
        getUsageTextLambda = { "cut in half the generations per draw" }
    )

    val callbackRewind = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_REWIND,
        invokeFeatureLambda = { processing.instantiateLifeform() },
        getUsageTextLambda = { "rewind the current life form back to generation 0" }
    )

    val callbackRandomLife = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_RANDOM_FILE,
        invokeFeatureLambda = { runBlocking { processing.getRandomLifeform(true) } },
        getUsageTextLambda = { "get a random life form from the built-in library" }
    )

    private val callbackZoomIn = SimpleKeyCallback.createKeyCallback(
        // we want it to handle both = and shift= (+) the same way
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_IN.code), KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoomXY(true, processing.mouseX.toFloat(), processing.mouseY.toFloat()) },
        getUsageTextLambda = { "zoom in centered on the mouse" }
    )

    val callbackZoomInCenter = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_ZOOM_CENTERED,
        invokeFeatureLambda = { drawer.zoomXY(true, processing.width.toFloat() / 2, processing.height.toFloat() / 2) },
        getUsageTextLambda = { "zoom in centered on the middle of the screen" }
    )

    val callbackZoomOutCenter = SimpleKeyCallback.createKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoomXY(false, processing.width.toFloat() / 2, processing.height.toFloat() / 2) },
        getUsageTextLambda = { "zoom out centered on the middle of the screen" }
    )

    private val callbackZoomOut = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_ZOOM_OUT,
        invokeFeatureLambda = { drawer.zoomXY(false, processing.mouseX.toFloat(), processing.mouseY.toFloat()) },
        getUsageTextLambda = { "zoom out centered on the mouse" }
    )

    val callbackDrawBounds = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_DISPLAY_BOUNDS,
        invokeFeatureLambda = { drawer.toggleDrawBounds() },
        getUsageTextLambda = { "draw a border around the part of the universe containing living cells" }
    )

    val callbackCenterView = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_CENTER,
        invokeFeatureLambda = { processing.centerView() },
        getUsageTextLambda = { "center the view on the universe - regardless of its size" }
    )

    val callbackUndoMovement = SimpleKeyCallback.createKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = { drawer.undoMovement() },
        getUsageTextLambda = { "undo various movement patterning.actions such as centering or fitting to screen" }
    )

    val callbackFitUniverseOnScreen = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_FIT_UNIVERSE,
        invokeFeatureLambda = { processing.fitUniverseOnScreen() },
        getUsageTextLambda = { "fit the visible universe on screen" }
    )

    val callbackThemeToggle = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_THEME_TOGGLE,
        invokeFeatureLambda = {
            Theme.setTheme(
                when (Theme.currentThemeType) {
                    ThemeType.DEFAULT -> ThemeType.DARK
                    else -> ThemeType.DEFAULT
                }
            )
        },
        getUsageTextLambda = { "toggle between dark and light themes" }
    )
    val callbackPerfTest = SimpleKeyCallback.createKeyCallback(
        key = SHORtCUT_PERFTEST,
        invokeFeatureLambda = {

        },
        getUsageTextLambda = { "run the performance test" }
    )

    private val callbackPaste = SimpleKeyCallback.createKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = { processing.pasteLifeForm() },
        getUsageTextLambda = { "paste a new lifeform into the app - currently only supports RLE encoded lifeforms" }
    )

    private val callbackMovement = SimpleKeyCallback.createKeyCallback(
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
        getUsageTextLambda = { "use arrow keys to move the image around. hold down two keys to move diagonally" },
    )

    val callbackSingleStep = SimpleKeyCallback.createKeyCallback(
        key = SHORTCUT_SINGLE_STEP,
        invokeFeatureLambda = { RunningState.toggleRunnningMode() },
        getUsageTextLambda = { "in single step mode, advanced one frame at a time" },
    )

    companion object {
        private const val SHORTCUT_CENTER = 'c'
        private const val SHORTCUT_DISPLAY_BOUNDS = 'b'
        private const val SHORTCUT_FIT_UNIVERSE = 'f'
        private const val SHORTCUT_PASTE = 'v'
        private const val SHORTCUT_PAUSE = ' '
        private const val SHORtCUT_PERFTEST = 'p'
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