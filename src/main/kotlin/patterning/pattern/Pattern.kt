package patterning.pattern

import patterning.Canvas
import patterning.Properties
import patterning.Theme
import patterning.ThemeType
import patterning.ThreeD
import patterning.util.applyAlpha
import processing.core.PApplet
import processing.core.PGraphics

abstract class Pattern(
    val pApplet: PApplet,
    val canvas: Canvas,
    val properties: Properties,
    val displayState: DisplayState,
) : ObservablePattern, DisplayState.Observer {

    private val observers: MutableMap<PatternEventType, MutableList<(PatternEvent) -> Unit>> = mutableMapOf()

    protected interface GhostState {
        fun prepareGraphics(graphics: PGraphics)
        fun transition()
        fun applyAlpha(color: Int): Int
    }

    protected var ghostState: GhostState = GhostOff()
    protected var threeD = ThreeD(canvas, displayState)

    init {
        registerObserver(PatternEventType.PatternSwapped) { _ -> resetOnNewPattern() }
    }

    override fun onStateChanged(changedOption: DisplayMode) {
        when (changedOption) {
            DisplayMode.DarkMode -> {
                Theme.setTheme(
                    when (Theme.currentThemeType) {
                        ThemeType.DEFAULT -> ThemeType.DARK
                        else -> ThemeType.DEFAULT
                    }
                )
            }

            DisplayMode.GhostMode -> ghostState.transition()

            else -> {}
        }
    }

    // currently these are the only methods called by PatterningPApplet so we
    // require all Patterns to implement them
    abstract fun draw()
    abstract fun getHUDMessage(): String
    abstract fun loadPattern()
    abstract fun move(dx: Float, dy: Float)
    abstract fun updateProperties()

    final override fun registerObserver(eventType: PatternEventType, observer: (PatternEvent) -> Unit) {
        observers.getOrPut(eventType) { mutableListOf() }.add(observer)
    }

    final override fun notifyObservers(eventType: PatternEventType, event: PatternEvent) {
        observers[eventType]?.forEach { it(event) }
    }

    private fun resetOnNewPattern() {
        ghostState = GhostOff()
    }

    internal fun onResetRotations() {
        notifyObservers(PatternEventType.ResetRotations, PatternEvent.ResetRotations(reset = true))
    }

    fun onNewPattern(patternName: String) {
        notifyObservers(PatternEventType.PatternSwapped, PatternEvent.PatternSwapped(patternName))
    }

    fun stampGhostModeKeyFrame() {
        ghostState = GhostKeyFrame()
    }

    /**
     * in preparation for future features such as drawing as inverse rainbow and gridlines
     */
    fun drawBackground() {
        pApplet.background(Theme.backgroundColor)
    }

    /**
     * okay - this is hacked in for now so you can at least et something out of it but ou really need to pop the
     * system dialog on non-mobile devices.  mobile - probably sharing
     */
    fun saveImage() {

        val newGraphics = pApplet.createGraphics(pApplet.width, pApplet.height)
        newGraphics.beginDraw()
        newGraphics.background(Theme.backgroundColor)
        val img = canvas.getNamedGraphicsReference(Theme.PATTERN_GRAPHICS).graphics.get()
        newGraphics.image(img, 0f, 0f)
        newGraphics.endDraw()

        val desktopDirectory = System.getProperty("user.home") + "/Desktop/"
        newGraphics.save("$desktopDirectory${pApplet.frameCount}.png")
    }

    protected inner class GhostOff : GhostState {
        override fun prepareGraphics(graphics: PGraphics) = graphics.clear()
        override fun transition() = run { ghostState = Ghosting() }
        override fun applyAlpha(color: Int): Int = color
    }

    protected inner class GhostKeyFrame : GhostState {
        private var emitted = false

        override fun prepareGraphics(graphics: PGraphics) {
            if (emitted) {
                transition()
                return
            }
            emitted = true
        }

        override fun transition() = run { ghostState = Ghosting(clearFirstFrame = false) }
        override fun applyAlpha(color: Int): Int = color
    }


    protected inner class Ghosting(private var clearFirstFrame: Boolean = true) : GhostState {
        private var firstFrame = true

        override fun prepareGraphics(graphics: PGraphics) {
            if (firstFrame && clearFirstFrame) {
                graphics.clear()
                firstFrame = false
            }
        }

        override fun transition() = run { ghostState = GhostOff() }
        override fun applyAlpha(color: Int): Int = color.applyAlpha(Theme.ghostAlpha)
    }
}