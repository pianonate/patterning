package patterning.panel

import patterning.Canvas
import patterning.actions.KeyCallback
import patterning.pattern.Pattern
import patterning.pattern.PatternEventType
import processing.event.KeyEvent

class ToggleHighlightControl private constructor(builder: Builder) : Control(builder) {
    
    init {
        builder.resetOnNew?.registerObserver(PatternEventType.PatternSwapped) {
            // if a new pattern is loaded we stop 'yaw-ing' as it's too confusing from a ux perspective
            isHighlightFromKeypress = false
        }
    }
    
    override fun onMouseReleased() {
        super.onMouseReleased()
        toggleHighlight()
    }
    
    override fun notifyGlobalKeyPress(event: KeyEvent) {
        toggleHighlight()
    }
    
    class Builder(
        canvas: Canvas,
        callback: KeyCallback,
        iconName: String,
        size: Int,
        val resetOnNew: Pattern? = null,
    ) :
        Control.Builder(canvas, callback, iconName, size) {
        override fun build() = ToggleHighlightControl(this)
    }
    
}