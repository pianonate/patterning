package patterning.pattern

import patterning.util.FlexibleInteger

interface ObservablePattern {
    val observers: MutableList<(FlexibleInteger) -> Unit>
    fun registerObserver(observer: (FlexibleInteger) -> Unit)
    fun notifyPatternObservers(biggestDimension: FlexibleInteger)
}