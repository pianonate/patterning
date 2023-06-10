package ux.informer

import processing.core.PGraphics

class DrawingInformer(
    private val graphicsSupplier: PGraphicsSupplier,
    private val resizedSupplier: ResizedSupplier,
    private val drawingSupplier: DrawingSupplier
) : DrawingInfoSupplier {
    override fun getPGraphics(): PGraphics? {
        return graphicsSupplier.get()
    }

    override fun isResized(): Boolean {
        return resizedSupplier.isResized()
    }

    override fun isDrawing(): Boolean {
        return drawingSupplier.isDrawing()
    }
}