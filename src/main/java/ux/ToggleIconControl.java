package ux;

import actions.KeyCallback;
import actions.KeyObservable;
import processing.core.PImage;

public class ToggleIconControl extends Control {
    PImage toggledIcon; // right now only used with play / pause
    boolean iconToggled = false;
    PImage currentIcon;
    protected ToggleIconControl(Builder builder) {
        super(builder);
        this.currentIcon = icon;
        this.toggledIcon = loadIcon(builder.toggledIconName);
    }

    @Override
    public void onMouseReleased() {
        super.onMouseReleased();
        toggleIcon();
    }

    @Override
    protected PImage getIcon() {
        return iconToggled ? toggledIcon : icon;
    }

    @Override
    public void notifyKeyPress(KeyObservable o) {
        toggleIcon();
    }

    private void toggleIcon() {
        currentIcon = (iconToggled) ? icon : toggledIcon;
        iconToggled = !iconToggled;
    }

    public static class Builder extends Control.Builder {
        private final String  toggledIconName;

        public Builder(PGraphicsSupplier graphicsSupplier, KeyCallback callback, String iconName, String tooggledIcontName, int size) {
            super(graphicsSupplier, callback, iconName, size);
            this.toggledIconName = tooggledIcontName;
        }

        @Override
        public ToggleIconControl build() {
            return new ToggleIconControl(this);
        }

        @Override
        public Builder self() {
            return this;
        }

    }
}