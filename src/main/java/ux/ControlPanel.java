package ux;


import actions.KeyCallback;

public class ControlPanel extends ContainerPanel{

    ControlPanel(Builder builder){
        super(builder);
    }

    public static class Builder extends ContainerPanel.Builder<Builder>{
        public Builder(PGraphicsSupplier graphicsSupplier, AlignHorizontal alignHorizontal, AlignVertical vAlign) {
            super(graphicsSupplier, alignHorizontal, vAlign);
        }

        public Builder setOrientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder addControl(String iconName, KeyCallback callback ) {

            Control c = new Control.Builder(
                    this.graphicsSupplier,
                    callback,
                    iconName,
                    UXThemeManager.getInstance().getControlSize()
            ).build();

            addPanel(c);

            return this;
        }

        public Builder addToggleHighlightControl(String iconName, KeyCallback callback ) {

            Control c = new ToggleHighlightControl.Builder(
                    this.graphicsSupplier,
                    callback,
                    iconName,
                    UXThemeManager.getInstance().getControlSize()
            ).build();

            addPanel(c);

            return this;
        }

        public Builder addToggleIconControl(String iconName, String toggledIconName, KeyCallback callback ) {

            Control c = new ToggleIconControl.Builder(
                    this.graphicsSupplier,
                    callback,
                    iconName,
                    toggledIconName,
                    UXThemeManager.getInstance().getControlSize()
            ).build();

            addPanel(c);

            return this;
        }

        @Override
        public ControlPanel build() {
            return new ControlPanel(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}