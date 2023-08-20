package patterning.state

abstract class RunningState(val runningMode: RunningMode) {
    internal val controller = RunningModeController
    abstract fun togglePlaying()
    abstract fun toggleSingleStep()
    abstract fun start()
    abstract fun load()
    abstract fun shouldAdvance(): Boolean
    abstract val isTesting: Boolean
    abstract val isUXAvailable: Boolean
    abstract val isPlaying: Boolean
    
}