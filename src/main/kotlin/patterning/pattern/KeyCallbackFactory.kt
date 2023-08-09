package patterning.pattern

import kotlinx.coroutines.runBlocking
import patterning.RunningState
import patterning.Theme
import patterning.ThemeType
import patterning.actions.KeyCombo
import patterning.actions.KeyHandler
import patterning.actions.MovementHandler
import patterning.actions.SimpleKeyCallback
import patterning.actions.ValidOS
import processing.core.PApplet
import processing.event.KeyEvent

class KeyCallbackFactory(private val pApplet: PApplet, private val pattern: Pattern) {
    
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
        invokeFeatureLambda = {
            if (pattern is Playable) {
                (pattern as Playable).handlePlay()
            }
        },
        usage = "play and pause"
    )
    
    private val callbackNumberedPattern = SimpleKeyCallback(
        keys = ('1'..'9').toSet(),
        invokeFeatureLambda = {
            if (pattern is NumberedPatternLoader) {
                val number = KeyHandler.latestKeyCode - '0'.code
                (pattern as NumberedPatternLoader).setNumberedPattern(number)
            }
        },
        usage = "load one of the first 9 patterns by pressing one of the # keys"
    )
    
    val callbackStepFaster = SimpleKeyCallback(
        key = SHORTCUT_STEP_FASTER,
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                (pattern as Steppable).handleStep(true)
            }
        },
        usage = "step faster - double the generations each step"
    )
    
    val callbackStepSlower = SimpleKeyCallback(
        key = SHORTCUT_STEP_SLOWER,
        invokeFeatureLambda = {
            if (pattern is Steppable) {
                (pattern as Steppable).handleStep(false)
            }
        },
        usage = "step slower - halve the generations per step"
    )
    
    val callbackRewind = SimpleKeyCallback(
        key = SHORTCUT_REWIND,
        invokeFeatureLambda = {
            if (pattern is Rewindable) {
                (pattern as Rewindable).rewind()
            }
        },
        usage = "rewind the current life form back to generation 0"
    )
    
    val callbackRandomPattern = SimpleKeyCallback(
        key = SHORTCUT_RANDOM_FILE,
        invokeFeatureLambda = {
            runBlocking {
                if (pattern is NumberedPatternLoader) {
                    (pattern as NumberedPatternLoader).setRandom()
                }
            }
        },
        usage = "random pattern from from the built-in library"
    )
    
    private val callbackZoomIn = SimpleKeyCallback(
        // we want it to handle both = and shift= (+) the same way
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_IN.code), KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)),
        invokeFeatureLambda = {
            if (pattern is Zoomable) {
                (pattern as Zoomable).zoom(true, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat())
            }
        },
        usage = "zoom in centered on the mouse"
    )
    
    val callbackZoomInCenter = SimpleKeyCallback(
        key = SHORTCUT_ZOOM_CENTERED,
        invokeFeatureLambda = {
            if (pattern is Zoomable) {
                (pattern as Zoomable).zoom(true, pApplet.width.toFloat() / 2, pApplet.height.toFloat() / 2)
            }
        },
        usage = "zoom in centered on the middle of the screen"
    )
    
    val callbackZoomOutCenter = SimpleKeyCallback(
        keyCombos = setOf(KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)),
        invokeFeatureLambda = {
            if (pattern is Zoomable) {
                (pattern as Zoomable).zoom(false, pApplet.width.toFloat() / 2, pApplet.height.toFloat() / 2)
            }
        },
        usage = "zoom out centered on the middle of the screen"
    )
    
    private val callbackZoomOut = SimpleKeyCallback(
        key = SHORTCUT_ZOOM_OUT,
        invokeFeatureLambda = {
            if (pattern is Zoomable) {
                (pattern as Zoomable).zoom(false, pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat())
            }
        },
        usage = "zoom out centered on the mouse"
    )
    
    val callbackDrawBounds = SimpleKeyCallback(
        key = SHORTCUT_DISPLAY_BOUNDS,
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).toggleDrawBounds()
            }
        },
        usage = "border drawn around the part of the universe containing living cells"
    )
    
    val callbackCenterView = SimpleKeyCallback(
        key = SHORTCUT_CENTER,
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).center()
            }
        },
        usage = "center the view"
    )
    
    val callbackUndoMovement = SimpleKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_UNDO.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).undoMovement()
            }
        },
        usage = "undo  movements / actions such as centering or fitting to screen"
    )
    
    val callbackFitUniverseOnScreen = SimpleKeyCallback(
        key = SHORTCUT_FIT_UNIVERSE,
        invokeFeatureLambda = {
            if (pattern is Movable) {
                (pattern as Movable).fitToScreen()
            }
        },
        usage = "fit the pattern to the screen"
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
            KeyCombo(SHORTCUT_PERFTEST.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_PERFTEST.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = {
            if (pattern is PerformanceTestable) {
                RunningState.test()
            }
        },
        usage = "performance test"
    )
    
    private val callbackPaste = SimpleKeyCallback(
        keyCombos = setOf(
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.META, ValidOS.MAC),
            KeyCombo(SHORTCUT_PASTE.code, KeyEvent.CTRL, ValidOS.NON_MAC)
        ),
        invokeFeatureLambda = {
            if (pattern is Pasteable) {
                (pattern as Pasteable).paste()
            }
        },
        usage = "paste a new pattern into the app - currently only supports RLE encoded lifeforms"
    )
    
    private val callbackMovement = SimpleKeyCallback(
        keyCombos = setOf(
            MovementHandler.WEST,
            MovementHandler.EAST,
            MovementHandler.NORTH,
            MovementHandler.SOUTH
        ).map { KeyCombo(it) }.toSet(),
        invokeFeatureLambda = {
            if (pattern is Movable) {
                if (!KeyHandler.pressedKeys.any {
                        it in listOf(
                            MovementHandler.WEST,
                            MovementHandler.EAST,
                            MovementHandler.NORTH,
                            MovementHandler.SOUTH
                        )
                    }) {
                    (pattern as Movable).saveUndoState()
                }
            }
        },
        usage = "move pattern with arrow. hold down two keys to move diagonally"
    )
    
    val callbackSingleStep = SimpleKeyCallback(
        key = SHORTCUT_SINGLE_STEP,
        invokeFeatureLambda = {
            if (pattern is Playable) {
                RunningState.toggleSingleStep()
            }
        },
        usage = "toggle single step mode where which advances one generation at a time"
    )
    
    companion object {
        private const val SHORTCUT_CENTER = 'c'
        private const val SHORTCUT_DISPLAY_BOUNDS = 'b'
        private const val SHORTCUT_FIT_UNIVERSE = 'f'
        private const val SHORTCUT_PASTE = 'v'
        private const val SHORTCUT_PAUSE = ' '
        private const val SHORTCUT_PERFTEST = 't'
        private const val SHORTCUT_RANDOM_FILE = 'r'
        private const val SHORTCUT_REWIND = 'w'
        private const val SHORTCUT_STEP_FASTER = ']'
        private const val SHORTCUT_STEP_SLOWER = '['
        private const val SHORTCUT_UNDO = 'z'
        
        // private const val SHORTCUT_DRAW_SPEED = 's'
        private const val SHORTCUT_THEME_TOGGLE = 'd'
        private const val SHORTCUT_SINGLE_STEP = 't'
        
        const val SHORTCUT_ZOOM_IN = '='
        const val SHORTCUT_ZOOM_CENTERED = 'z'
        const val SHORTCUT_ZOOM_OUT = '-'
    }
}