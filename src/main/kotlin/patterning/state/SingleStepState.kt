package patterning.state

class SingleStepState() : RunningState(RunningMode.SINGLE_STEP) {
    private var wasSingleStepActivated = false

    override fun togglePlayPause() {
        wasSingleStepActivated = true
    }

    override fun toggleSingleStep() {
        wasSingleStepActivated = false
        controller.changeState(PausedState())
    }

    override fun play() {
        throw IllegalStateException("Cannot toggle run during single step mode")
    }

    override fun pause() {
        throw IllegalStateException("Cannot toggle pause during single step mode")
    }

    override fun load() {
        controller.changeState(LoadingState())
    }

    override fun test() {
        controller.changeState(TestingState())
    }

    override fun shouldAdvance(): Boolean {
        return if (wasSingleStepActivated) {
            wasSingleStepActivated = false
            true
        } else {
            false
        }
    }

    override val isTesting = false
    override val isUXAvailable = true
    override val isPlaying = false
    override val isPaused = false
}