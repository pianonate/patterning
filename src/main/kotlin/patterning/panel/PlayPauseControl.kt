package patterning.panel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import patterning.RunningMode
import patterning.RunningState
import patterning.SingleStepObserver
import patterning.Theme
import patterning.actions.KeyCallback
import patterning.actions.KeyObservable
import patterning.informer.DrawingInfoSupplier
import processing.core.PImage

class PlayPauseControl(builder: Builder) : Control(builder), SingleStepObserver,
    CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val pauseIcon: PImage
    private val playIcon: PImage
    private var currentIcon: PImage
    private var runningModeChanged = false

    init {
        pauseIcon = loadIcon(builder.pausedIconName)
        playIcon = super.icon
        currentIcon = playIcon
        // allows for highlighting playpause whenever the single step mode is changed
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

    private var timerJob: Job? = null

    private fun toggleIcon() {
        when (RunningState.runningMode) {
            RunningMode.PLAYING, RunningMode.TESTING -> {
                currentIcon = pauseIcon
            }

            RunningMode.PAUSED, RunningMode.SINGLE_STEP -> {
                currentIcon = pauseIcon
                timerJob?.cancel()
                timerJob = launch {
                    delay(Theme.controlHighlightDuration.toLong())
                    currentIcon = playIcon
                }
            }
        }
    }

    fun cleanup() {
        cancel() // This cancels all coroutines launched by this CoroutineScope.
    }

    class Builder(
        drawingInformer: DrawingInfoSupplier?,
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