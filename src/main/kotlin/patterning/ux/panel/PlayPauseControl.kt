package patterning.ux.panel

import kotlinx.coroutines.*
import patterning.RunningState
import patterning.actions.KeyCallback
import patterning.actions.KeyObservable
import patterning.ux.Theme
import patterning.ux.informer.DrawingInfoSupplier
import processing.core.PImage

class PlayPauseControl(builder: Builder) : Control(builder), CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val pauseIcon: PImage
    private val playIcon: PImage
    private var currentIcon: PImage
    private val modeChangeCallback: KeyCallback
    private val getRunningState: () -> RunningState

    init {
        pauseIcon = loadIcon(builder.pausedIconName)
        playIcon = super.icon
        modeChangeCallback = builder.modeChangeCallback
        modeChangeCallback.addObserver(this)
        currentIcon = playIcon
        getRunningState = builder.getRunningState
    }

    override fun getCurrentIcon(): PImage {
        return currentIcon
    }

    override fun notifyKeyPress(observer: KeyObservable) {
        highlightFromKeyPress()

        if (observer.invokeModeChange()) {
            when (getRunningState()) {
                RunningState.SINGLE_STEP -> {
                    currentIcon = playIcon
                }
                else -> toggleIcon()
            }
        } else toggleIcon()
    }

    override fun onMouseReleased() {
        super.onMouseReleased() // this will change the play pause state
/*        if (patterning.ux.PatternDrawer.countdownInterrupted) {
            patterning.ux.PatternDrawer.countdownInterrupted = false
            return
        }*/
        toggleIcon()
    }

    private var timerJob: Job? = null

    private fun toggleIcon() {
        when (getRunningState()) {
            RunningState.PLAYING -> {
                currentIcon = pauseIcon
            }

            RunningState.PAUSED, RunningState.SINGLE_STEP -> {
                currentIcon = pauseIcon
                timerJob?.cancel()
                timerJob = launch {
                    delay(Theme.singleModeToggleDuration.toLong())
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
        val modeChangeCallback: KeyCallback,
        iconName: String?,
        val pausedIconName: String,
        size: Int,
        val getRunningState: () -> RunningState

    ) : Control.Builder(drawingInformer, callback!!, iconName!!, size) {
        override fun self(): Builder {
            return this
        }

        override fun build(): PlayPauseControl {
            return PlayPauseControl(this)
        }
    }
}