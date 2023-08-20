package patterning.state

object RunningModeController {
    
    private var currentRunningState: RunningState = PausedState()
    internal var previousRunningState: RunningState = PausedState()
    
    val runningMode: RunningMode
        get() = currentRunningState.runningMode
    
    val isUXAvailable: Boolean
        get() = currentRunningState.isUXAvailable
    
    val isPlaying: Boolean
        get() = currentRunningState.isPlaying
    
    val isTesting: Boolean
        get() = currentRunningState.isTesting
    
    fun toggleRunning() {
        currentRunningState.togglePlaying()
    }
    
    fun toggleSingleStep() {
        currentRunningState.toggleSingleStep()
    }
    
    fun addModeChangeObserver(observer: RunningModeObserver) {
        runningModeObservers.add(observer)
    }
    
    fun start() {
        currentRunningState.start()
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
    
    private fun enterTestMode() {
        changeState(TestingState())
    }
    
    private fun exitTestMode() {
        changeState(previousRunningState)
    }
    
}