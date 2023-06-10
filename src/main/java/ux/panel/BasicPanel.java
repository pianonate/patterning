package ux.panel;

import ux.informer.DrawingInfoSupplier;

public class BasicPanel extends Panel {
    protected BasicPanel(Builder builder) {
        super(builder);
    }

    @Override
    protected void panelSubclassDraw() {
        ;
    }

    public static class Builder extends Panel.Builder<Builder> {

        public Builder(DrawingInfoSupplier drawingInfoSupplier, AlignHorizontal alignHorizontal, AlignVertical vAlign, int width, int height) {
            super(drawingInfoSupplier, alignHorizontal, vAlign, width, height);
        }

        @Override
        public BasicPanel build() {
            return new BasicPanel(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}