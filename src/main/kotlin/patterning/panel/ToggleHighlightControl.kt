package patterning.panel

import patterning.Canvas
import patterning.DrawingContext
import patterning.actions.KeyCallback
import patterning.actions.KeyObservable

class ToggleHighlightControl private constructor(builder: Builder) : Control(
    builder
) {
    override fun onMouseReleased() {
        super.onMouseReleased()
        isHighlightFromKeypress = !isHighlightFromKeypress
    }
    
    override fun notifyKeyPress(observer: KeyObservable) {
        isHighlightFromKeypress = !isHighlightFromKeypress
    }
    
    class Builder(
        drawingContext: DrawingContext,
        canvas: Canvas,
        callback: KeyCallback,
        iconName: String,
        size: Int
    ) :
        Control.Builder(drawingContext, canvas, callback, iconName, size) {
        override fun build() = ToggleHighlightControl(this)
    }
}