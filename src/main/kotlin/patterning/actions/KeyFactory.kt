package patterning.actions

import kotlinx.coroutines.runBlocking
import patterning.Patterning
import patterning.ux.DrawRateManager
import patterning.ux.PatternDrawer
import patterning.ux.Theme
import patterning.ux.ThemeType
import processing.core.PApplet
import processing.event.KeyEvent

class KeyFactory(private val patterning: Patterning, private val drawer: PatternDrawer) {
    private val processing: PApplet = patterning

    // todo: turn this into a get() for PatternDrawer
    //       it can be constructed in the init and returned as a val
    //       then you can move MovementHandler into KeyHandler
    //       and call handleMovementKeys on KeyHandler and pass it a lambda
    //       to call back on to do movement when there are movement keys in the mix
    //       first get PatternDrawer converted to Kotlin
    fun setupKeyHandler() {
        val keyHandler = KeyHandler.Builder(processing)
            .addKeyCallback(callbackPause)
            .addKeyCallback(callbackZoomIn)
            .addKeyCallback(callbackZoomInCenter)
            .addKeyCallback(callbackZoomOut)
            .addKeyCallback(callbackZoomOutCenter)
            .addKeyCallback(callbackStepFaster)
            .addKeyCallback(callbackStepSlower)
            .addKeyCallback(callbackDrawFaster)
            .addKeyCallback(callbackDrawSlower)
            .addKeyCallback(callbackDisplayBounds)
            .addKeyCallback(callbackCenterView)
            .addKeyCallback(callbackFitUniverseOnScreen)
            .addKeyCallback(callbackThemeToggle)
            .addKeyCallback(callbackRandomLife)
            .addKeyCallback(callbackSingleStep)
            .addKeyCallback(callbackRewind)
            .addKeyCallback(callbackPaste)
            .addKeyCallback(callbackUndoMovement)
            .addKeyCallback(callbackMovement)
            .addKeyCallback(callbackLoadLifeForm)
            .build()
        println(keyHandler.usageText)
    }

    private fun createKeyCallback(
        key: Char,
        invokeFeatureLambda: () -> Unit,
        getUsageTextLambda: () -> String,
        invokeModeChangeLambda: (() -> Boolean)? = null,
        cleanupFeatureLambda: (() -> Unit)? = null
    ): KeyCallback {
        return object : ExtendedKeyCallback(linkedSetOf(KeyCombo(key.code)), invokeFeatureLambda, getUsageTextLambda, invokeModeChangeLambda, cleanupFeatureLambda) {}
    }

    private fun createKeyCallback(
        keys: Set<Char>,
        invokeFeatureLambda: () -> Unit,
        getUsageTextLambda: () -> String,
        invokeModeChangeLambda: (() -> Boolean)? = null,
        cleanupFeatureLambda: (() -> Unit)? = null
    ): KeyCallback {
        val keyCombos = keys.mapTo(LinkedHashSet()) { KeyCombo(keyCode = it.code) }
        return object : ExtendedKeyCallback(keyCombos, invokeFeatureLambda, getUsageTextLambda, invokeModeChangeLambda, cleanupFeatureLambda) {}
    }

    private fun createKeyCallback(
        keyCombos: Collection<KeyCombo>,
        invokeFeatureLambda: () -> Unit,
        getUsageTextLambda: () -> String,
        invokeModeChangeLambda: (() -> Boolean)? = null,
        cleanupFeatureLambda: (() -> Unit)? = null
    ): KeyCallback {
        return object : ExtendedKeyCallback(LinkedHashSet(keyCombos), invokeFeatureLambda, getUsageTextLambda, invokeModeChangeLambda, cleanupFeatureLambda) {}
    }

    val callbackPause: KeyCallback = createKeyCallback(
        key = SHORTCUT_PAUSE,
        invokeFeatureLambda = {drawer.handlePause()},
        getUsageTextLambda = {"pause and play"}
    )
    private val callbackLoadLifeForm: KeyCallback = createKeyCallback(
        keys = setOf('1', '2', '3', '4', '5', '6', '7', '8', '9'),
        invokeFeatureLambda = {patterning.numberedLifeForm},
        getUsageTextLambda = {"press a # key to load one of the first 9 embedded RLE resource files"}
    )

   val callbackDrawSlower = createKeyCallback(
       keyCombos = setOf(KeyCombo(SHORTCUT_DRAW_SPEED, KeyEvent.SHIFT)),
       invokeFeatureLambda = { DrawRateManager.instance!!.goSlower() },
       getUsageTextLambda = { "slow the animation down" }
   )

    val callbackDrawFaster = createKeyCallback(
        key = SHORTCUT_DRAW_SPEED,
        invokeFeatureLambda = { DrawRateManager.instance!!.goFaster() },
        getUsageTextLambda = { "speed the animation up" }
    )

    val callbackStepFaster = createKeyCallback(
        key = SHORTCUT_STEP_FASTER,
        invokeFeatureLambda = { patterning.handleStep(true) },
        getUsageTextLambda = { "double the generations per draw" }
    )

    val callbackStepSlower = createKeyCallback(
        key = SHORTCUT_STEP_SLOWER,
        invokeFeatureLambda = { patterning.handleStep(false) },
        getUsageTextLambda = { "cut in half the generations per draw" }
    )

    val callbackRewind = createKeyCallback(
        key = SHORTCUT_REWIND,
        invokeFeatureLambda = { runBlocking { patterning.destroyAndCreate() } },
        getUsageTextLambda = { "rewind the current life form back to generation 0" }
    )

    val callbackRandomLife = createKeyCallback(
        key = SHORTCUT_RANDOM_FILE,
        invokeFeatureLambda = { runBlocking { patterning.getRandomLifeform(true) } },
        getUsageTextLambda = { "get a random life form from the built-in library" }
    )

    private val callbackZoomIn = createKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_IN.code), KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoomXY(true, patterning.mouseX.toFloat(), patterning.mouseY.toFloat()) },
        getUsageTextLambda = { "zoom in centered on the mouse" }
    )

    val callbackZoomInCenter = createKeyCallback(
        key = SHORTCUT_ZOOM_CENTERED,
        invokeFeatureLambda = { drawer.zoomXY(true, patterning.width.toFloat() / 2, patterning.height.toFloat() / 2) },
        getUsageTextLambda = { "zoom in centered on the middle of the screen" }
    )

    val callbackZoomOutCenter = createKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)),
        invokeFeatureLambda = { drawer.zoomXY(false, patterning.width.toFloat() / 2, patterning.height.toFloat() / 2) },
        getUsageTextLambda =  { "zoom out centered on the middle of the screen" }
    )

    val callbackZoomOut = createKeyCallback(
        key = SHORTCUT_ZOOM_OUT,
        invokeFeatureLambda = { drawer.zoomXY(false, patterning.mouseX.toFloat(), patterning.mouseY.toFloat()) },
        getUsageTextLambda = { "zoom out centered on the mouse" }
    )

    val callbackDisplayBounds = createKeyCallback(
        key = SHORTCUT_DISPLAY_BOUNDS,
        invokeFeatureLambda = { drawer.toggleDrawBounds() },
        getUsageTextLambda =  { "draw a border around the part of the universe containing living cells" }
    )

    val callbackCenterView = createKeyCallback(
        key = SHORTCUT_CENTER,
        invokeFeatureLambda = { patterning.centerView() },
        getUsageTextLambda = { "center the view on the universe - regardless of its size" }
    )

    val callbackUndoMovement = createKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC), KeyCombo(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)),
        invokeFeatureLambda = { drawer.undoMovement() },
        getUsageTextLambda = { "undo various movement patterning.actions such as centering or fitting to screen" }
    )

    val callbackFitUniverseOnScreen = createKeyCallback(
        key = SHORTCUT_FIT_UNIVERSE,
        invokeFeatureLambda = { patterning.fitUniverseOnScreen() },
        getUsageTextLambda = { "fit the visible universe on screen" }
    )

    val callbackThemeToggle = createKeyCallback(
        key = SHORTCUT_THEME_TOGGLE,
        invokeFeatureLambda = {
            if (Theme.currentThemeType == ThemeType.DEFAULT) Theme.setTheme(ThemeType.DARK)
            else Theme.setTheme(ThemeType.DEFAULT)
        },
        getUsageTextLambda = { "toggle between dark and light themes" }
    )

    private val callbackPaste = createKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC), KeyCombo(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)),
        invokeFeatureLambda = { patterning.pasteLifeForm() },
        getUsageTextLambda = { "paste a new lifeform into the app - currently only supports RLE encoded lifeforms" }
    )

    private val callbackMovement = createKeyCallback(
        keyCombos = setOf(MovementHandler.WEST, MovementHandler.EAST, MovementHandler.NORTH, MovementHandler.SOUTH).map { KeyCombo(it) }.toSet(),
        invokeFeatureLambda = {
            if (!KeyHandler.pressedKeys.any { it in listOf(MovementHandler.WEST, MovementHandler.EAST, MovementHandler.NORTH, MovementHandler.SOUTH) }) {
                drawer.saveUndoState()
            }
        },
        getUsageTextLambda = { "use arrow keys to move the image around. hold down two keys to move diagonally" },
        cleanupFeatureLambda = {
            if (KeyHandler.pressedKeys.isEmpty()) {
                DrawRateManager.instance!!.drawImmediately()
            }
        }
    )

    val callbackSingleStep = createKeyCallback(
        key = SHORTCUT_SINGLE_STEP,
        invokeFeatureLambda = {  patterning.toggleSingleStep() },
        getUsageTextLambda = { "in single step mode, advanced one frame at a time" },
        invokeModeChangeLambda = { true }
    )

    companion object {
        private const val SHORTCUT_CENTER = 'c'
        private const val SHORTCUT_DISPLAY_BOUNDS = 'b'
        private const val SHORTCUT_FIT_UNIVERSE = 'f'
        private const val SHORTCUT_PASTE = 'v'
        private const val SHORTCUT_PAUSE = ' '
        private const val SHORTCUT_RANDOM_FILE = 'r'
        private const val SHORTCUT_REWIND = 'w'
        private const val SHORTCUT_STEP_FASTER = ']'
        private const val SHORTCUT_STEP_SLOWER = '['
        private const val SHORTCUT_ZOOM_IN = '='
        private const val SHORTCUT_ZOOM_OUT = '-'
        private const val SHORTCUT_UNDO = 'z'
        private const val SHORTCUT_ZOOM_CENTERED = 'z'
        private const val SHORTCUT_DRAW_SPEED = 's'
        private const val SHORTCUT_THEME_TOGGLE = 'd'
        private const val SHORTCUT_SINGLE_STEP = 't'
    }
}