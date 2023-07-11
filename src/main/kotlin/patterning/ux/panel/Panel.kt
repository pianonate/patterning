package patterning.ux.panel

import patterning.Processing
import patterning.actions.MouseEventReceiver
import patterning.ux.Drawable
import patterning.ux.Theme
import patterning.ux.informer.DrawingInfoSupplier
import patterning.ux.panel.Transition.TransitionDirection
import patterning.ux.panel.Transition.TransitionType
import processing.core.PGraphics
import processing.core.PVector
import java.awt.Component
import java.awt.MouseInfo
import java.util.*

abstract class Panel protected constructor(builder: Builder<*>) : Drawable, MouseEventReceiver {
    // alignment
    private val alignAble: Boolean
    private val radius: OptionalInt



    var parentPanel: Panel? = null
    var drawingInformer: DrawingInfoSupplier

    // size & positioning
    var position: PVector? = null
    var width: Int
    var height: Int
    protected var fill: Int
    var hAlign: AlignHorizontal?
    var vAlign: AlignVertical?

    // transition
    private var transitionAble: Boolean
    private var transition: Transition? = null

    // image buffers and callbacks
    protected var panelBuffer: PGraphics

    // mouse stuff
    var isPressed = false
    var isHovering = false
    var isHoveringPrevious = false
    private var transitionDirection: TransitionDirection?
    private var transitionType: TransitionType?
    private var transitionDuration: Int

    init {
        setPosition(builder.x, builder.y)
        drawingInformer = builder.drawingInformer
        width = builder.width
        height = builder.height
        radius = builder.radius
        hAlign = builder.alignHorizontal
        vAlign = builder.alignVertical
        alignAble = builder.alignable
        fill = builder.fill
        transitionDirection = builder.transitionDirection
        transitionType = builder.transitionType
        transitionDuration = builder.transitionDuration
        transitionAble = transitionDirection != null && transitionType != null
        val parentBuffer = drawingInformer.supplyPGraphics()
        panelBuffer = getPanelBuffer(parentBuffer)
        if (transitionAble) {
            transition = Transition(drawingInformer, transitionDirection!!, transitionType!!, transitionDuration)
        }
    }

    fun setPosition(x: Int, y: Int) {
        if (position == null) {
            position = PVector()
        }
        position!!.x = x.toFloat()
        position!!.y = y.toFloat()
    }

    protected fun getPanelBuffer(parentBuffer: PGraphics): PGraphics {
        return parentBuffer.parent.createGraphics(width, height)
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

/*    fun setFill(fill: Int) {
        this.fill = fill
    }*/

    fun setTransition(direction: TransitionDirection?, type: TransitionType?, duration: Int) {
        transitionDirection = direction
        transitionType = type
        transitionDuration = duration
        transitionAble = true
    }

    //public void draw(PGraphics parentBuffer) {
    override fun draw() {
        val parentBuffer = drawingInformer.supplyPGraphics()
        parentBuffer.pushStyle()
        panelBuffer.beginDraw()
        panelBuffer.pushStyle()
        panelBuffer.fill(fill)
        //panelBuffer.fill(0xFFFF0000); // debugging ghost panel
        panelBuffer.noStroke()
        panelBuffer.clear()

        // handle alignment if requested
        if (alignAble) {
            updateAlignment(parentBuffer)
        }

        // output the background Rect for this panel
        if (radius.isPresent) {
            panelBuffer.rect(0f, 0f, width.toFloat(), height.toFloat(), radius.asInt.toFloat())
        } else {
            panelBuffer.rect(0f, 0f, width.toFloat(), height.toFloat())
        }

        // subclass of Panels (such as a Control) can provide an implementation to be called at this point
        panelSubclassDraw()
        panelBuffer.endDraw()
        parentBuffer.popStyle()
        if (transitionAble && transition!!.isTransitioning) {
            transition!!.image(panelBuffer, position!!.x, position!!.y)
        } else {
            parentBuffer.image(panelBuffer, position!!.x, position!!.y)
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

    private val effectivePosition: PVector?
        get() =// used in isMouseOverMe when a Panel contains other Panels
            // can walk up the hierarchy if you have nested panels
            if (parentPanel != null) {
                PVector(
                    position!!.x + parentPanel!!.effectivePosition!!.x,
                    position!!.y + parentPanel!!.effectivePosition!!.y
                )
            } else {
                position
            }
/*    protected val isMouseOverMe: Boolean
        protected get() = try {
            // the parent is a Panel, which has a PGraphics panelBuffer which has its PApplet
            val processing = parentPanel!!.panelBuffer.parent

            // our Patterning class extends Processing so we can use it here also
            val patterning = processing as Patterning
            if (patterning.draggingDrawing) {
                return false
            }
            val mousePosition = MouseInfo.getPointerInfo().location
            val windowPosition = (processing.getSurface().native as Component).locationOnScreen
            val mouseX = mousePosition.x - windowPosition.x
            val mouseY = mousePosition.y - windowPosition.y
            if (mouseX < 0 || mouseX > processing.width || mouseY < 0 || mouseY > processing.height) {
                return false
            }
            val effectivePosition = effectivePosition
            mouseX >= effectivePosition!!.x && mouseX < effectivePosition.x + width && mouseY >= effectivePosition.y && mouseY < effectivePosition.y + height
        } catch (e: Exception) {
            false
        }*/
protected val isMouseOverMe: Boolean
    get() {
        return try {
            // the parent is a Panel, which has a PGraphics panelBuffer which has its PApplet
            val processing = parentPanel!!.panelBuffer.parent

            // our Patterning class extends Processing so we can use it here also
            val patterning = processing as Processing
            if (patterning.draggingDrawing) {
                false
            } else {
                val mousePosition = MouseInfo.getPointerInfo().location
                val windowPosition = (processing.getSurface().native as Component).locationOnScreen
                val mouseX = mousePosition.x - windowPosition.x
                val mouseY = mousePosition.y - windowPosition.y
                if (mouseX < 0 || mouseX > processing.width || mouseY < 0 || mouseY > processing.height) {
                    false
                } else {
                    val effectivePosition = effectivePosition
                    mouseX >= effectivePosition!!.x && mouseX < effectivePosition.x + width && mouseY >= effectivePosition.y && mouseY < effectivePosition.y + height
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    abstract class Builder<T : Builder<T>?> {
        @JvmField
        val drawingInformer: DrawingInfoSupplier
        var x = 0
        var y = 0
        var width = 0
        var height = 0
        var alignable = false
        var alignHorizontal: AlignHorizontal? = null
        var alignVertical: AlignVertical? = null
        var fill = Theme.defaultPanelColor
        var transitionDirection: TransitionDirection? = null
        var transitionType: TransitionType? = null
        var transitionDuration = 0
        var radius: OptionalInt = OptionalInt.empty()

        // used by Control
        constructor(drawingInformer: DrawingInfoSupplier, width: Int, height: Int) {
            setRect(0, 0, width, height) // parent positioned
            this.drawingInformer = drawingInformer
        }

        private fun setRect(x: Int, y: Int, width: Int, height: Int) {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
        }

        // used by TextPanel for explicitly positioned text
        constructor(
            drawingInformer: DrawingInfoSupplier,
            position: PVector,
            hAlign: AlignHorizontal,
            vAlign: AlignVertical
        ) {
            setRect(position.x.toInt(), position.y.toInt(), 0, 0) // parent positioned
            setAlignment(hAlign, vAlign, false)
            this.drawingInformer = drawingInformer
        }

        private fun setAlignment(alignHorizontal: AlignHorizontal, vAlign: AlignVertical, alignAble: Boolean) {
            alignable = alignAble
            this.alignHorizontal = alignHorizontal
            alignVertical = vAlign
        }

        // used by BasicPanel for demonstration purposes
        constructor(
            drawingInformer: DrawingInfoSupplier,
            alignHorizontal: AlignHorizontal,
            alignVertical: AlignVertical,
            width: Int,
            height: Int
        ) {
            setRect(0, 0, width, height) // we're only using BasicPanel to show that panels are useful...
            setAlignment(alignHorizontal, alignVertical, true)
            this.drawingInformer = drawingInformer
        }

        //  ContainerPanel(s) and TextPanel are often alignHorizontal / vAlign able
        constructor(
            drawingInformer: DrawingInfoSupplier,
            alignHorizontal: AlignHorizontal,
            alignVertical: AlignVertical
        ) {
            setRect(0, 0, 0, 0) // Containers and text, so far, only need to be aligned around the screen
            setAlignment(alignHorizontal, alignVertical, true)
            this.drawingInformer = drawingInformer
        }

        fun fill(fill: Int): T {
            this.fill = fill
            return self()
        }

        // Method to allow subclass builders to return "this" correctly
        protected open fun self(): T {
            @Suppress("UNCHECKED_CAST")
            return this as T
        }

        fun transition(direction: TransitionDirection?, type: TransitionType?, duration: Int): T {
            transitionDirection = direction
            transitionType = type
            transitionDuration = duration
            return self()
        }

        fun radius(radius: Int): T {
            this.radius = OptionalInt.of(radius)
            return self()
        }

        abstract fun build(): Panel?
    }
}