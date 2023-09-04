package patterning.panel

import patterning.Canvas
import patterning.Theme
import patterning.actions.KeyCallback
import patterning.pattern.Pattern

class ControlPanel internal constructor(builder: Builder) : ContainerPanel(builder) {

    class Builder(canvas: Canvas, hAlign: AlignHorizontal, vAlign: AlignVertical) :
        ContainerPanel.Builder(canvas, hAlign, vAlign) {

        fun addControl(iconName: String, callback: KeyCallback) = apply {
            val c = Control.Builder(
                canvas,
                callback,
                iconName,
                Theme.controlSize
            ).build()
            addPanel(c)
        }

        fun addToggleHighlightControl(iconName: String, callback: KeyCallback, resetOnNew: Pattern? = null) =
            apply {
                val c: Control = ToggleHighlightControl.Builder(
                    canvas,
                    callback,
                    iconName,
                    Theme.controlSize,
                    resetOnNew = resetOnNew
                ).build()
                addPanel(c)
            }

        fun addPlayPauseControl(
            playIconName: String,
            pauseIconName: String,
            callback: KeyCallback,
        ) = apply {
            val c: Control = PlayPauseControl.Builder(
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