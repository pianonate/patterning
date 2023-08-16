package patterning.panel

import patterning.Canvas
import patterning.actions.KeyCallback
import patterning.state.RunningModeController
import patterning.state.RunningModeObserver
import processing.event.KeyEvent

class ToggleHighlightControl private constructor(builder: Builder) : Control(builder), RunningModeObserver {
    
    init {
        RunningModeController.addModeChangeObserver(this)
    }
    
    override fun onRunningModeChange() {
        isHighlightFromKeypress = false
    }
    
    override fun onMouseReleased() {
        super.onMouseReleased()
        toggleHighlight()
    }
    
    override fun onKeyPress(event: KeyEvent) {
        toggleHighlight()
    }
    
    fun reset() {
        isHighlightFromKeypress = false
    }
    
    class Builder(
        canvas: Canvas,
        callback: KeyCallback,
        iconName: String,
        size: Int,
        val resetOnNewPattern: Boolean = false,
    ) :
        Control.Builder(canvas, callback, iconName, size) {
        override fun build() = ToggleHighlightControl(this)
    }

}