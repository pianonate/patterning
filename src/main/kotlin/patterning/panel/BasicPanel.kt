package patterning.panel

import patterning.Canvas
import patterning.DrawingContext

class BasicPanel private constructor(builder: Builder) : Panel(builder) {
    override fun panelSubclassDraw() {
    }
    
    class Builder(
        drawingContext: DrawingContext,
        canvas: Canvas,
        hAlign: AlignHorizontal,
        vAlign: AlignVertical,
        width: Int,
        height: Int
    ) : Panel.Builder(
        drawingContext, canvas,hAlign, vAlign, width, height
    ) {
        override fun build() = BasicPanel(this)
    }
}