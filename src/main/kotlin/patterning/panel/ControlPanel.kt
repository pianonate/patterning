package patterning.panel

import patterning.Canvas
import patterning.DrawingContext
import patterning.Theme
import patterning.actions.KeyCallback

class ControlPanel internal constructor(builder: Builder) : ContainerPanel(builder) {
    
    class Builder(drawingContext: DrawingContext, canvas: Canvas, hAlign: AlignHorizontal, vAlign: AlignVertical) :
        ContainerPanel.Builder(drawingContext, canvas, hAlign, vAlign) {
        
        fun addControl(iconName: String, callback: KeyCallback) = apply {
            val c = Control.Builder(
                drawingContext,
                canvas,
                callback,
                iconName,
                Theme.controlSize
            ).build()
            addPanel(c)
        }
        
        fun addToggleHighlightControl(iconName: String, callback: KeyCallback) = apply {
            val c: Control = ToggleHighlightControl.Builder(
                drawingContext,
                canvas,
                callback,
                iconName,
                Theme.controlSize
            ).build()
            addPanel(c)
        }
        
        fun addPlayPauseControl(
            playIconName: String,
            pauseIconName: String,
            callback: KeyCallback,
        ) = apply {
            val c: Control = PlayPauseControl.Builder(
                drawingContext,
                canvas,
                callback,
                playIconName,
                pauseIconName,
                Theme.controlSize,
            ).build()
            addPanel(c)
        }
        
        override fun build() = ControlPanel(this)
    }
}