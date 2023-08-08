package patterning

import processing.core.PGraphics

class DrawingInformer(
    private val graphicsSupplier: () -> PGraphics,
) {
    fun getPGraphics(): PGraphics {
        return graphicsSupplier()
    }
}