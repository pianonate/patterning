package patterning

interface TestModeObserver {
    /**
     * Called when the running state enters test mode.
     *
     * Implementations of this method are required to call
     * `RunningState.endTest()` when their operations are complete.
     */
    fun onTestModeEnter()
}