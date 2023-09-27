package patterning.panel

import patterning.Canvas
import patterning.actions.KeyCallback

class ToggleHighlightControl private constructor(builder: Builder) : Control(builder) {

    override fun updateEnabled() {
        isEnabled = keyCallback.isEnabled
    }

    class Builder(
        canvas: Canvas,
        callback: KeyCallback,
        iconName: String,
        size: Int,
    ) :
        Control.Builder(canvas, callback, iconName, size) {
        override fun build() = ToggleHighlightControl(this)
    }

}