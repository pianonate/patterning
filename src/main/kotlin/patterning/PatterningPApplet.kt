package patterning

import kotlin.math.roundToInt

import patterning.actions.KeyHandler
import patterning.actions.MouseEventManager
import patterning.life.LifePattern
import processing.core.PApplet


class PatterningPApplet : PApplet() {
    
    private val canvas = Canvas(this)
    private lateinit var pattern: LifePattern
    private lateinit var properties: Properties
    
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
        
        canvas.updateDimensions()
        
        pattern = LifePattern(
            pApplet = this,
            canvas = canvas,
            properties
        ).also {
            println(KeyHandler.usageText)
        }
    }
    
    override fun draw() {
        canvas.drawBackground()
        pattern.draw()
        
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