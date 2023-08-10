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
    
    override val observers: MutableList<(FlexibleInteger) -> Unit> = mutableListOf()
    
    
    // currently these are the only methods called by PatterningPApplet so we
    // require all Patterns to implement them
    abstract fun draw()
    abstract fun move(dx: Float, dy: Float)
    abstract fun updateProperties()
    
    override fun registerObserver(observer: (FlexibleInteger) -> Unit) {
        observers.add(observer)
    }
    
    override fun notifyPatternObservers(biggestDimension: FlexibleInteger) {
        observers.forEach { it(biggestDimension) }
    }
    
    protected fun patternChanged() {
        canvas.resetMathContext()
    }
}