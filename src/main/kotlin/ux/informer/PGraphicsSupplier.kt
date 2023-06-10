package ux.informer

import processing.core.PGraphics

fun interface PGraphicsSupplier {
    fun get(): PGraphics?
}