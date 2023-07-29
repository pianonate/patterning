package patterning.informer

import processing.core.PGraphics

class DrawingInformer(
    private val graphicsSupplier: PGraphicsSupplier,
    private val resizedSupplier: ResizedSupplier,
    private val drawingSupplier: DrawingSupplier
) : DrawingInfoSupplier {
    override fun supplyPGraphics(): PGraphics {
        return graphicsSupplier.get()
    }

    override fun isResized(): Boolean {
        return resizedSupplier.isResized()
    }

    override fun isDrawing(): Boolean {
        return drawingSupplier.isDrawing()
    }
}