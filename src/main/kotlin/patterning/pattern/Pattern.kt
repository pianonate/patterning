package patterning.pattern

import patterning.Canvas
import patterning.Properties
import patterning.Theme
import patterning.util.FlexibleInteger
import processing.core.PApplet
import processing.core.PGraphics

abstract class Pattern(
    val pApplet: PApplet,
    val canvas: Canvas,
    val properties: Properties
) : ObservablePattern {
    
    private val observers: MutableMap<PatternEventType, MutableList<(PatternEvent) -> Unit>> = mutableMapOf()
    
    private interface GhostState {
        fun update(graphics: PGraphics)
        fun transition()
    }
    
    private var ghostState: GhostState = GhostOff()
    
    init {
        registerObserver(PatternEventType.PatternSwapped) { _ -> resetOnNewPattern() }
    }
    
    // currently these are the only methods called by PatterningPApplet so we
    // require all Patterns to implement them
    abstract fun draw()
    abstract fun getHUDMessage(): String
    abstract fun handlePlayPause()
    abstract fun loadPattern()
    abstract fun move(dx: Float, dy: Float)
    abstract fun updateProperties()
    
    final override fun registerObserver(eventType: PatternEventType, observer: (PatternEvent) -> Unit) {
        observers.getOrPut(eventType) { mutableListOf() }.add(observer)
    }
    
    final override fun notifyObservers(eventType: PatternEventType, event: PatternEvent) {
        observers[eventType]?.forEach { it(event) }
    }
    
    private fun resetOnNewPattern() {
        ghostState = GhostOff()
    }
    
    protected fun updateGhost(graphics: PGraphics) {
        ghostState.update(graphics)
    }
    
    fun onBiggestDimensionChanged(biggestDimension: FlexibleInteger) {
        notifyObservers(PatternEventType.DimensionChanged, PatternEvent.DimensionChanged(biggestDimension))
    }
    
    fun onNewPattern(patternName: String) {
        notifyObservers(PatternEventType.PatternSwapped, PatternEvent.PatternSwapped(patternName))
    }
    
    fun toggleGhost() {
        ghostState.transition()
    }
    
    fun stampGhostModeKeyFrame() {
        ghostState = GhostKeyFrame()
    }
    
    private inner class GhostOff : GhostState {
        override fun update(graphics: PGraphics) {
            graphics.clear()
            graphics.hint(PGraphics.DISABLE_DEPTH_TEST)
            graphics.fill(Theme.cellColor)
        }
        
        override fun transition() = run { ghostState = Ghosting() }
    }
    
    private inner class GhostKeyFrame : GhostState {
        private var emitted = false
        override fun update(graphics: PGraphics) {
            if (emitted) {
                transition()
                return
            }
            
            graphics.hint(PGraphics.DISABLE_DEPTH_TEST)
            graphics.fill(Theme.cellColor)
            emitted = true
        }
        
        override fun transition() = run { ghostState = Ghosting() }
    }
    
    
    private inner class Ghosting : GhostState {
        override fun update(graphics: PGraphics) {
            graphics.hint(PGraphics.DISABLE_DEPTH_TEST)
            graphics.fill(Theme.ghostColor)
        }
        
        override fun transition() = run { ghostState = GhostOff() }
    }
}