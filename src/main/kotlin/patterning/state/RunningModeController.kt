package patterning.state

object RunningModeController {
    
    private var currentRunningState: RunningState = PausedState()
    private var previousRunningState: RunningState = PausedState()
    
    val runningMode: RunningMode
        get() = currentRunningState.runningMode
    
    val isUXAvailable: Boolean
        get() = currentRunningState.isUXAvailable
    
    val isPlaying: Boolean
        get() = currentRunningState.isPlaying
    
    val isTesting: Boolean
        get() = currentRunningState.isTesting
    
    fun toggleRunning() {
        currentRunningState.toggleRunning()
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
    
    fun load() {
        currentRunningState.load()
    }
    
    
    fun shouldAdvance(): Boolean {
        return currentRunningState.shouldAdvance()
    }
    
    fun test() {
        enterTestMode()
    }
    
    fun endTest() {
        exitTestMode()
    }
    
    internal fun changeState(newState: RunningState) {
        currentRunningState = newState
        notifyRunningModeChangedObservers()
    }
    
    private val runningModeObservers = mutableListOf<RunningModeObserver>()
    
    private fun notifyRunningModeChangedObservers() {
        for (observer in runningModeObservers) {
            observer.onRunningModeChange()
        }
    }
    
    private fun enterTestMode() {
        previousRunningState = currentRunningState
        changeState(TestingState())
    }
    
    private fun exitTestMode() {
        changeState(previousRunningState)
    }
    
}