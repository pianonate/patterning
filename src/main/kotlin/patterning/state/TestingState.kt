package patterning.state

class TestingState() : RunningState(RunningMode.TESTING) {
    
    override fun togglePlaying() {
        throw IllegalStateException("Cannot toggle running mode during test mode")
    }
    
    override fun toggleSingleStep() {
        throw IllegalStateException("Cannot toggle single step mode during test mode")
    }
    
    override fun start() {
        throw IllegalStateException("Cannot play when in test mode")
    }
    
    override fun load() {
        throw IllegalStateException("Cannot load when in test mode")
    }
    
    override fun shouldAdvance(): Boolean {
        return true
    }
    
    override val isTesting = true
    override val isUXAvailable = true
    override val isPlaying = false
}