package patterning.pattern


sealed class PatternEvent() {
    data class DimensionChanged(val biggestDimension: Long) : PatternEvent()
    data class PatternSwapped(val patternName: String) : PatternEvent()
}