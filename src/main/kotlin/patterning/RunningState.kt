package patterning

enum class RunningMode {
    PLAYING,
    PAUSED,
    SINGLE_STEP
}

object RunningState {

    val runningMode: RunningMode
        get() = currentRunningMode

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

    fun toggleSingleStep() {
        currentRunningMode = if (currentRunningMode == RunningMode.SINGLE_STEP) {
            RunningMode.PAUSED
        } else {
            RunningMode.SINGLE_STEP
        }
        wasSingleStepActivated = false
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