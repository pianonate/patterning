package patterning.panel

import patterning.Canvas
import patterning.actions.KeyCallback
import patterning.pattern.Pattern
import patterning.pattern.PatternEventType
import processing.event.KeyEvent

class ToggleHighlightControl private constructor(builder: Builder) : Control(builder) {

    init {
        builder.resetOnNew?.registerObserver(PatternEventType.PatternSwapped) {
            // if a new pattern is loaded, reset the control
            // right now we have to update this behavior both in the boolean (such as rotation managing booleans)
            // and in the instantiation of the associated control
            // maybe there's a better way?
            isHighlightFromKeypress = false
        }

        builder.resetRotations?.registerObserver(PatternEventType.ResetRotations) {
            // if we've reset rotations then clear these controls
            isHighlightFromKeypress = false
        }
    }

    override fun onMouseReleased() {
        super.onMouseReleased()
        toggleHighlight()
    }

    override fun onKeyPressed(event: KeyEvent) {
        toggleHighlight()
    }

    class Builder(
        canvas: Canvas,
        callback: KeyCallback,
        iconName: String,
        size: Int,
        val resetOnNew: Pattern? = null,
        val resetRotations: Pattern? = null
    ) :
        Control.Builder(canvas, callback, iconName, size) {
        override fun build() = ToggleHighlightControl(this)
    }

}