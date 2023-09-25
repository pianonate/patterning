package patterning.pattern

sealed class PatternEvent {
    data class ResetRotations(val reset:Boolean) : PatternEvent()
    data class PatternSwapped(val patternName: String) : PatternEvent()
}