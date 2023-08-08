package patterning

import processing.core.PGraphics

class DrawingInformer(
    private val graphicsSupplier: () -> PGraphics,
    private val resizedSupplier: () -> Boolean,
) {
    fun getPGraphics(): PGraphics {
        return graphicsSupplier()
    }
    
    fun isResized(): Boolean {
        return resizedSupplier()
    }
}