package patterning

object RunningState {
    
    val runningMode: RunningMode
        get() = currentRunningMode
    
    private val singleStepObservers = mutableListOf<SingleStepObserver>()
    private val testModeObservers = mutableListOf<TestModeObserver>()
    
    
    private var currentRunningMode = RunningMode.PAUSED
    private var previousRunningMode = RunningMode.PAUSED
    private var wasSingleStepActivated = false
    
    fun toggleRunning() {
        when (currentRunningMode) {
            RunningMode.PAUSED -> currentRunningMode = RunningMode.PLAYING
            RunningMode.PLAYING -> currentRunningMode = RunningMode.PAUSED
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
        notifySingleStepObservers()  // Add this line to notify PlayPauseControl when toggleSingleStep() is called.
    }
    
    fun addModeChangeObserver(observer: SingleStepObserver) {
        singleStepObservers.add(observer)
    }
    
    
    private fun notifySingleStepObservers() {
        for (observer in singleStepObservers) {
            observer.onSingleStepModeChange()
        }
    }
    
    fun run() {
        if (currentRunningMode == RunningMode.SINGLE_STEP) return
        currentRunningMode = RunningMode.PLAYING
    }
    
    fun pause() {
        if (currentRunningMode != RunningMode.SINGLE_STEP)
            currentRunningMode = RunningMode.PAUSED
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