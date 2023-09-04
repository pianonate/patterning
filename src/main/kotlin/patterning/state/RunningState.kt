package patterning.state

abstract class RunningState(val runningMode: RunningMode) {
    internal val controller = RunningModeController
    abstract fun togglePlayPause()
    abstract fun toggleSingleStep()
    abstract fun play()
    abstract fun pause()
    abstract fun load()
    abstract fun test()
    abstract fun shouldAdvance(): Boolean
    abstract val isTesting: Boolean
    abstract val isUXAvailable: Boolean
    abstract val isPlaying: Boolean

}