package ux;

import actions.KeyCallback;
import actions.MouseEventReceiver;
import patterning.Patterning;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OldControlPanel implements Drawable, MouseEventReceiver {

    public static final int FILL_ALPHA = 200;
    private static final int CONTROL_COLOR = 40;
    private final List<OldControl> oldControls;
    private final OldPanelPosition position;
    private final OldPanelAlignment alignment;

    private final int controlSize;

    private final boolean sizeToFit;

    private OldControlPanel(Builder builder) {
        this.oldControls = builder.oldControls;
        this.position = builder.position;
        this.alignment = builder.alignment;
        this.sizeToFit = builder.sizeToFit;
        this.controlSize =  UXThemeManager.getInstance().getControlSize();

    }

    @Override
    public void onMousePressed() {
        for (OldControl c : oldControls) {
            c.mousePressed(isMouseOverControl(c));
        }
    }

    @Override
    public void onMouseReleased() {
        for (OldControl c : oldControls) {
            if (isMouseOverControl(c))
                c.mouseReleased();
        }
    }

    private int getLongestDimension(int dimension) {
        if (sizeToFit) {
            return oldControls.size() * controlSize;
        } else {
            return dimension;
        }
    }

    @Override
    public void draw(PGraphics buffer) {
        // Determine the width and height based on the position.
        int width = (position == OldPanelPosition.LEFT || position == OldPanelPosition.RIGHT) ? controlSize : getLongestDimension(buffer.width);
        int height = (position == OldPanelPosition.TOP || position == OldPanelPosition.BOTTOM) ? controlSize : getLongestDimension(buffer.height);

        buffer.pushStyle();
        buffer.fill(CONTROL_COLOR, FILL_ALPHA); // semi-transparent panel

        float x = (position == OldPanelPosition.RIGHT) ? buffer.width - controlSize : 0;
        float y = (position == OldPanelPosition.BOTTOM) ? buffer.height - controlSize : 0;

        if (sizeToFit) {
            switch (position) {
                case LEFT, RIGHT -> {
                    // only y can vary
                    switch (alignment) {
                        case CENTER -> {
                            y = (float) (buffer.height - height) / 2;
                        }
                        case BOTTOM -> {
                            y = buffer.height - height;
                        }
                    }
                }
                case TOP, BOTTOM -> {
                    // only x can vary
                    switch (alignment) {
                        case CENTER -> {
                            x = (float) (buffer.width - width) / 2;
                        }
                        case RIGHT -> {
                            x = buffer.width - width;
                        }
                    }
                }
            }
        }

        buffer.stroke(40);
        buffer.strokeWeight(1);
        buffer.rect(x, y, width, height);

        arrangeControls(x, y);

        for (OldControl c : oldControls) {
            c.mouseHover(isMouseOverControl(c));
            c.draw(buffer);
        }

        buffer.popStyle();
    }

    private void arrangeControls(float startX, float startY) {
        float x = startX, y = startY;

        for (OldControl c : oldControls) {
            c.setPosition(new PVector(x, y));

            if (position == OldPanelPosition.LEFT || position == OldPanelPosition.RIGHT) {
                y += controlSize;
            } else {
                x += controlSize;
            }
        }
    }

    public boolean mousePressedOverMe() {
        for (OldControl c : oldControls) {
           if (isMouseOverControl(c)) {
             return true;
           }
        }
        return false;
    }

    private boolean isMouseOverControl(OldControl c) {
        PVector controlPosition = c.getPosition(); // This method needs to exist in your oldControls.OldControl class
        int controlSize = UXThemeManager.getInstance().getControlSize();

        try {
            PApplet processing = Patterning.getInstance();

            // we have to go through these shenanigans as processing by itself
            // can't tell when we've moved the mouse pointer away from its window
            // which breaks the hover behavior
            // we need a reference to it to get the ball rolling
            // I wish there were a simpler way
            Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            Point windowPosition = ((java.awt.Component) Patterning.getInstance().getSurface().getNative()).getLocationOnScreen();

            // Translate mouse position to be relative to window
            int mouseX = mousePosition.x - windowPosition.x;
            int mouseY = mousePosition.y - windowPosition.y;

            // Check if the mouse is within the window
            if (mouseX < 0 || mouseX > processing.width || mouseY < 0 || mouseY > processing.height) {
                return false;
            }

            return mouseX >= controlPosition.x && mouseX <= controlPosition.x + controlSize &&
                    mouseY >= controlPosition.y && mouseY <= controlPosition.y + controlSize;
        } catch (Exception e) {
            return false;
        }
    }

    public static class Builder {
        protected OldPanelPosition position;
        protected OldPanelAlignment alignment;
        protected List<OldControl> oldControls;

        private static final int controlSize = UXThemeManager.getInstance().getControlSize();
        protected boolean sizeToFit;

        public Builder(OldPanelPosition position) {
            this.position = position;
            this.oldControls = new ArrayList<OldControl>();
            this.alignment = OldPanelAlignment.CENTER;
            this.sizeToFit = false;
        }

        private PImage getIcon(String iconName) {
            PImage icon = Patterning.getInstance().loadImage("icon/" + iconName);
            icon.resize( controlSize - 5, controlSize - 5);
            return icon;
        }

        public Builder addControl(String iconName, KeyCallback callback) throws IOException {
            PImage icon = getIcon(iconName);
            OldControl oldControl = new OldControl(icon, controlSize, this.position, callback);
            return addControlInternal(oldControl, callback);
        }

        public Builder addToggleControl(String iconName, KeyCallback callback) throws IOException {
            OldControl oldControl = new ToggleHighlightOldControl(getIcon(iconName), controlSize, this.position, callback);
            return addControlInternal(oldControl, callback);
        }

        public Builder addToggleIconControl(String iconName, String toggledIconName, KeyCallback callback) throws IOException {
            PImage icon = getIcon(iconName);
            PImage toggledIcon = getIcon(toggledIconName);
            OldControl oldControl = new ToggleIconOldControl(icon, toggledIcon, controlSize, this.position, callback);
            return addControlInternal(oldControl, callback);
        }

        private Builder addControlInternal(OldControl oldControl, KeyCallback callback) {
            callback.addObserver(oldControl);
            oldControls.add(oldControl);
            return this;
        }


        public Builder sizeToFit(boolean sizeToFit) {
            this.sizeToFit = sizeToFit;
            return this;
        }

        public Builder alignment(OldPanelAlignment alignment) {
            this.alignment = alignment;
            return this;
        }

        public OldControlPanel build() {
            return new OldControlPanel(this);
        }
    }
}