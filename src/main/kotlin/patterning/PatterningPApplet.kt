package patterning

import Canvas
import kotlin.math.roundToInt
import patterning.actions.KeyHandler
import patterning.actions.MouseEventManager
import patterning.life.LifePattern
import processing.core.PApplet

class PatterningPApplet : PApplet() {
    var draggingDrawing = false
    
    private lateinit var pattern: LifePattern
    private lateinit var properties: Properties
    
    // used to control dragging the image around the screen with the mouse
    private var lastMouseX = 0f
    private var lastMouseY = 0f
    
    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private var mousePressedOverReceiver = false
    
    override fun settings() {
        properties = Properties(this)
        size(properties.width, properties.height)
    }
    
    override fun setup() {
        Theme.initialize(this)
        KeyHandler.registerKeyHandler(this)
        
        surface.setResizable(true)
        
        properties.setWindowPosition()
        
        pattern = LifePattern(
            pApplet = this,
            canvas = Canvas(this),
            properties
        ).also {
            println(KeyHandler.usageText)
        }
    }
    
    override fun draw() {
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