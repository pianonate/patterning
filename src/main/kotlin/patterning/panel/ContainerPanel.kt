package patterning.panel

import patterning.Canvas
import patterning.GraphicsReference

abstract class ContainerPanel protected constructor(builder: Builder) : Panel(builder) {
    private val childPanels: List<Panel>
    val orientation: Orientation

    init {
        childPanels = ArrayList(builder.childPanels)
        check(childPanels.isNotEmpty()) { "ContainerPanel must have at least one child panel" }
        orientation = builder.orientation
        updatePanelSize()

        // super(builder) causes Panel to create an initial panelGraphics
        // to draw into.  However ContainerPanel's don't have a width and height until
        // we've run updatePanelSize as we don't know how
        // many children will get added to a ContainerPanel -
        // given we've already called super(builder), set
        // as the one created in Panel won't work
        // there's probably a better way but i think it can wait
        // so, just re-initialize the already created panelGraphics
        initPanelGraphics()
        for (child in childPanels) {
            // child panels need special handling to orient themselves to this container Panel
            child.parentPanel = this
            child.parentGraphicsReference = GraphicsReference(panelGraphics, "ContainerPanel")
        }
    }

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

    // Constructor for aligned Panel with default dimensions (0, 0)
    // addPanel will update the actual dimensions
    abstract class Builder
        (canvas: Canvas, hAlign: AlignHorizontal, vAlign: AlignVertical) :
        Panel.Builder(
            canvas, hAlign, vAlign
        ) {
        val childPanels: MutableList<Panel> = ArrayList()

        var orientation = Orientation.HORIZONTAL

        fun setOrientation(orientation: Orientation) = apply { this.orientation = orientation }

        fun addPanel(child: Panel) = apply { childPanels.add(child) }

        abstract override fun build(): ContainerPanel
    }
}