package patterning.state

class LoadingState() : RunningState(RunningMode.LOADING) {

    override fun togglePlayPause() {
        throw IllegalStateException("Cannot toggle running during loading")
    }

    override fun toggleSingleStep() {
        throw IllegalStateException("Cannot toggle single step during loading")
    }

    // we say "start" instead of "play" because we just continue from whatever mode was last invoked
    // this way we can stay in single step between patterns - or stay paused between patterns
    override fun play() {
        start(RunningMode.PLAYING)
    }

    override fun pause() {
        start(RunningMode.PAUSED)
    }

    override fun load() {
        throw IllegalStateException("Cannot load if already loading")
    }

    override fun test() {
        controller.changeState(TestingState())
    }

    override fun shouldAdvance(): Boolean {
        return false
    }

    override val isTesting = false
    override val isUXAvailable = false
    override val isPlaying = false
    override val isPaused = true


    private fun start(startWith: RunningMode) {
        if (controller.previousRunningState is SingleStepState)
            controller.changeState(SingleStepState())
        else
            when (startWith) {
                RunningMode.PLAYING -> controller.changeState(PlayingState())
                RunningMode.PAUSED -> controller.changeState(PlayingState())
                else -> throw IllegalStateException("Cannot start with $startWith")
            }
    }
}