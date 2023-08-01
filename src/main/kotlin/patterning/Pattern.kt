package patterning

import processing.core.PApplet

abstract class Pattern(
    val pApplet: PApplet,
    val properties: Properties
) {
    // currently these are the only methods called by PatterningPApplet so we
    // require all Patterns to implement them
    abstract fun draw()
    abstract fun move(dx: Float, dy: Float)
    abstract fun shutdownAsyncJobRunner()
    abstract fun updateProperties()
}