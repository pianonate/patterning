package patterning.pattern

interface NumberedPatternLoader {
    fun setRandom()
    fun setNumberedPattern(number:Int, testing: Boolean = false)
}