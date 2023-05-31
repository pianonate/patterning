package ux;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public class ContainerPanel extends Panel {

    private final List<Panel> childPanels;
    private final Orientation orientation;

    protected ContainerPanel(Builder builder) {
        super(builder);
        this.childPanels = new ArrayList<>(builder.childPanels);

        // Set parent panel for each child
        for (Panel child : childPanels) {
            // child panels need special handling to orient themselves to this container Panel
            // rather than the UXBUffer, which is the more common case...
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

    public static class Builder extends Panel.Builder<Builder> {
        private Orientation orientation = Orientation.HORIZONTAL;
        private final List<Panel> childPanels = new ArrayList<>();

        // Constructor for aligned Panel with default dimensions (0, 0)
        // addPanel will update the actual dimensions
        public Builder(PGraphicsSupplier graphicsSupplier, AlignHorizontal alignHorizontal, AlignVertical vAlign) {
            super(graphicsSupplier, alignHorizontal, vAlign);
        }
        public Builder setOrientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder addPanel(Panel child) {
            this.childPanels.add(child);
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ContainerPanel build() {
            if (childPanels.isEmpty()) {
                throw new IllegalStateException("ContainerPanel must have at least one child panel");
            }
            return new ContainerPanel(this);
        }
    }
}

