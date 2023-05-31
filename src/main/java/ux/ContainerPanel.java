package ux;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerPanel extends Panel {

    private final List<Panel> childPanels;
    private final Orientation orientation;

    protected ContainerPanel(Builder builder) {
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
            child.graphicsSupplier = this::getContainerPanelBuffer;
        }

        this.orientation = builder.orientation;
        updatePanelSize();

        // Panel's create an initial buffer for their children to draw into
        // however in some cases the child doesn't have a size until
        panelBuffer = getPanelBuffer(graphicsSupplier.get());
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
        public Builder(PGraphicsSupplier graphicsSupplier, AlignHorizontal alignHorizontal, AlignVertical vAlign) {
            super(graphicsSupplier, alignHorizontal, vAlign);
        }

        protected Builder setOrientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        protected Builder addPanel(Panel child) {
            this.childPanels.add(child);
            return this;
        }

        @Override
        protected P self() {
            return (P) this;
        }

        public abstract ContainerPanel build();

    }
}

