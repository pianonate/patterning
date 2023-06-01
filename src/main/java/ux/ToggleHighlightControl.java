package ux;

import actions.KeyCallback;
import actions.KeyObservable;

public class ToggleHighlightControl extends Control {
    protected ToggleHighlightControl(Builder builder) {
        super(builder);
    }

    @Override
    public void onMouseReleased() {
        super.onMouseReleased();
        isHighlightFromKeypress = !isHighlightFromKeypress;
    }

    @Override
    public void notifyKeyPress(KeyObservable o) {
        // Specific behavior for ToggleHighlightControl
        isHighlightFromKeypress = !isHighlightFromKeypress;
    }

    public static class Builder extends Control.Builder {

        public Builder(DrawingInfoSupplier drawingInformer, KeyCallback callback, String iconName, int size) {
            super(drawingInformer, callback, iconName, size);
        }

        @Override
        public ToggleHighlightControl build() {
            return new ToggleHighlightControl(this);
        }

        @Override
        public Builder self() {
            return this;
        }

    }
}