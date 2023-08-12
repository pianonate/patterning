package patterning.pattern

interface ObservablePattern {
    val observers: MutableMap<PatternEventType, MutableList<(PatternEvent) -> Unit>>
    
    fun registerObserver(eventType: PatternEventType, observer: (PatternEvent) -> Unit)
    fun notifyPatternObservers(eventType: PatternEventType, event: PatternEvent)
}