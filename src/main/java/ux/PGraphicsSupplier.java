package ux;

import processing.core.PGraphics;

@FunctionalInterface
public interface PGraphicsSupplier {
    PGraphics get();
}
