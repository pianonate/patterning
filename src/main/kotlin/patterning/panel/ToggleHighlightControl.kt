package patterning.panel

import patterning.Canvas
import patterning.pattern.Command

class ToggleHighlightControl private constructor(builder: Builder) : Control(builder) {

    override fun updateEnabled() {
        isEnabled = controlCommand.isEnabled
    }

    class Builder(
        canvas: Canvas,
        command: Command,
        iconName: String,
        size: Int,
    ) :
        Control.Builder(canvas, command, iconName, size) {
        override fun build() = ToggleHighlightControl(this)
    }

}