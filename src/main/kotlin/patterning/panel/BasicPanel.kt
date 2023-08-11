package patterning.panel

import patterning.Canvas

class BasicPanel private constructor(builder: Builder) : Panel(builder) {
    override fun panelSubclassDraw() {
    }
    
    class Builder(
        canvas: Canvas,
        hAlign: AlignHorizontal,
        vAlign: AlignVertical,
        width: Int,
        height: Int
    ) : Panel.Builder(
        canvas,hAlign, vAlign, width, height
    ) {
        override fun build() = BasicPanel(this)
    }
}