package patterning.panel

import patterning.Canvas
import patterning.actions.KeyCallback
import processing.event.KeyEvent

class ToggleHighlightControl private constructor(builder: Builder) : Control(
    builder
) {
    override fun onMouseReleased() {
        super.onMouseReleased()
        isHighlightFromKeypress = !isHighlightFromKeypress
    }
    
    override fun onKeyEvent(event: KeyEvent) {
        isHighlightFromKeypress = !isHighlightFromKeypress
    }
    
    class Builder(
        canvas: Canvas,
        callback: KeyCallback,
        iconName: String,
        size: Int
    ) :
        Control.Builder(canvas, callback, iconName, size) {
        override fun build() = ToggleHighlightControl(this)
    }
}