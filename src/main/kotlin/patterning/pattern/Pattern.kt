package patterning.pattern

import patterning.Canvas
import patterning.Properties
import patterning.util.FlexibleInteger
import processing.core.PApplet

abstract class Pattern(
    val pApplet: PApplet,
    val canvas: Canvas,
    val properties: Properties
) : ObservablePattern {
    
    override val observers: MutableMap<PatternEventType, MutableList<(PatternEvent) -> Unit>> = mutableMapOf()
    
    // currently these are the only methods called by PatterningPApplet so we
    // require all Patterns to implement them
    abstract fun draw()
    abstract fun getHUDMessage(): String
    abstract fun handlePlay()
    abstract fun loadPattern()
    abstract fun move(dx: Float, dy: Float)
    abstract fun updateProperties()
    
    override fun registerObserver(eventType: PatternEventType, observer: (PatternEvent) -> Unit) {
        observers.getOrPut(eventType) { mutableListOf() }.add(observer)
    }
    
    override fun notifyPatternObservers(eventType: PatternEventType, event: PatternEvent) {
        observers[eventType]?.forEach { it(event) }
    }
    
    fun onBiggestDimensionChanged(biggestDimension: FlexibleInteger) {
        notifyPatternObservers(PatternEventType.DimensionChanged, PatternEvent.DimensionChanged(biggestDimension))
    }
    
    fun onNewPattern(patternName: String) {
        notifyPatternObservers(PatternEventType.PatternSwapped, PatternEvent.PatternSwapped(patternName))
    }
}