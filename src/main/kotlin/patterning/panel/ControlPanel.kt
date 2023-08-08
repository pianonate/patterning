package patterning.panel

import patterning.DrawingInformer
import patterning.Theme
import patterning.actions.KeyCallback

class ControlPanel internal constructor(builder: Builder) : ContainerPanel(builder) {
    
    class Builder(drawingInformer: DrawingInformer, hAlign: AlignHorizontal, vAlign: AlignVertical) :
        ContainerPanel.Builder<Builder>(drawingInformer, hAlign, vAlign) {
        public override fun setOrientation(orientation: Orientation): Builder {
            this.orientation = orientation
            return this
        }
        
        fun addControl(iconName: String, callback: KeyCallback): Builder {
            val c = Control.Builder(
                drawingInformer,
                callback,
                iconName,
                Theme.controlSize
            ).build()
            addPanel(c)
            return this
        }
        
        fun addToggleHighlightControl(iconName: String, callback: KeyCallback): Builder {
            val c: Control = ToggleHighlightControl.Builder(
                drawingInformer,
                callback,
                iconName,
                Theme.controlSize
            ).build()
            addPanel(c)
            return this
        }
        
        fun addPlayPauseControl(
            playIconName: String,
            pauseIconName: String,
            callback: KeyCallback,
        ): Builder {
            val c: Control = PlayPauseControl.Builder(
                drawingInformer,
                callback,
                playIconName,
                pauseIconName,
                Theme.controlSize,
            ).build()
            addPanel(c)
            return this
        }
        
        override fun build(): ControlPanel {
            return ControlPanel(this)
        }
        
        override fun self(): Builder {
            return this
        }
    }
}