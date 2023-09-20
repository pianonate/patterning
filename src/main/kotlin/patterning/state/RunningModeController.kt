package patterning.state

object RunningModeController {

    private var currentRunningState: RunningState = PausedState()
    internal var previousRunningState: RunningState = LoadingState()

    val runningMode: RunningMode
        get() = currentRunningState.runningMode

    val isUXAvailable: Boolean
        get() = currentRunningState.isUXAvailable

    val isPaused: Boolean
        get() = currentRunningState.isPaused

    val isPlaying: Boolean
        get() = currentRunningState.isPlaying

    val isTesting: Boolean
        get() = currentRunningState.isTesting

    fun shouldAdvance(): Boolean {
        return currentRunningState.shouldAdvance()
    }

    fun togglePlayPause() {
        currentRunningState.togglePlayPause()
    }

    fun toggleSingleStep() {
        currentRunningState.toggleSingleStep()
    }

    fun addModeChangeObserver(observer: RunningModeObserver) {
        runningModeObservers.add(observer)
    }

    fun play() {
        currentRunningState.play()
    }

    fun pause() {
        currentRunningState.pause()
    }

    fun load() {
        currentRunningState.load()
    }

    fun test() {
        currentRunningState.test()
    }

    fun endTest() {
        if (currentRunningState is TestingState)
            changeState(previousRunningState)
        else
            throw IllegalStateException("can only end test from within TestState")
    }

    internal fun changeState(newState: RunningState) {

        previousRunningState = currentRunningState
        currentRunningState = newState
        notifyRunningModeChangedObservers()
    }

    private val runningModeObservers = mutableListOf<RunningModeObserver>()

    private fun notifyRunningModeChangedObservers() {
        for (observer in runningModeObservers) {
            observer.onRunningModeChange()
        }
    }
}