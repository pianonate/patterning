package ux;

import actions.*;
import patterning.Patterning;
import processing.core.PImage;

import java.util.Timer;
import java.util.TimerTask;


public class Control extends Panel implements KeyObserver, MouseEventReceiver {

    private static final UXThemeManager theme = UXThemeManager.getInstance();
    private final KeyCallback callback;
    private final int size;
    boolean isHighlightFromKeypress = false;
    private PImage icon;

    protected Control(Builder builder) {
        super(builder);

        this.callback = builder.callback;
        this.size = builder.size;

        setFill(theme.getControlColor());
        callback.addObserver(this);
        MouseEventManager.getInstance().addReceiver(this);

        setIcon(builder.iconName);
    }

    @Override
    protected void panelSubclassDraw() {
        mouseHover(isMouseOverMe());
        drawHover();
        drawPressed();
        drawIcon();
    }

    private void drawHover() {
        if (isHovering) {
            drawControlHighlight(theme.getControlHighlightColor());
        }
    }

    void mouseHover(boolean isHovering) {

        if (isPressed && !isHovering) {
            // If pressed and not hovering, reset the pressed state
            this.isPressed = false;
        } else if (isHovering != isHoveringPrevious) {
            // Only update isHovering if there is a change in hover state
            this.isHovering = isHovering;
            isHoveringPrevious = isHovering;
        }
    }

    private void setIcon(String iconName) {
        PImage icon = Patterning.getInstance().loadImage(theme.getIconPath() + iconName);
        icon.resize(width - 5, height - 5);
        this.icon = icon;
    }

    private void drawPressed() {
        if (isPressed || isHighlightFromKeypress) {
            drawControlHighlight(theme.getControlMousePressedColor());
        }
    }

    private void drawIcon() {
        float x = (float) (width - icon.width) / 2;
        float y = (float) (height - icon.height) / 2;
        panelBuffer.image(icon, x, y);
    }

    private void drawControlHighlight(int color) {
        // highlight the control with a semi-transparent rect
        panelBuffer.fill(color); // Semi-transparent gray
        float roundedRectSize = size;
        // Rounded rectangle with radius
        panelBuffer.rect(0,0, roundedRectSize, roundedRectSize, theme.getControlHighlightCornerRadius());
    }

    @Override
    public void notifyKeyPress(KeyObservable o) {
        highlightFromKeyPress();
    }

    private void highlightFromKeyPress() {
        isHighlightFromKeypress = true;

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        isHighlightFromKeypress = false;
                    }
                },
                theme.getControlHighlightDuration()
        );
    }

    @Override
    public void onMousePressed() {
        super.onMousePressed();
    }
    @Override
    public void onMouseReleased() {
        super.onMouseReleased();  // Calls Panel's onMouseReleased
        if (isMouseOverMe()) {
            callback.invokeFeature();  // Specific to Control
        }
    }
/*
    @Override
    public void onMousePressed() {
        if (isMouseOverMe()) {
            super.onMousePressed();
        }
    }

    @Override
    public void onMouseReleased() {
        if (isMouseOverMe()) {
            super.onMouseReleased();
            callback.invokeFeature();
        }
    }*/


    public static class Builder extends Panel.Builder<Builder> {
        private final KeyCallback callback;
        private final String iconName;
        private final int size;

        public Builder(KeyCallback callback, String iconName, int size) {
            super(0,0,size,size);
            this.callback = callback;
            this.iconName = iconName;
            this.size = size;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Control build() {
            return new Control(this);
        }
    }

}
