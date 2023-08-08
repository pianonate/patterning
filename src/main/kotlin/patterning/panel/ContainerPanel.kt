package patterning.panel

import patterning.DrawingInformer
import processing.core.PGraphics

abstract class ContainerPanel protected constructor(builder: Builder<*>) : Panel(builder) {
    private val childPanels: List<Panel>
    val orientation: Orientation
    
    init {
        childPanels = ArrayList(builder.childPanels)
        check(childPanels.isNotEmpty()) { "ContainerPanel must have at least one child panel" }
        
        // Set parent panel for each child
        for (child in childPanels) {
            // child panels need special handling to orient themselves to this container Panel
            // rather than the UXBuffer, which is the more common case...
            child.parentPanel = this
            child.drawingInformer =
                DrawingInformer({ containerPanelBuffer })
        }
        orientation = builder.orientation
        updatePanelSize()
        
        // super(builder) causes Panel to create an initial panelBuffer
        // to draw into.  However ContainerPanel's don't have a width and height until
        // we've run updatePanelSize as we don't know how
        // many children will get added to a ContainerPanel -
        // given we've already called super(builder), set
        // as the one created in Panel won't work
        // there's probably a better way but i think it can wait
        panelBuffer = initPanelBuffer(drawingInformer.getPGraphics())
    }
    
    private val containerPanelBuffer: PGraphics
        get() = panelBuffer
    
    private fun updatePanelSize() {
        var totalWidth = 0
        var totalHeight = 0
        for (child in childPanels) {
            if (orientation === Orientation.HORIZONTAL) {
                child.setPosition(totalWidth, 0)
                totalWidth += child.width
                totalHeight = totalHeight.coerceAtLeast(child.height)
            } else { // Orientation.VERTICAL
                child.setPosition(0, totalHeight)
                totalHeight += child.height
                totalWidth = totalWidth.coerceAtLeast(child.width)
            }
        }
        
        // Update parent size
        width = totalWidth
        height = totalHeight
    }
    
    override fun panelSubclassDraw() {
        // Draw child panels
        for (child in childPanels) {
            child.draw()
        }
    }
    
    abstract class Builder<P : Builder<P>>  // Constructor for aligned Panel with default dimensions (0, 0)
    // addPanel will update the actual dimensions
        (drawingInformer: DrawingInformer, hAlign: AlignHorizontal?, vAlign: AlignVertical?) :
        Panel.Builder<P>(
            drawingInformer, hAlign!!, vAlign!!
        ) {
        val childPanels: MutableList<Panel> = ArrayList()
        
        var orientation = Orientation.HORIZONTAL
        protected open fun setOrientation(orientation: Orientation): P {
            this.orientation = orientation
            return self()
        }
        
        protected fun addPanel(child: Panel): P {
            childPanels.add(child)
            return self()
        }
        
        @Suppress("UNCHECKED_CAST")
        override fun self(): P {
            return this as P
        }
        
        abstract override fun build(): ContainerPanel
    }
}