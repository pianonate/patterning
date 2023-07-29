package patterning.panel

import patterning.informer.DrawingInfoSupplier

class BasicPanel private constructor(builder: Builder?) : Panel(builder!!) {
    override fun panelSubclassDraw() {
    }

    class Builder(
        drawingInfoSupplier: DrawingInfoSupplier?,
        hAlign: AlignHorizontal?,
        vAlign: AlignVertical?,
        width: Int,
        height: Int
    ) : Panel.Builder<Builder?>(
        drawingInfoSupplier!!, hAlign!!, vAlign!!, width, height
    ) {
        override fun build(): BasicPanel {
            return BasicPanel(this)
        }

        override fun self(): Builder {
            return this
        }
    }
}