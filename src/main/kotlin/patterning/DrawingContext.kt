package patterning

import processing.core.PGraphics

class DrawingContext(
    private val graphicsSupplier: () -> PGraphics,
) {
    fun getPGraphics(): PGraphics {
        return graphicsSupplier()
    }
}