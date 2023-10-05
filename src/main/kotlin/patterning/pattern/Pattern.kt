package patterning.pattern

import patterning.Canvas
import patterning.Properties
import patterning.Theme
import patterning.ThemeType
import patterning.ThreeD
import patterning.state.RunningModeController
import patterning.util.applyAlpha
import patterning.util.drawPixelLine
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PVector

abstract class Pattern(
    val pApplet: PApplet,
    val canvas: Canvas,
    val properties: Properties,
    val visuals: VisualsManager,
    val threeD: ThreeD,
) : ObservablePattern, VisualsManager.Observer {

    protected interface GhostState {
        fun prepareGraphics(graphics: PGraphics)
        fun transition()
        fun applyAlpha(color: Int): Int
    }

    private val observers: MutableMap<PatternEventType, MutableList<(PatternEvent) -> Unit>> = mutableMapOf()
    private val accumulator
        get() = canvas.getNamedGraphicsReference(Theme.GRAPHICS_ACCUMULATOR, useOpenGL = true).graphics
    private val patternGraphics
        get() = canvas.getNamedGraphicsReference(Theme.GRAPHICS_PATTERN).graphics
    val getFillColorsLambda =
        { x: Float, y: Float, applyCubeAlpha: Boolean -> getFillColor(x, y, applyCubeAlpha) }

    var drawRate = 1
        private set

    protected var ghostState: GhostState = GhostOff()
    /*var threeD = ThreeD(canvas, visuals)
        private set*/

    init {
        registerObserver(PatternEventType.PatternSwapped) { _ -> resetOnNewPattern() }
        registerObserver(PatternEventType.PatternSwapped) { _ ->
            canvas.newPattern()
        }
    }

    override fun onStateChanged(changedOption: Visual) {
        when (changedOption) {
            Visual.DarkMode -> updateTheme()
            Visual.GhostMode -> ghostState.transition()
            Visual.FadeAway -> initFadeAway()
            Visual.ThreeDYaw, Visual.ThreeDPitch, Visual.ThreeDRoll -> threeD.updateActiveRotations(changedOption)
            else -> {}
        }
    }

    private fun initFadeAway() {
        with(accumulator) {
            beginDraw()
            clear()
            endDraw()
        }
    }


    private fun updateTheme() {
        Theme.setTheme(
            when (Theme.currentThemeType) {
                ThemeType.DEFAULT -> {
                    ThemeType.DARK
                }

                else -> {
                    ThemeType.DEFAULT
                }
            }
        )
    }

    // requirements of a pattern
    abstract fun drawPattern(patternIsDrawable: Boolean, shouldAdvancePattern: Boolean)
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
        with(visuals) {
            disable(Visual.AlwaysRotate)
            disable(Visual.GhostMode)
            disable(Visual.GhostFadeAwayMode)
            disable(Visual.ThreeDBoxes)
            disable(Visual.ThreeDYaw)
            disable(Visual.ThreeDPitch)
            disable(Visual.ThreeDRoll)
        }
        resetDrawRate()
    }

    fun onNewPattern(patternName: String) {
        notifyObservers(PatternEventType.PatternSwapped, PatternEvent.PatternSwapped(patternName))
    }

    fun stampGhostModeKeyFrame() {
        ghostState = GhostKeyFrame()
    }

    private fun getFillColor(x: Float, y: Float, applyCubeAlpha: Boolean = true): Int {

        val cubeAlpha = if (
            visuals requires Visual.ThreeDBoxes &&
            canvas.zoomLevel >= 4F && applyCubeAlpha
        )
            Theme.cubeAlpha else Theme.OPAQUE

        return with(patternGraphics) {
            val color = if (visuals requires Visual.Colorful) {
                colorMode(PConstants.HSB, 360f, 100f, 100f, 255f)
                val mappedColor = PApplet.map(x + y, 0f, canvas.width + canvas.height, 0f, 360f)
                color(mappedColor, 100f, 100f, cubeAlpha.toFloat())
            } else {
                Theme.cellColor.applyAlpha(cubeAlpha)
            }

            ghostState.applyAlpha(color)
        }
    }


    fun move(movementDelta:PVector) {
        canvas.saveUndoState()

        val newScreenOffset = PVector.add(canvas.offsetVector, movementDelta)
        canvas.setCanvasOffsets(newScreenOffset)
    }

    fun draw() {
        with(pApplet) {
            background(Theme.backgroundColor)

            val shouldAdvancePattern = RunningModeController.shouldAdvancePattern()

            with(patternGraphics) {
                beginDraw()

                ghostState.prepareGraphics(this)
                stroke(ghostState.applyAlpha(Theme.backgroundColor))

                val patternIsDrawable = shouldAdvancePattern || (visuals requires Visual.AlwaysRotate)
                if (patternIsDrawable)
                    threeD.rotate()

                drawPattern(patternIsDrawable, shouldAdvancePattern)

                drawMousePoints()

                endDraw()
            }

            handleImaging()

        }
    }

    private fun drawMousePoints() {

        if (!(visuals requires Visual.ThreeDMousePosition)) return

        val mouse = PVector(pApplet.mouseX.toFloat(), pApplet.mouseY.toFloat())

        val mouseAsModelToScreen = threeD.translatePVector(mouse,ThreeD.Translate.ToScreen)

        val mousePosInModel = threeD.translatePVector(mouse, ThreeD.Translate.ToModel)

        // this is just a sanity check in a sense as the result should be the same as the mouse position
        val mousePosReified = threeD.translatePVector(mousePosInModel, ThreeD.Translate.ToScreen)

        val points = listOf(
            Triple("ToScreen",
                mouseAsModelToScreen,
                getFillColor(mouseAsModelToScreen.x, mouseAsModelToScreen.y)
            ),
            Triple("ToModel", mousePosInModel, getFillColor(mousePosInModel.x, mousePosInModel.y)),
            Triple("mouseReified", mousePosReified, getFillColor(mousePosReified.x, mousePosReified.y)),
        )
        drawCirclesAndLines(points)

    }


    @Suppress("UNUSED_VARIABLE")
    private fun drawCirclesAndLines(points: List<Triple<String, PVector, Int>>) {
        with(patternGraphics) {

            // Draw circles
            for ((name, point, color) in points) {
                fill(ghostState.applyAlpha(color))
                circle(point.x, point.y, Theme.MOUSE_CIRCLE_SIZE)
                /*fill(Theme.RED)  // Set text color, e.g., to black
                text(name, point.x + Theme.MOUSE_CIRCLE_SIZE / 2f + 5f, point.y)*/
            }

            // Draw lines between circles
            for (i in 0..2) {
                val j = (i + 1) % 3
                /*          stroke(ghostState.applyAlpha(Theme.cellColor))  // Set the line color; adjust as needed
                         line(points[i].first.x, points[i].first.y, points[j].first.x, points[j].first.y)*/
                patternGraphics.push()
                patternGraphics.beginShape(PConstants.POINTS)
                patternGraphics.noFill()
                strokeWeight(Theme.STROKE_WEIGHT_BOUNDS)
                patternGraphics.drawPixelLine(
                    startX = points[i].second.x.toInt(),
                    startY = points[i].second.y.toInt(),
                    endX = points[j].second.x.toInt(),
                    endY = points[j].second.y.toInt(),
                    getFillColor = getFillColorsLambda,
                )
                patternGraphics.endShape()
                patternGraphics.pop()
            }
        }
    }


    private fun PApplet.handleImaging() {

        if (visuals requires Visual.FadeAway) {

            with(accumulator) {
                beginDraw()
                blendMode(PConstants.BLEND)
                fill(Theme.backgroundColor, 15f)
                rect(0f, 0f, width.toFloat(), height.toFloat())
                image(patternGraphics, 0f, 0f)
                endDraw()
            }
            image(accumulator, 0f, 0f)

        } else {
            image(patternGraphics, 0f, 0f)
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
        val img = canvas.getNamedGraphicsReference(Theme.GRAPHICS_PATTERN).graphics.get()
        newGraphics.image(img, 0f, 0f)
        newGraphics.endDraw()

        val desktopDirectory = System.getProperty("user.home") + "/Desktop/"
        newGraphics.save("$desktopDirectory${pApplet.frameCount}.png")
    }

    fun drawSlower() {
        drawRate++
    }

    fun drawFaster() {
        if (drawRate > 1)
             drawRate--
    }

    fun resetDrawRate() {
        drawRate = 1
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