package patterning

import processing.core.PGraphics

class GraphicsReference(private val initialGraphics: PGraphics) {
    var graphics: PGraphics = initialGraphics
        private set
    
    internal fun updateGraphics(newGraphics: PGraphics) {
        graphics = newGraphics
    }
}