package ux;

import actions.*;
import patterning.Patterning;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.Timer;
import java.util.TimerTask;


public class Control extends Panel implements KeyObserver, MouseEventReceiver {
    private final KeyCallback callback;
    private final int size;
    boolean isHighlightFromKeypress = false;
    private PImage icon;

    public Control(KeyCallback callback, String iconName, int size) {
        super(0, 0, size, size);
        super.setFill(UXTheme.getInstance().getControlColor());
        super.setSubclassDraw(this::controlDrawer);
        callback.addObserver(this);
        MouseEventManager.getInstance().addReceiver(this);
        this.size = size;
        this.callback = callback;
        setIcon(iconName);
    }

    public void controlDrawer(PGraphics buffer) {
        mouseHover(isMouseOverMe());
        drawHover(buffer);
        drawPressed(buffer);
        drawIcon(buffer);
    }

    private void drawHover(PGraphics buffer) {
        if (isHovering) {
            drawControlHighlight(buffer, UXTheme.getInstance().getControlHighlightColor());
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
        PImage icon = Patterning.getInstance().loadImage("icon/" + iconName);
        icon.resize(width - 5, height - 5);
        this.icon = icon;
    }

    private void drawPressed(PGraphics buffer) {
        if (isPressed || isHighlightFromKeypress) {
            drawControlHighlight(buffer, UXTheme.getInstance().getControlMousePressedColor());
        }
    }

    private void drawIcon(PGraphics buffer) {
        float x = (float) (width - icon.width) / 2;
        float y = (float) (height - icon.height) / 2;
        buffer.image(icon, x, y);
    }

    private void drawControlHighlight(PGraphics buffer, int color) {
        // highlight the control with a semi-transparent rect
        buffer.fill(color); // Semi-transparent gray
        float roundedRectSize = size;
        // Rounded rectangle with radius
        buffer.rect(0,0, roundedRectSize, roundedRectSize, UXTheme.getInstance().getControlHighlightCornerRadius());
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
                UXTheme.getInstance().getControlHighlightDuration()
        );
    }

    @Override
    public void onMousePressed() {  super.onMousePressed();}

    @Override
    public void onMouseReleased() {
        super.onMouseReleased();  // Calls Panel's onMouseReleased
        if (isMouseOverMe()) {
            callback.invokeFeature();  // Specific to Control
        }
    }

}
