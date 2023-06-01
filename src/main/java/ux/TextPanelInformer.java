package ux;

import processing.core.PGraphics;

public class TextPanelInformer implements InformativePGraphicsSupplier {

    private final PGraphicsSupplier graphicsSupplier;
    private final ResizedSupplier resizedSupplier;
    private final DrawingSupplier drawingSupplier;

    public TextPanelInformer(PGraphicsSupplier graphicsSupplier, ResizedSupplier resizedSupplier, DrawingSupplier drawingSupplier) {
        this.graphicsSupplier = graphicsSupplier;
        this.resizedSupplier = resizedSupplier;
        this.drawingSupplier = drawingSupplier
        ;
    }

    @Override
    public PGraphics get() {
        return graphicsSupplier.get();
    }


    @Override
    public boolean isResized() {
        return resizedSupplier.isResized();
    }

    @Override
    public boolean isDrawing() {
        return drawingSupplier.isWithinBeginDraw();
    }
}
