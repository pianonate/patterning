package patterning.state

enum class RunningMode(val value: String) {
    LOADING("loading"),
    PLAYING("playing"),
    PAUSED("paused"),
    SINGLE_STEP("single step"),
    TESTING("testing");

    override fun toString(): String {
        return value
    }
}