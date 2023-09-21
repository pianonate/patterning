package patterning

import kotlin.math.roundToInt
import patterning.actions.KeyHandler
import patterning.actions.MouseEventNotifier
import patterning.pattern.Movable
import patterning.pattern.Pattern
import patterning.pattern.PatternEvent
import patterning.pattern.PatternEventType
import processing.core.PApplet


class PatterningPApplet : PApplet() {

    private val canvas = Canvas(this)
    private lateinit var ux: UX
    private lateinit var pattern: Pattern
    private lateinit var properties: Properties

    // used to control dragging the image around the screen with the mouse
    private var lastMouseX = 0f
    private var lastMouseY = 0f
    var draggingDrawing = false

    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private var mousePressedOverReceiver = false

    override fun settings() {
        fullScreen(P3D)
    }

    /**
     * we can't load properties until setup because of idiosyncrasies of processing
     * and i don't want to find a different way right now to load a properties file
     * so we don't have the stored window position until now
     *
     * if we did have it, we could construct Canvas with the actual width, height.
     * i also didn't want Canvas to be a var, so we internally expose the updateDimensions()
     * to work around Processing
     */
    override fun setup() {
        windowTitle("patterning")
        Theme.init(this)

        // pattern needs to have proper dimensions before instantiation
        canvas.updateDimensions()

        properties = Properties(this)

        pattern = patterning.life.LifePattern(
            pApplet = this,
            canvas = canvas,
            properties
        )

        ux = UX(this, canvas, pattern)

        // ux has to be created before observers are registered
        registerPatternObservers(pattern)

        // order matters here - dont' load the pattern until
        // observers have been registered!
        pattern.loadPattern()

        // ux sets up key callbacks and we can now print them out
        println(KeyHandler.usageText)

        KeyHandler.registerKeyHandler(this)
    }

    private fun registerPatternObservers(pattern: Pattern) {

        pattern.registerObserver(PatternEventType.DimensionChanged) { event ->
            canvas.updateBiggestDimension((event as PatternEvent.DimensionChanged).biggestDimension)
        }

        pattern.registerObserver(PatternEventType.PatternSwapped) { _ ->
            canvas.newPattern()
        }

        pattern.registerObserver(PatternEventType.PatternSwapped) { event ->
            ux.newPattern((event as PatternEvent.PatternSwapped).patternName)
        }
    }

    override fun draw() {

        try {

            canvas.handleResize()
            pattern.drawBackground()

            if (pattern is Movable) {
                canvas.updateZoom()
            }

            pattern.draw()
            ux.draw()

        } catch (e: Exception) {
            // it seems these only seem to happen during startup or during resizing (moving from screen to screeN)
            // we can just let them go for now - log them just in case
            // possibly the early return above has fixed this but leaving it in just in case something else
            // mysterious pops up
            println("error in draw() - the uncaught error of last resort: $e")
        }

    }

    override fun mousePressed() {
        lastMouseX += mouseX.toFloat()
        lastMouseY += mouseY.toFloat()
        MouseEventNotifier.onMousePressed()
        mousePressedOverReceiver = MouseEventNotifier.isMousePressedOverAnyReceiver

        // If the mouse press is not over any MouseEventReceiver, we consider it as over the drawing.
        draggingDrawing = !mousePressedOverReceiver
    }

    override fun mouseReleased() {
        if (draggingDrawing) {
            draggingDrawing = false
        } else {
            mousePressedOverReceiver = false
            MouseEventNotifier.onMouseReleased()
        }
        lastMouseX = 0f
        lastMouseY = 0f

        ux.mouseReleased()
    }

    override fun mouseDragged() {
        if (draggingDrawing) {
            val dx = (mouseX - lastMouseX).roundToInt().toFloat()
            val dy = (mouseY - lastMouseY).roundToInt().toFloat()
            pattern.move(dx, dy)
            lastMouseX += dx
            lastMouseY += dy
        }
    }

    // Override the exit() method to save window properties before closing
    override fun exit() {
        pattern.updateProperties()
        properties.saveProperties()
        super.exit()
    }
}