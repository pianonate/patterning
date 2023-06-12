package actions

import kotlinx.coroutines.runBlocking
import patterning.Patterning
import processing.core.PApplet
import processing.event.KeyEvent
import ux.DrawRateManager
import ux.PatternDrawer
import ux.Theme
import ux.ThemeType

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

    val callbackPause: KeyCallback = object : KeyCallback(SHORTCUT_PAUSE) {
        override fun invokeFeature() {
            // the encapsulation is messy to ask the drawer to stop displaying countdown text
            // and just continue running, or toggle the running state...
            // but CountdownText already reaches back to patterning.Patterning.run()
            // so there aren't that many complex paths to deal with here...
            drawer.handlePause()
        }

        override fun getUsageText(): String {
            return "pause and play"
        }
    }
    private val callbackLoadLifeForm: KeyCallback = object : KeyCallback(
        LinkedHashSet(mutableListOf('1', '2', '3', '4', '5', '6', '7', '8', '9'))
    ) {
        override fun invokeFeature() {
            patterning.numberedLifeForm
        }

        override fun getUsageText(): String {
            return "press a # key to load one of the first 9 embedded RLE resource files"
        }
    }
    val callbackDrawSlower: KeyCallback = object : KeyCallback(
        KeyCombo(SHORTCUT_DRAW_SPEED, KeyEvent.SHIFT)
    ) {
        override fun invokeFeature() {
            DrawRateManager.instance!!.goSlower()
        }

        override fun getUsageText(): String {
            return "slow the animation down"
        }
    }
    val callbackDrawFaster: KeyCallback = object : KeyCallback(SHORTCUT_DRAW_SPEED) {
        override fun invokeFeature() {
            DrawRateManager.instance!!.goFaster()
        }

        override fun getUsageText(): String {
            return "speed the animation up"
        }
    }
    val callbackStepFaster: KeyCallback = object : KeyCallback(SHORTCUT_STEP_FASTER) {
        override fun invokeFeature() {
            patterning.handleStep(true)
        }

        override fun getUsageText(): String {
            return "double the generations per draw"
        }
    }
    val callbackStepSlower: KeyCallback = object : KeyCallback(SHORTCUT_STEP_SLOWER) {
        override fun invokeFeature() {
            patterning.handleStep(false)
        }

        override fun getUsageText(): String {
            return "cut in half the generations per draw"
        }
    }
    val callbackRewind: KeyCallback = object : KeyCallback(SHORTCUT_REWIND) {
        override fun invokeFeature() {
            runBlocking { patterning.destroyAndCreate() }
        }

        override fun getUsageText(): String {
            return "rewind the current life form back to generation 0"
        }
    }
    val callbackRandomLife: KeyCallback = object : KeyCallback(SHORTCUT_RANDOM_FILE) {
        override fun invokeFeature() {
            runBlocking { patterning.getRandomLifeform(true) }
        }

        override fun getUsageText(): String {
            return "get a random life form from the built-in library"
        }
    }
    private val callbackZoomIn: KeyCallback = object : KeyCallback(
        KeyCombo(SHORTCUT_ZOOM_IN.code),
        KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)
    ) {
        override fun invokeFeature() {
            drawer.zoomXY(true, patterning.mouseX.toFloat(), patterning.mouseY.toFloat())
        }

        override fun getUsageText(): String {
            return "zoom in centered on the mouse"
        }
    }

    val callbackZoomInCenter: KeyCallback = object : KeyCallback(SHORTCUT_ZOOM_CENTERED) {
        override fun invokeFeature() {
            // todo - why do we have to call back to patterning
            //patterning.centerView();
            drawer.zoomXY(true, patterning.width.toFloat() / 2, patterning.height.toFloat() / 2)
        }

        override fun getUsageText(): String {
            return "zoom in centered on the middle of the screen"
        }
    }

    val callbackZoomOutCenter: KeyCallback = object : KeyCallback(
        KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)
    ) {
        override fun invokeFeature() {
            //patterning.centerView();
            drawer.zoomXY(false, patterning.width.toFloat() / 2, patterning.height.toFloat() / 2)
        }

        override fun getUsageText(): String {
            return "zoom out centered on the middle of the screen"
        }
    }
    private val callbackZoomOut: KeyCallback = object : KeyCallback(SHORTCUT_ZOOM_OUT) {
        override fun invokeFeature() {
            drawer.zoomXY(false, patterning.mouseX.toFloat(), patterning.mouseY.toFloat())
        }

        override fun getUsageText(): String {
            return "zoom out centered on the mouse"
        }
    }

    val callbackDisplayBounds: KeyCallback = object : KeyCallback(SHORTCUT_DISPLAY_BOUNDS) {
        override fun invokeFeature() {
            drawer.toggleDrawBounds()
        }

        override fun getUsageText(): String {
            return "draw a border around the part of the universe containing living cells"
        }
    }

    val callbackCenterView: KeyCallback = object : KeyCallback(SHORTCUT_CENTER) {
        override fun invokeFeature() {
            patterning.centerView()
        }

        override fun getUsageText(): String {
            return "center the view on the universe - regardless of its size"
        }
    }

    val callbackUndoMovement: KeyCallback = object : KeyCallback(
        KeyCombo(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC),
        KeyCombo(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)
    ) {
        override fun invokeFeature() {
            drawer.undoMovement()
        }

        override fun getUsageText(): String {
            return "undo various movement actions such as centering or fitting to screen"
        }
    }
    val callbackFitUniverseOnScreen: KeyCallback = object : KeyCallback(SHORTCUT_FIT_UNIVERSE) {
        override fun invokeFeature() {
            patterning.fitUniverseOnScreen()
        }

        override fun getUsageText(): String {
            return "fit the visible universe on screen"
        }
    }
    val callbackThemeToggle: KeyCallback = object : KeyCallback(SHORTCUT_THEME_TOGGLE) {
        private var toggled = true
        override fun invokeFeature() {
            if (toggled) Theme.setTheme(ThemeType.DEFAULT) else Theme.setTheme(ThemeType.DARK)
            toggled = !toggled
        }

        override fun getUsageText(): String {
            return "toggle between dark and light themes"
        }
    }
    private val callbackPaste: KeyCallback = object : KeyCallback(
        KeyCombo(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC),
        KeyCombo(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)
    ) {
        override fun invokeFeature() {
            patterning.pasteLifeForm()
        }

        override fun getUsageText(): String {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms"
        }
    }
    private val callbackMovement: KeyCallback = object : KeyCallback(
        setOf(
            MovementHandler.WEST,
            MovementHandler.EAST,
            MovementHandler.NORTH,
            MovementHandler.SOUTH)
            .map { keyCode -> KeyCombo(keyCode) }
            .toCollection(LinkedHashSet())) {
        private var pressed = false
        override fun invokeFeature() {
            if (!pressed) {
                pressed = true
                // we only want to save the undo state for key presses when we start them
                // no need to save again until they're all released
                drawer.saveUndoState()
            }
        }

        override fun cleanupFeature() {
            if (KeyHandler.pressedKeys.isEmpty()) {
                pressed = false
                DrawRateManager.instance!!.drawImmediately()
            }
        }

        override fun getUsageText(): String {
            return "use arrow keys to move the image around. hold down two keys to move diagonally"
        }
    }
    val callbackSingleStep: KeyCallback = object : KeyCallback(SHORTCUT_SINGLE_STEP) {
        override fun invokeFeature() {
            patterning.toggleSingleStep()
        }

        override fun invokeModeChange(): Boolean {
            return true
        }

        override fun getUsageText(): String {
            return "in single step mode, advanced one frame at a time"
        }
    }

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

abstract class ExtendedKeyCallback(
    keys: LinkedHashSet<KeyCombo>
) : KeyCallback(keys) {
    abstract override fun invokeFeature()
    abstract override fun getUsageText(): String
}