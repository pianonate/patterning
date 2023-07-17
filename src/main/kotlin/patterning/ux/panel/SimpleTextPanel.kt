package patterning.ux.panel

import patterning.ux.informer.DrawingInfoSupplier

class SimpleTextPanel private constructor(builder: Builder?) : Panel(builder!!) {
    override fun panelSubclassDraw() {
    }

    class Builder(
        drawingInfoSupplier: DrawingInfoSupplier?,
        alignHorizontal: AlignHorizontal?,
        vAlign: AlignVertical?,
        width: Int,
        height: Int
    ) : Panel.Builder<Builder?>(
        drawingInfoSupplier!!, alignHorizontal!!, vAlign!!, width, height
    ) {
        override fun build(): SimpleTextPanel {
            return SimpleTextPanel(this)
        }

        override fun self(): Builder {
            return this
        }
    }
}