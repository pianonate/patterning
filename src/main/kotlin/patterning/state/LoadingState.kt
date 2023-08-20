package patterning.state

class LoadingState() : RunningState(RunningMode.LOADING) {
    
    override fun togglePlaying() {
        throw IllegalStateException("Cannot toggle running during loading")
    }
    
    override fun toggleSingleStep() {
        throw IllegalStateException("Cannot toggle single step during loading")
    }
    
    // we say "start" instead of "play" because we just continue from whatever mode was last invoked
    // this way we can stay in single step between patterns - or stay paused between patterns
    override fun start() {
        controller.changeState(controller.previousRunningState)
    }
    
    override fun load() {
        throw IllegalStateException("Cannot load if already loading")
    }
    
    override fun shouldAdvance(): Boolean {
        return false
    }
    
    override val isTesting = false
    override val isUXAvailable = false
    override val isPlaying = false
}