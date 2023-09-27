package patterning.pattern

import patterning.Canvas
import patterning.Properties
import patterning.Theme
import patterning.ThemeType
import patterning.ThreeD
import patterning.state.RunningModeController
import patterning.util.applyAlpha
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PGraphics

abstract class Pattern(
    val pApplet: PApplet,
    val canvas: Canvas,
    val properties: Properties,
    val displayState: DisplayState,
) : ObservablePattern, DisplayState.Observer {

    protected interface GhostState {
        fun prepareGraphics(graphics: PGraphics)
        fun transition()
        fun applyAlpha(color: Int): Int
    }

    private val observers: MutableMap<PatternEventType, MutableList<(PatternEvent) -> Unit>> = mutableMapOf()
    private val accumulatedGraphics =
        canvas.getNamedGraphicsReference(Theme.GHOST_ACCUMULATED_GRAPHICS, useOpenGL = true).graphics

    private val patternGraphics = canvas.getNamedGraphicsReference(Theme.PATTERN_GRAPHICS).graphics

    protected var ghostState: GhostState = GhostOff()
    protected var threeD = ThreeD(canvas, displayState)

    init {
        registerObserver(PatternEventType.PatternSwapped) { _ -> resetOnNewPattern() }
  /*      accumulatedGraphics.beginDraw()
        accumulatedGraphics.background(Theme.backgroundColor)
        accumulatedGraphics.endDraw()
        fadeShader.set("targetColor", 0.0f,0.0f, 0.0f)
        fadeShader.set("alphaModifier", 0.05f)*/
    }

    override fun onStateChanged(changedOption: Behavior) {
        when (changedOption) {
            Behavior.DarkMode -> updateTheme()
            Behavior.GhostMode -> ghostState.transition()
            else -> {}
        }
    }

    private fun updateTheme() {
        Theme.setTheme(
            when (Theme.currentThemeType) {
                ThemeType.DEFAULT -> {
                    /*fadeShader.set("targetColor", 0f, 0f, 0f, 1f)
                    fadeShader.set("alphaModifier", 0.01f)
                    accumulatedGraphics.beginDraw()
                    accumulatedGraphics.background(0f,0f,0f,0f)
                    accumulatedGraphics.endDraw()*/
                    ThemeType.DARK
                }

                else -> {
                /*    fadeShader.set("targetColor", 1.0f,1.0f, 1.0f, 1.0f)
                    fadeShader.set("alphaModifier", -0.05f)
                    accumulatedGraphics.beginDraw()
                    accumulatedGraphics.background(255f,255f,255f,255f)
                    accumulatedGraphics.endDraw()*/
                    ThemeType.DEFAULT
                }
            }
        )


    }

    // requirements of a pattern
    abstract fun drawPattern(shouldAdvancePattern: Boolean)
    abstract fun getHUDMessage(): String
    abstract fun loadPattern()
    abstract fun updateProperties()

    final override fun registerObserver(eventType: PatternEventType, observer: (PatternEvent) -> Unit) {
        observers.getOrPut(eventType) { mutableListOf() }.add(observer)
    }

    final override fun notifyObservers(eventType: PatternEventType, event: PatternEvent) {
        observers[eventType]?.forEach { it(event) }
    }

    private fun resetOnNewPattern() {
        ghostState = GhostOff()
        with(displayState) {
            disable(Behavior.AlwaysRotate)
            disable(Behavior.GhostMode)
            disable(Behavior.GhostFadeAwayMode)
            disable(Behavior.ThreeDBoxes)
            disable(Behavior.ThreeDYaw)
            disable(Behavior.ThreeDPitch)
            disable(Behavior.ThreeDRoll)
        }
    }

    fun onNewPattern(patternName: String) {
        notifyObservers(PatternEventType.PatternSwapped, PatternEvent.PatternSwapped(patternName))
    }

    fun stampGhostModeKeyFrame() {
        ghostState = GhostKeyFrame()
    }

    fun move(dx: Float, dy: Float) {
        canvas.saveUndoState()
        canvas.moveCanvasOffsets(dx, dy)
    }

    fun draw() {
        with(pApplet) {
            background(Theme.backgroundColor)

            val shouldAdvancePattern = RunningModeController.shouldAdvancePattern()
            drawPattern(shouldAdvancePattern)

            val x = 0f
            val y = 0f

            if (displayState expects Behavior.FadeAway) {
                accumulatedGraphics.beginDraw()
                accumulatedGraphics.blendMode(PConstants.BLEND )
                accumulatedGraphics.fill(Theme.backgroundColor, 35f)
                accumulatedGraphics.rect(0f,0f,width.toFloat(), height.toFloat())
                accumulatedGraphics.image(patternGraphics, x, y)
                accumulatedGraphics.endDraw()
                image(accumulatedGraphics, x, y)


            } else {
                accumulatedGraphics.beginDraw()
                accumulatedGraphics.clear()
                accumulatedGraphics.endDraw()
                image(patternGraphics, x, y)
            }
        }
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