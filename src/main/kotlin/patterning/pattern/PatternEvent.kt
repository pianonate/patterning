package patterning.pattern

sealed class PatternEvent {
    data class PatternSwapped(val patternName: String) : PatternEvent()
}