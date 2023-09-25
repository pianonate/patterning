package patterning.pattern

import patterning.Canvas
import patterning.Properties
import patterning.Theme
import patterning.ThemeType
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

    init {
        registerObserver(PatternEventType.PatternSwapped) { _ -> resetOnNewPattern() }
    }

    override fun onStateChanged(changedOption: RenderingOption) {
        when (changedOption) {
            RenderingOption.DarkMode -> {
                Theme.setTheme(
                    when (Theme.currentThemeType) {
                        ThemeType.DEFAULT -> ThemeType.DARK
                        else -> ThemeType.DEFAULT
                    }
                )
            }
            else -> {}
        }
    }

    // currently these are the only methods called by PatterningPApplet so we
    // require all Patterns to implement them
    abstract fun draw()
    abstract fun getHUDMessage(): String
    abstract fun handlePlayPause()
    abstract fun loadPattern()
    abstract fun move(dx: Float, dy: Float)
    abstract fun saveImage()
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

    fun onResetRotations() {
        notifyObservers(PatternEventType.ResetRotations, PatternEvent.ResetRotations(reset = true))
    }

    fun onNewPattern(patternName: String) {
        notifyObservers(PatternEventType.PatternSwapped, PatternEvent.PatternSwapped(patternName))
    }

    fun toggleGhost() {
        ghostState.transition()
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