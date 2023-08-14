package patterning

object RunningState {
    
    val runningMode: RunningMode
        get() = currentRunningMode
    
    private val runningModeObservers = mutableListOf<RunningModeObserver>()
    private val testModeObservers = mutableListOf<TestModeObserver>()
    
    private var currentRunningMode = RunningMode.PAUSED
    private var previousRunningMode = RunningMode.PAUSED
    private var wasSingleStepActivated = false
    
    fun toggleRunning() {
        when (currentRunningMode) {
            RunningMode.PAUSED -> {
                currentRunningMode = RunningMode.PLAYING
                notifyRunningModeChangedObservers()
            }
            
            RunningMode.PLAYING -> {
                currentRunningMode = RunningMode.PAUSED
                notifyRunningModeChangedObservers()
            }
            
            RunningMode.SINGLE_STEP -> {
                wasSingleStepActivated = true
            }
            
            RunningMode.TESTING -> throw IllegalStateException("Cannot toggle running state during testing.")
        }
    }
    
    fun toggleSingleStep() {
        if (currentRunningMode == RunningMode.TESTING)
            throw IllegalStateException("Cannot toggle running state during testing.")
        
        currentRunningMode = if (currentRunningMode == RunningMode.SINGLE_STEP) {
            RunningMode.PAUSED
        } else {
            RunningMode.SINGLE_STEP
        }
        wasSingleStepActivated = false
        notifyRunningModeChangedObservers()  // Add this line to notify PlayPauseControl when toggleSingleStep() is called.
    }
    
    fun addModeChangeObserver(observer: RunningModeObserver) {
        runningModeObservers.add(observer)
    }
    
    
    private fun notifyRunningModeChangedObservers() {
        for (observer in runningModeObservers) {
            observer.onRunningModeChange()
        }
    }
    
    fun run() {
        if (currentRunningMode == RunningMode.SINGLE_STEP) return
        if (currentRunningMode == RunningMode.TESTING) throw IllegalStateException("Cannot run when in test mode.")
        if (currentRunningMode != RunningMode.PLAYING) notifyRunningModeChangedObservers()
        currentRunningMode = RunningMode.PLAYING
    }
    
    fun pause() {
        if (currentRunningMode == RunningMode.TESTING) throw IllegalStateException("Cannot run when in test mode.")
        if (currentRunningMode != RunningMode.SINGLE_STEP) {
            currentRunningMode = RunningMode.PAUSED
            notifyRunningModeChangedObservers()
        }
    }
    
    fun addTestModeObserver(observer: TestModeObserver) {
        testModeObservers.add(observer)
    }
    
    fun test() {
        previousRunningMode = currentRunningMode
        currentRunningMode = RunningMode.TESTING
        for (observer in testModeObservers) {
            observer.onTestModeEnter()
        }
    }
    
    fun runMessage(): String {
        return when (currentRunningMode) {
            RunningMode.PLAYING -> "running"
            RunningMode.SINGLE_STEP -> "step"
            RunningMode.PAUSED -> "paused"
            RunningMode.TESTING -> "testing"
        }
    }
    
    fun shouldAdvance(): Boolean {
        return when (currentRunningMode) {
            
            RunningMode.SINGLE_STEP -> {
                if (wasSingleStepActivated) {
                    wasSingleStepActivated = false
                    true
                } else {
                    false
                }
            }
            
            RunningMode.PAUSED -> false
            RunningMode.PLAYING -> true
            RunningMode.TESTING -> true
        }
    }
    
    fun endTest() {
        when (previousRunningMode) {
            RunningMode.PLAYING -> run()
            RunningMode.PAUSED -> pause()
            RunningMode.SINGLE_STEP -> toggleSingleStep()
            RunningMode.TESTING -> throw IllegalStateException("Cannot end test mode when not in test mode.")
        }
    }
}