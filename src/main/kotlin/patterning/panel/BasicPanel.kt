package patterning.panel

import patterning.DrawingInformer

class BasicPanel private constructor(builder: Builder) : Panel(builder) {
    override fun panelSubclassDraw() {
    }
    
    class Builder(
        drawingInformer: DrawingInformer,
        hAlign: AlignHorizontal,
        vAlign: AlignVertical,
        width: Int,
        height: Int
    ) : Panel.Builder(
        drawingInformer, hAlign, vAlign, width, height
    ) {
        override fun build() = BasicPanel(this)
    }
}