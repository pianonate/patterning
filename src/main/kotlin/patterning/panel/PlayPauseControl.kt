package patterning.panel

import kotlinx.coroutines.delay
import patterning.Canvas
import patterning.RunningMode
import patterning.RunningModeObserver
import patterning.RunningState
import patterning.Theme
import patterning.actions.KeyCallback
import patterning.util.AsyncJobRunner
import processing.core.PImage
import processing.event.KeyEvent

class PlayPauseControl(builder: Builder) : Control(builder), RunningModeObserver {
    
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
    
    override fun onRunningModeChange() {
        runningModeChanged = true
        highlightFromKeyPress()
        handleIcons()
    }
    
    override fun getCurrentIcon(): PImage {
        return currentIcon
    }
    
    private fun handleIcons() {
        
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
    
    override fun onKeyEvent(event: KeyEvent) {
        highlightFromKeyPress()
        handleIcons()
    }
    
    override fun onMouseReleased() {
        super.onMouseReleased() // this will change the play pause state
        handleIcons()
    }
    
    private val iconAsyncJobRunner = AsyncJobRunner(
        method = suspend {
            delay(Theme.controlHighlightDuration.toLong())
            currentIcon = playIcon
        }
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
        canvas: Canvas,
        callback: KeyCallback,
        iconName: String,
        val pausedIconName: String,
        size: Int,
        
        ) : Control.Builder(canvas, callback, iconName, size) {
        
        override fun build() = PlayPauseControl(this)
    }
}