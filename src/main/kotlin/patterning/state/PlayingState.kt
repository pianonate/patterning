package patterning.state

class PlayingState() : RunningState(RunningMode.PLAYING) {
    
    override fun toggleRunning() {
        controller.changeState(PausedState())
    }
    
    override fun toggleSingleStep() {
        controller.changeState(SingleStepState())
    }
    
    override fun play() {
        throw IllegalStateException("Play is only toggled from pause, not set directly")
    }
    
    override fun load() {
        controller.changeState(LoadingState())
    }
    
    override fun shouldAdvance(): Boolean {
        return true
    }
    
    override val isTesting = false
    override val isUXAvailable = true
    override val isPlaying = true
}