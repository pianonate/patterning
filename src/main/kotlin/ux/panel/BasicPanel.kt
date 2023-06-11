package ux.panel

import ux.informer.DrawingInfoSupplier

class BasicPanel private constructor(builder: Builder?) : Panel(builder!!) {
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
        override fun build(): BasicPanel {
            return BasicPanel(this)
        }

        override fun self(): Builder {
            return this
        }
    }
}