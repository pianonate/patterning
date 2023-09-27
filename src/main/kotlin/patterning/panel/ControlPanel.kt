package patterning.panel

import patterning.Canvas
import patterning.Theme
import patterning.pattern.Command

class ControlPanel internal constructor(builder: Builder) : ContainerPanel(builder) {

    class Builder(canvas: Canvas, hAlign: AlignHorizontal, vAlign: AlignVertical) :
        ContainerPanel.Builder(canvas, hAlign, vAlign) {

        fun addControl(iconName: String, command: Command) = apply {
            val c = Control.Builder(
                canvas,
                command,
                iconName,
                Theme.CONTROL_SIZE
            ).build()
            addPanel(c)
        }

        fun addToggleHighlightControl(iconName: String, command: Command) =
            apply {
                val c: Control = ToggleHighlightControl.Builder(
                    canvas,
                    command,
                    iconName,
                    Theme.CONTROL_SIZE,
                ).build()
                addPanel(c)
            }

        fun addPlayPauseControl(
            playIconName: String,
            pauseIconName: String,
            command: Command,
        ) = apply {
            val c: Control = PlayPauseControl.Builder(
                canvas,
                command,
                playIconName,
                pauseIconName,
                Theme.CONTROL_SIZE,
            ).build()
            addPanel(c)
        }

        override fun build() = ControlPanel(this)
    }
}