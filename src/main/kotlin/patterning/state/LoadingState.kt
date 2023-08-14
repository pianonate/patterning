package patterning.state

class LoadingState() : RunningState(RunningMode.LOADING) {
    
    override fun toggleRunning() {
        throw IllegalStateException("Cannot toggle running during loading")
    }
    
    override fun toggleSingleStep() {
        throw IllegalStateException("Cannot toggle single step during loading")
    }
    
    override fun play() {
        controller.changeState(PlayingState())
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