package patterning.state

class PausedState() : RunningState(RunningMode.PAUSED) {
    
    override fun togglePlaying() {
        controller.changeState(PlayingState())
    }
    
    override fun toggleSingleStep() {
        controller.changeState(SingleStepState())
    }
    
    override fun start() {
        throw IllegalStateException("Pause is only toggled from play, not set directly")
    }
    
    override fun load() {
        controller.changeState(LoadingState())
    }
    
    override fun shouldAdvance(): Boolean {
        return false
    }
    
    override val isTesting = false
    override val isUXAvailable = true
    override val isPlaying = false
}