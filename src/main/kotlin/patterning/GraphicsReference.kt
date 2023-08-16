package patterning

import processing.core.PGraphics

class GraphicsReference(
    initialGraphics: PGraphics,
    val name: String,
    val isResizable: Boolean = false,
    val useOpenGL: Boolean = false
) {
    var graphics: PGraphics = initialGraphics
        private set
    
    internal fun updateGraphics(newGraphics: PGraphics) {
        graphics = newGraphics
    }
}