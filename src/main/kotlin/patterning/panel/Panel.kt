package patterning.panel

import java.awt.Component
import java.awt.MouseInfo
import java.util.OptionalInt
import patterning.Canvas
import patterning.DrawBuffer
import patterning.Drawable
import patterning.PatterningPApplet
import patterning.Theme
import patterning.actions.MouseEventReceiver
import patterning.panel.Transition.TransitionDirection
import patterning.panel.Transition.TransitionType
import processing.core.PGraphics
import processing.core.PVector

/**
 * Panel is the base of all UI elements in Patterning
 *
 * I'm using the kotlin apply function to allow for a builder pattern
 * if i want to use method chaining via something like this I'd have to implement it with generics
 *
 * ```
 * TextPanel.Build()
 *  .radius(10)
 *  .fill(20)
 *  .textSize(30)
 *  .build()
 * ```
 * To keep it simple, instead I'm using basic inheritance with apply blocks
 * to allow for method chaining which has to be done like this:
 * ```
 *   TextPanel.Build.apply {
 *      radius(10)
 *      fill(20)
 *      textSize(30)
 *   }.build()
 *   ```
 *   the apply ensures that you can apply methods from anywhere in the hierarchy without
 *   having to worry about the return type of each. The receiver of the apply block will always be
 *   TextPanel.builder type - allowing you to call methods from both the parent and the child
 *   seamlessly
 *
 *   Note that you'll only need to use apply if you're trying to chain methods from different levels
 *   of the hierarchy - the compiler will tell you if it can't find something when you chain the first way
 *   with methods from across the hierarchy.
 */
abstract class Panel protected constructor(builder: Builder) : Drawable, MouseEventReceiver {
    
    
    internal var parentPanel: Panel? = null
    internal val canvas: Canvas
    
    // alignment
    private val alignAble: Boolean
    private val radius: OptionalInt
    
    // size & positioning
    var position: PVector = PVector()
    var width: Int
    var height: Int
    protected var fill: Int
    var hAlign: AlignHorizontal
    var vAlign: AlignVertical
    
    // transition
    private var transitionAble: Boolean
    private var transition: Transition? = null
    
    // image buffers and callbacks
    internal var parentBuffer: DrawBuffer
    private val parentGraphics: PGraphics
        get() = parentBuffer.graphics
    
    internal lateinit var panelGraphics: PGraphics
    
    // mouse stuff
    var isPressed = false
    var isHovering = false
    var isHoveringPrevious = false
    private var transitionDirection: TransitionDirection?
    private var transitionType: TransitionType?
    private var transitionDuration: Int
    
    init {
        setPosition(builder.x, builder.y)
        canvas = builder.canvas
        width = builder.width
        height = builder.height
        radius = builder.radius
        hAlign = builder.hAlign
        vAlign = builder.vAlign
        alignAble = builder.alignable
        fill = builder.fill
        transitionDirection = builder.transitionDirection
        transitionType = builder.transitionType
        transitionDuration = builder.transitionDuration
        transitionAble = transitionDirection != null && transitionType != null
        
        parentBuffer = canvas.getDrawBuffer(Theme.uxBuffer) // drawingContext.getPGraphics()
        
        // we don't say UXBuffer here because sometimes the parentBuffer is the uxBuffer
        // and sometimes it's going to be the buffer from a container panel so we need to
        // be using the correct one when we do invoke alignment
        initPanelBuffer()
        
        if (transitionAble) {
            transition = Transition(canvas, transitionDirection!!, transitionType!!, transitionDuration)
        }
    }
    
    fun setPosition(x: Int, y: Int) {
        position.x = x.toFloat()
        position.y = y.toFloat()
    }
    
    fun initPanelBuffer() {
        panelGraphics = parentGraphics.parent.createGraphics(width, height)
    }
    
    override fun onMousePressed() {
        isPressed = isMouseOverMe
        if (isPressed) {
            isHovering = false
            isHoveringPrevious = true
        }
    }
    
    override fun onMouseReleased() {
        if (isMouseOverMe) {
            isPressed = false
        }
    }
    
    override fun mousePressedOverMe(): Boolean {
        return isMouseOverMe
    }
    
    fun setTransition(direction: TransitionDirection?, type: TransitionType?, duration: Int) {
        transitionDirection = direction
        transitionType = type
        transitionDuration = duration
        transitionAble = true
    }
    
    open fun updatePanelBuffer() {}
    
    //public void draw(PGraphics parentBuffer) {
    override fun draw() {
        
        val localParent = parentGraphics
        val parentBuffer = parentGraphics // drawingContext.getPGraphics()
        
        /* some subclasses (e.g. TextPanel) need to adjust the panelBuffer before drawing */
        updatePanelBuffer()
        
        panelGraphics.beginDraw()
        panelGraphics.pushStyle()
        panelGraphics.fill(fill)
        panelGraphics.noStroke()
        panelGraphics.clear()
        
        // handle alignment if requested
        if (alignAble) {
            updateAlignment(parentBuffer)
        }
        
        // output the background Rect for this panel
        if (radius.isPresent) {
            panelGraphics.rect(0f, 0f, width.toFloat(), height.toFloat(), radius.asInt.toFloat())
        } else {
            panelGraphics.rect(0f, 0f, width.toFloat(), height.toFloat())
        }
        
        // subclass of Panels (such as a Control) can provide an implementation to be called at this point
        panelSubclassDraw()
        panelGraphics.endDraw()
        
        if (this is TextPanel) parentBuffer.blendMode(Theme.blendMode)
        
        if (transitionAble && transition!!.isTransitioning) {
            transition!!.image(panelGraphics, position.x, position.y)
        } else {
            parentBuffer.image(panelGraphics, position.x, position.y)
        }
        
    }
    
    private fun updateAlignment(buffer: PGraphics) {
        var posX = 0
        var posY = 0
        when (hAlign) {
            AlignHorizontal.CENTER -> posX = (buffer.width - width) / 2
            AlignHorizontal.RIGHT -> posX = buffer.width - width
            else -> {}
        }
        when (vAlign) {
            AlignVertical.CENTER -> posY = (buffer.height - height) / 2
            AlignVertical.BOTTOM -> posY = buffer.height - height
            else -> {}
        }
        setPosition(posX, posY)
    }
    
    protected abstract fun panelSubclassDraw()
    
    private val effectivePosition: PVector
        get() =// used in isMouseOverMe when a Panel contains other Panels
            // can walk up the hierarchy if you have nested panels
            if (parentPanel != null) {
                PVector(
                    position.x + parentPanel!!.effectivePosition.x,
                    position.y + parentPanel!!.effectivePosition.y
                )
            } else {
                position
            }
    
    protected val isMouseOverMe: Boolean
        get() {
            return try {
                parentPanel?.let {
                    // the parent is a Panel, which has a PGraphics panelBuffer which has its PApplet
                    val pApplet = it.panelGraphics.parent
                    
                    // our Patterning class extends Processing so we can use it here also
                    val patterningPApplet = pApplet as PatterningPApplet
                    if (patterningPApplet.draggingDrawing) {
                        false
                    } else {
                        val mousePosition = MouseInfo.getPointerInfo().location
                        val windowPosition = (pApplet.getSurface().native as Component).locationOnScreen
                        val mouseX = mousePosition.x - windowPosition.x
                        val mouseY = mousePosition.y - windowPosition.y
                        if (mouseX < 0 || mouseX > pApplet.width || mouseY < 0 || mouseY > pApplet.height) {
                            false
                        } else {
                            val effectivePosition = effectivePosition
                            mouseX >= effectivePosition!!.x && mouseX < effectivePosition.x + width && mouseY >= effectivePosition.y && mouseY < effectivePosition.y + height
                        }
                    }
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
    
    abstract class Builder {
        val canvas: Canvas
        var x = 0
        var y = 0
        var width = 0
        var height = 0
        var alignable = false
        var hAlign: AlignHorizontal = AlignHorizontal.LEFT
        var vAlign: AlignVertical = AlignVertical.TOP
        var fill = Theme.defaultPanelColor
        var transitionDirection: TransitionDirection? = null
        var transitionType: TransitionType? = null
        var transitionDuration = 0
        var radius: OptionalInt = OptionalInt.empty()
        
        // used by Control
        constructor(canvas: Canvas, width: Int, height: Int) {
            setRect(x = 0, y = 0, width = width, height = height) // parent positioned
            this.canvas = canvas
        }
        
        // used by TextPanel for explicitly positioned text
        constructor(
            canvas: Canvas,
            position: PVector,
            hAlign: AlignHorizontal,
            vAlign: AlignVertical
        ) {
            setRect(position.x.toInt(), position.y.toInt(), 0, 0) // parent positioned
            setAlignment(hAlign, vAlign, false)
            this.canvas = canvas
        }
        
        private fun setRect(x: Int, y: Int, width: Int, height: Int) {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
        }
        
        private fun setAlignment(hAlign: AlignHorizontal, vAlign: AlignVertical, alignAble: Boolean) {
            alignable = alignAble
            this.hAlign = hAlign
            this.vAlign = vAlign
        }
        
        // used by BasicPanel for demonstration purposes
        constructor(
            canvas: Canvas,
            hAlign: AlignHorizontal,
            vAlign: AlignVertical,
            width: Int,
            height: Int
        ) {
            setRect(0, 0, width, height) // we're only using BasicPanel to show that panels are useful...
            setAlignment(hAlign, vAlign, true)
            this.canvas = canvas
        }
        
        //  ContainerPanel(s) and TextPanel are often alignHorizontal / vAlign able
        constructor(
            canvas: Canvas,
            hAlign: AlignHorizontal,
            vAlign: AlignVertical
        ) {
            setRect(0, 0, 0, 0) // Containers and text, so far, only need to be aligned around the screen
            setAlignment(hAlign, vAlign, true)
            this.canvas = canvas
        }
        
        fun fill(fill: Int) = apply { this.fill = fill }
        
        fun transition(direction: TransitionDirection?, type: TransitionType?, duration: Int) = apply {
            transitionDirection = direction
            transitionType = type
            transitionDuration = duration
        }
        
        fun radius(radius: Int) = apply { this.radius = OptionalInt.of(radius) }
        
        abstract fun build(): Panel
    }
}