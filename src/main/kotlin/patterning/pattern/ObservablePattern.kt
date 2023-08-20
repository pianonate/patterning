package patterning.pattern

interface ObservablePattern {
    fun registerObserver(eventType: PatternEventType, observer: (PatternEvent) -> Unit)
    fun notifyObservers(eventType: PatternEventType, event: PatternEvent)
}