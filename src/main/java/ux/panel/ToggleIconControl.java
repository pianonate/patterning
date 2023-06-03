package ux.panel;

import actions.KeyCallback;
import actions.KeyObservable;
import processing.core.PImage;
import ux.UXThemeManager;
import ux.informer.DrawingInfoSupplier;

import java.util.Timer;
import java.util.TimerTask;

public class ToggleIconControl extends Control {
    PImage toggledIcon; // right now only used with play / pause
    boolean iconToggled = false;

    boolean singleMode = false;
    PImage currentIcon;

    KeyCallback modeChangeCallback;

    protected ToggleIconControl(Builder builder) {
        super(builder);
        this.currentIcon = icon;
        this.toggledIcon = loadIcon(builder.toggledIconName);
        this.modeChangeCallback = builder.modeChangeCallback;
        modeChangeCallback.addObserver(this);
    }

    @Override
    protected PImage getIcon() {
        return iconToggled ? toggledIcon : icon;
    }

    @Override
    public void notifyKeyPress(KeyObservable o) {
        if (o.invokeModeChange()) {
            toggleMode();
            if (!iconToggled)
                toggleIcon();
        }
        else
            toggleIcon();
    }

    @Override
    public void onMouseReleased() {
        super.onMouseReleased();
        toggleIcon();
    }

    private void toggleIcon() {
        currentIcon = (iconToggled) ? icon : toggledIcon;
        iconToggled = !iconToggled;

        if (singleMode && !iconToggled) {
            new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            toggleIcon();
                        }
                    },
                    UXThemeManager.getInstance().getSingleModeToggleDuration()
            );
        }
    }

    private void toggleMode() {
        singleMode = !singleMode;
    }

    public static class Builder extends Control.Builder {
        private final String toggledIconName;
        private final KeyCallback modeChangeCallback;

        public Builder(DrawingInfoSupplier drawingInformer, KeyCallback callback, KeyCallback modeChangeCallback, String iconName, String tooggledIcontName, int size) {
            super(drawingInformer, callback, iconName, size);
            this.toggledIconName = tooggledIcontName;
            this.modeChangeCallback = modeChangeCallback;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ToggleIconControl build() {
            return new ToggleIconControl(this);
        }

    }
}