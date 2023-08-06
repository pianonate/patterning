package patterning.panel

import kotlinx.coroutines.delay
import patterning.DrawingInformer
import patterning.RunningMode
import patterning.RunningState
import patterning.SingleStepObserver
import patterning.Theme
import patterning.actions.KeyCallback
import patterning.actions.KeyObservable
import patterning.util.AsyncJobRunner
import processing.core.PImage

class PlayPauseControl(builder: Builder) : Control(builder), SingleStepObserver {

    private val pauseIcon: PImage
    private val playIcon: PImage
    private var currentIcon: PImage
    private var runningModeChanged = false

    init {
        pauseIcon = loadIcon(builder.pausedIconName)
        playIcon = super.icon
        currentIcon = playIcon
        // allows for highlighting play pause whenever the single step mode is changed
        RunningState.addModeChangeObserver(this)
    }

    override fun onSingleStepModeChange() {
        runningModeChanged = true
        highlightFromKeyPress()
    }

    override fun getCurrentIcon(): PImage {
        return currentIcon
    }

    override fun notifyKeyPress(observer: KeyObservable) {
        highlightFromKeyPress()

        if (runningModeChanged) {
            when (RunningState.runningMode) {
                RunningMode.SINGLE_STEP -> {
                    currentIcon = playIcon
                }

                else -> toggleIcon()
            }
            runningModeChanged = false
        } else toggleIcon()
    }

    override fun onMouseReleased() {
        super.onMouseReleased() // this will change the play pause state
        toggleIcon()
    }

    private val iconAsyncJobRunner = AsyncJobRunner(
        method = suspend {
            delay(Theme.controlHighlightDuration.toLong())
            currentIcon = playIcon
        },
        threadName = "Icon Toggle Thread"
    )

    private fun toggleIcon() {
        when (RunningState.runningMode) {
            RunningMode.PLAYING, RunningMode.TESTING -> {
                currentIcon = pauseIcon
                iconAsyncJobRunner.cancelAndWait()  // Cancel any running job when playing
            }

            RunningMode.PAUSED, RunningMode.SINGLE_STEP -> {
                currentIcon = pauseIcon
                if (!iconAsyncJobRunner.isActive) {  // Only start if not already running
                    iconAsyncJobRunner.startJob()
                }
            }
        }
    }

    class Builder(
        drawingInformer: DrawingInformer,
        callback: KeyCallback?,
        iconName: String?,
        val pausedIconName: String,
        size: Int,

        ) : Control.Builder(drawingInformer, callback!!, iconName!!, size) {
        override fun self(): Builder {
            return this
        }

        override fun build(): PlayPauseControl {
            return PlayPauseControl(this)
        }
    }
}