package patterning.panel

import kotlinx.coroutines.delay
import patterning.Canvas
import patterning.Theme
import patterning.pattern.Command
import patterning.state.RunningModeController
import patterning.state.RunningModeObserver
import patterning.util.AsyncJobRunner
import processing.core.PImage
import processing.event.KeyEvent

class PlayPauseControl(builder: Builder) : Control(builder), RunningModeObserver {

    private val pausedIcon: PImage
    private val playIcon: PImage
    private var currentIcon: PImage
    private var runningModeChanged = false

    init {
        pausedIcon = loadIcon(builder.pausedIconName)
        playIcon = super.icon
        currentIcon = playIcon
        // allows for highlighting play pause whenever the single step mode is changed
        RunningModeController.addModeChangeObserver(this)
    }

    override fun onRunningModeChange() {
        runningModeChanged = true
        highlightFromKeyPress()
        handleIcons()
    }

    override fun getCurrentIcon(): PImage {
        return currentIcon
    }

    private fun handleIcons() {
        currentIcon = pausedIcon
        asyncSetPlayIconJob.cancelAndWait()  // Cancel any running job when playing

        if (!RunningModeController.isPlaying) {
            if (!asyncSetPlayIconJob.isActive) {  // Only start if not already running
                asyncSetPlayIconJob.start()
            }
        }
    }

    override fun onKeyPressed(event: KeyEvent) {
        super.onKeyPressed(event)
        handleIcons()
    }

    override fun onMouseReleased() {
        super.onMouseReleased() // this will change the play pause state
        handleIcons()
    }

    private val asyncSetPlayIconJob = AsyncJobRunner(
        method = suspend {
            delay(Theme.CONTROL_HIGHLIGHT_DURATION.toLong())
            currentIcon = playIcon
        }
    )

    class Builder(
        canvas: Canvas,
        behavior: Command,
        iconName: String,
        val pausedIconName: String,
        size: Int,

        ) : Control.Builder(canvas, behavior, iconName, size) {

        override fun build() = PlayPauseControl(this)
    }
}