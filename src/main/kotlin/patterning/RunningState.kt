package patterning

enum class RunningMode {
    PLAYING,
    PAUSED,
    SINGLE_STEP
}

interface RunningModeObserver {
    fun onRunningModeChange()
}

object RunningState {

    val runningMode: RunningMode
        get() = currentRunningMode

    private val observers = mutableListOf<RunningModeObserver>()
    private var currentRunningMode = RunningMode.PAUSED
    private var wasSingleStepActivated = false

    fun toggleRunning() {
        when (currentRunningMode) {
            RunningMode.PAUSED -> currentRunningMode = RunningMode.PLAYING
            RunningMode.PLAYING -> currentRunningMode = RunningMode.PAUSED
            RunningMode.SINGLE_STEP -> {
                wasSingleStepActivated = true
            }
        }
    }

    fun toggleRunnningMode() {
        currentRunningMode = if (currentRunningMode == RunningMode.SINGLE_STEP) {
            RunningMode.PAUSED
        } else {
            RunningMode.SINGLE_STEP
        }
        wasSingleStepActivated = false
        notifyObservers()  // Add this line to notify PlayPauseControl when toggleSingleStep() is called.
    }

    fun addObserver(observer: RunningModeObserver) {
        observers.add(observer)
    }


    private fun notifyObservers() {
        for (observer in observers) {
            observer.onRunningModeChange()
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

    fun runMessage(): String {
        return when (currentRunningMode) {
            RunningMode.PLAYING -> "running"
            RunningMode.SINGLE_STEP -> "step"
            RunningMode.PAUSED -> "paused"
        }
    }

    fun shouldAdvance(): Boolean {
        return when (currentRunningMode) {
            RunningMode.PLAYING, RunningMode.SINGLE_STEP -> {
                if (currentRunningMode == RunningMode.PLAYING || wasSingleStepActivated) {
                    if (currentRunningMode == RunningMode.SINGLE_STEP) {
                        wasSingleStepActivated = false
                    }
                    true
                } else {
                    false
                }
            }

            RunningMode.PAUSED -> false
        }
    }

}