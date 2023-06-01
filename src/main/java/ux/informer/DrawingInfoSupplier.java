package ux.informer;

import processing.core.PGraphics;

public interface DrawingInfoSupplier {
    PGraphics getPGraphics();
    boolean isResized();
    boolean isDrawing();
}
