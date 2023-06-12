package ux.panel

import actions.KeyCallback
import ux.Theme
import ux.informer.DrawingInfoSupplier

class ControlPanel internal constructor(builder: Builder) : ContainerPanel(builder) {
    class Builder(drawingInformer: DrawingInfoSupplier?, alignHorizontal: AlignHorizontal?, vAlign: AlignVertical?) :
        ContainerPanel.Builder<Builder>(drawingInformer, alignHorizontal, vAlign) {
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
            addPanel(c!!)
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

        fun addToggleIconControl(
            iconName: String,
            toggledIconName: String,
            callback: KeyCallback,
            modeChangeCallback: KeyCallback
        ): Builder {
            val c: Control = ToggleIconControl.Builder(
                drawingInformer,
                callback,
                modeChangeCallback,
                iconName,
                toggledIconName,
                Theme.controlSize
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