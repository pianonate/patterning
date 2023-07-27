package patterning.actions

import kotlinx.coroutines.runBlocking
import patterning.Processing
import patterning.RunningState
import patterning.ux.PatternDrawer
import patterning.ux.Theme
import patterning.ux.ThemeType
import processing.core.PApplet
import processing.event.KeyEvent

class KeyFactory(private val patterning: Processing, private val drawer: PatternDrawer) {

    private val processing: PApplet = patterning

    // todo: turn this into a get() for PatternDrawer
    //       it can be constructed in the init and returned as a val
    //       then you can move MovementHandler into KeyHandler
    //       and call handleMovementKeys on KeyHandler and pass it a lambda
    //       to call back on to do movement when there are movement keys in the mix
    fun setupKeyHandler() {
        KeyHandler.Builder(processing)
            .addKeyCallback(callbackPause)
            .addKeyCallback(callbackZoomIn)
            .addKeyCallback(callbackZoomInCenter)
            .addKeyCallback(callbackZoomOut)
            .addKeyCallback(callbackZoomOutCenter)
            .addKeyCallback(callbackStepFaster)
            .addKeyCallback(callbackStepSlower)
            /*            .addKeyCallback(callbackDrawFaster)
                        .addKeyCallback(callbackDrawSlower)*/
            .addKeyCallback(callbackDrawBounds)
            .addKeyCallback(callbackCenterView)
            .addKeyCallback(callbackFitUniverseOnScreen)
            .addKeyCallback(callbackThemeToggle)
            .addKeyCallback(callbackPerfTest)
            .addKeyCallback(callbackRandomLife)
            .addKeyCallback(callbackSingleStep)
            .addKeyCallback(callbackRewind)
            .addKeyCallback(callbackPaste)
            .addKeyCallback(callbackUndoMovement)
            .addKeyCallback(callbackMovement)
            .addKeyCallback(callbackLoadLifeForm)
            .build().also {
                println(it.usageText)
            }
    }

    // callbacks all constructed with factory methods to make them easy to read and write
    val callbackPause: KeyCallback = KeyCallback.createKeyCallback(
        key = SHORTCUT_PAUSE,
        invokeFeatureLambda = { drawer.handlePause() },
        getUsageTextLambda = { "pause and play" }
    )
    private val callbackLoadLifeForm: KeyCallback = KeyCallback.createKeyCallback(
        keys = setOf('1', '2', '3', '4', '5', '6', '7', '8', '9'),
        invokeFeatureLambda = { patterning.numberedLifeForm },
        getUsageTextLambda = { "press a # key to load one of the first 9 embedded RLE resource files" }
    )

    val callbackStepFaster = KeyCallback.createKeyCallback(
        key = SHORTCUT_STEP_FASTER,
        invokeFeatureLambda = { patterning.handleStep(true) },
        getUsageTextLambda = { "double the generations per draw" }
    )

    val callbackStepSlower = KeyCallback.createKeyCallback(
        key = SHORTCUT_STEP_SLOWER,
        invokeFeatureLambda = { patterning.handleStep(false) },
        getUsageTextLambda = { "cut in half the generations per draw" }
    )

    val callbackRewind = KeyCallback.createKeyCallback(
        key = SHORTCUT_REWIND,
        invokeFeatureLambda = { patterning.instantiateLifeform() },
        getUsageTextLambda = { "rewind the current life form back to generation 0" }
    )

    val callbackRandomLife = KeyCallback.createKeyCallback(
        key = SHORTCUT_RANDOM_FILE,
        invokeFeatureLambda = { runBlocking { patterning.getRandomLifeform(true) } },
        getUsageTextLambda = { "get a random life form from the built-in library" }
    )

    private val callbackZoomIn = KeyCallback.createKeyCallback(
        // we want it to handle both = and shift= (+) the same way
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_IN.code), KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoomXY(true, patterning.mouseX.toFloat(), patterning.mouseY.toFloat()) },
        getUsageTextLambda = { "zoom in centered on the mouse" }
    )

    val callbackZoomInCenter = KeyCallback.createKeyCallback(
        key = SHORTCUT_ZOOM_CENTERED,
        invokeFeatureLambda = { drawer.zoomXY(true, patterning.width.toFloat() / 2, patterning.height.toFloat() / 2) },
        getUsageTextLambda = { "zoom in centered on the middle of the screen" }
    )

    val callbackZoomOutCenter = KeyCallback.createKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoomXY(false, patterning.width.toFloat() / 2, patterning.height.toFloat() / 2) },
        getUsageTextLambda = { "zoom out centered on the middle of the screen" }
    )

    private val callbackZoomOut = KeyCallback.createKeyCallback(
        key = SHORTCUT_ZOOM_OUT,
        invokeFeatureLambda = { drawer.zoomXY(false, patterning.mouseX.toFloat(), patterning.mouseY.toFloat()) },
        getUsageTextLambda = { "zoom out centered on the mouse" }
    )

    val callbackDrawBounds = KeyCallback.createKeyCallback(
        key = SHORTCUT_DISPLAY_BOUNDS,
        invokeFeatureLambda = { drawer.toggleDrawBounds() },
        getUsageTextLambda = { "draw a border around the part of the universe containing living cells" }
    )

    val callbackCenterView = KeyCallback.createKeyCallback(
        key = SHORTCUT_CENTER,
        invokeFeatureLambda = { patterning.centerView() },
        getUsageTextLambda = { "center the view on the universe - regardless of its size" }
    )

    val callbackUndoMovement = KeyCallback.createKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = { drawer.undoMovement() },
        getUsageTextLambda = { "undo various movement patterning.actions such as centering or fitting to screen" }
    )

    val callbackFitUniverseOnScreen = KeyCallback.createKeyCallback(
        key = SHORTCUT_FIT_UNIVERSE,
        invokeFeatureLambda = { patterning.fitUniverseOnScreen() },
        getUsageTextLambda = { "fit the visible universe on screen" }
    )

    val callbackThemeToggle = KeyCallback.createKeyCallback(
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
    val callbackPerfTest = KeyCallback.createKeyCallback(
        key = SHORtCUT_PERFTEST,
        invokeFeatureLambda = {

        },
        getUsageTextLambda = { "run the performance test" }
    )

    private val callbackPaste = KeyCallback.createKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = { patterning.pasteLifeForm() },
        getUsageTextLambda = { "paste a new lifeform into the app - currently only supports RLE encoded lifeforms" }
    )

    private val callbackMovement = KeyCallback.createKeyCallback(
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

    val callbackSingleStep = KeyCallback.createKeyCallback(
        key = SHORTCUT_SINGLE_STEP,
        // invokeFeatureLambda = { patterning.toggleSingleStep() },
        invokeFeatureLambda = { RunningState.toggleSingleStep() },
        getUsageTextLambda = { "in single step mode, advanced one frame at a time" },
        invokeModeChangeLambda = { true }
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