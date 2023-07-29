package patterning.informer

import processing.core.PGraphics

interface DrawingInfoSupplier {
    fun supplyPGraphics(): PGraphics
    fun isResized(): Boolean
    fun isDrawing(): Boolean
}