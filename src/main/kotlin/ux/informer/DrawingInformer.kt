package ux.informer;

import processing.core.PGraphics;

public class DrawingInformer implements DrawingInfoSupplier {

    private final PGraphicsSupplier graphicsSupplier;
    private final ResizedSupplier resizedSupplier;
    private final DrawingSupplier drawingSupplier;

    public DrawingInformer(PGraphicsSupplier graphicsSupplier,
                           ResizedSupplier resizedSupplier,
                           DrawingSupplier drawingSupplier)
    {
        this.graphicsSupplier = graphicsSupplier;
        this.resizedSupplier = resizedSupplier;
        this.drawingSupplier = drawingSupplier;
    }



    @Override
    public PGraphics getPGraphics() {
        return graphicsSupplier.get();
    }


    @Override
    public boolean isResized() {
        return resizedSupplier.isResized();
    }

    @Override
    public boolean isDrawing() {
        return drawingSupplier.isDrawing();
    }
}
