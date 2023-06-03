package ux.panel;

import processing.core.PGraphics;
import ux.informer.DrawingInfoSupplier;
import ux.informer.DrawingInformer;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerPanel extends Panel {

    private final List<Panel> childPanels;
    protected final Orientation orientation;

    protected ContainerPanel(Builder<?> builder) {
        super(builder);

        this.childPanels = new ArrayList<>(builder.childPanels);

        if (childPanels.isEmpty()) {
            throw new IllegalStateException("ContainerPanel must have at least one child panel");
        }

        // Set parent panel for each child
        for (Panel child : childPanels) {
            // child panels need special handling to orient themselves to this container Panel
            // rather than the UXBuffer, which is the more common case...
            child.parentPanel = this;
            child.drawingInformer =
                    new DrawingInformer(this::getContainerPanelBuffer, drawingInformer::isResized, drawingInformer::isDrawing);
        }

        this.orientation = builder.orientation;

        updatePanelSize();

        // super(builder) causes Panel to create an initial panelBuffer
        // to draw into.  However ContainerPanel's don't have a width and height until
        // we've run updatePanelSize as we don't know how
        // many children will get added to a ContainerPanel -
        // given we've already called super(builder), set
        // as the one created in Panel won't work
        // there's probably a better way but i think it can wait
        panelBuffer = getPanelBuffer(drawingInformer.getPGraphics());
    }

    private PGraphics getContainerPanelBuffer() {
        return this.panelBuffer;
    }

    private void updatePanelSize() {
        int totalWidth = 0, totalHeight = 0;
        for (Panel child : childPanels) {
            if (this.orientation == Orientation.HORIZONTAL) {
                child.setPosition(totalWidth, 0);
                totalWidth += child.width;
                totalHeight = Math.max(totalHeight, child.height);
            } else { // Orientation.VERTICAL
                child.setPosition(0, totalHeight);
                totalHeight += child.height;
                totalWidth = Math.max(totalWidth, child.width);
            }
        }

        // Update parent size
        this.width = totalWidth;
        this.height = totalHeight;
    }

    @Override
    protected void panelSubclassDraw() {
        // Draw child panels
        for (Panel child : childPanels) {
            child.draw();
        }
    }

    // public abstract static class Builder extends Panel.Builder<Builder> {
    public abstract static class Builder<P extends Builder<P>> extends Panel.Builder<P> {

        private final List<Panel> childPanels = new ArrayList<>();
        protected Orientation orientation = Orientation.HORIZONTAL;

        // Constructor for aligned Panel with default dimensions (0, 0)
        // addPanel will update the actual dimensions
        public Builder(DrawingInfoSupplier drawingInformer, AlignHorizontal alignHorizontal, AlignVertical vAlign) {
            super(drawingInformer, alignHorizontal, vAlign);
        }

        protected P setOrientation(Orientation orientation) {
            this.orientation = orientation;
            return self();
        }

        @SuppressWarnings("UnusedReturnValue")
        protected P addPanel(Panel child) {
            this.childPanels.add(child);
            return self();
        }

        @Override
        @SuppressWarnings("unchecked")
        protected P self() {
            return (P) this;
        }

        public abstract ContainerPanel build();

    }
}

