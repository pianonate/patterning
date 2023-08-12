package patterning

import kotlin.math.roundToInt
import patterning.actions.KeyHandler
import patterning.actions.MouseEventManager
import patterning.actions.MovementHandler
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
    private lateinit var movementHandler: MovementHandler
    
    
    // used to control dragging the image around the screen with the mouse
    private var lastMouseX = 0f
    private var lastMouseY = 0f
    var draggingDrawing = false
    
    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private var mousePressedOverReceiver = false
    
    override fun settings() {
        properties = Properties(this)
        size(properties.width, properties.height)
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
        Theme.init(this)
        KeyHandler.registerKeyHandler(this)
        
        surface.setResizable(true)
        
        properties.setWindowPosition()
        
        // pattern needs to have proper dimensions before instantiation
        canvas.updateDimensions()
        
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
        
        // the coupling is weird here - we'll see...
        movementHandler = MovementHandler(pattern)
        
        // ux sets up key callbacks and we can now print them out
        println(KeyHandler.usageText)
        
    }
    
    private fun registerPatternObservers(pattern: Pattern) {
        
        pattern.registerObserver(PatternEventType.DimensionChanged) { event ->
            canvas.updateBiggestDimension((event as PatternEvent.DimensionChanged).biggestDimension)
        }
        
        pattern.registerObserver(PatternEventType.PatternSwapped) { _ ->
            canvas.newPattern()
        }
        
        pattern.registerObserver(PatternEventType.PatternSwapped) {event ->
            ux.newPattern((event as PatternEvent.PatternSwapped).patternName)
        }
    }
    
    override fun draw() {
        canvas.drawBackground()
        
        if (pattern is Movable) {
            canvas.updateZoom()
            movementHandler.handleRequestedMovement()
        }
        
        pattern.draw()
        ux.draw()
    }
    
    override fun mousePressed() {
        lastMouseX += mouseX.toFloat()
        lastMouseY += mouseY.toFloat()
        MouseEventManager.onMousePressed()
        mousePressedOverReceiver = MouseEventManager.isMousePressedOverAnyReceiver
        
        // If the mouse press is not over any MouseEventReceiver, we consider it as over the drawing.
        draggingDrawing = !mousePressedOverReceiver
    }
    
    override fun mouseReleased() {
        if (draggingDrawing) {
            draggingDrawing = false
        } else {
            mousePressedOverReceiver = false
            MouseEventManager.onMouseReleased()
        }
        lastMouseX = 0f
        lastMouseY = 0f
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