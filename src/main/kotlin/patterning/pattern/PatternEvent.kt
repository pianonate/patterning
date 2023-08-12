package patterning.pattern

import patterning.util.FlexibleInteger

sealed class PatternEvent() {
    data class DimensionChanged(val biggestDimension: FlexibleInteger) : PatternEvent()
    data class PatternSwapped(val patternName: String) : PatternEvent()
}