package patterning

import processing.core.PGraphics

class DrawBuffer(private val initialGraphics: PGraphics) {
    var graphics: PGraphics = initialGraphics
        private set // Only methods inside BufferReference can modify it.
    
    // This method can only be accessed by classes inside this file (like Canvas).
    internal fun updateGraphics(newGraphics: PGraphics) {
        graphics = newGraphics
    }
}