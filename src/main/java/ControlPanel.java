import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ControlPanel implements Drawable, MouseEventReceiver {

    public static final int FILL_ALPHA = 200;
    private static final int CONTROL_COLOR = 40;

    static final int CONTROL_SIZE = 35; // Adjust the size as needed
    private final int controlSize;
    private final List<Control> controls;
    private final PanelPosition position;
    private final PanelAlignment alignment;
    private final PApplet processing;

    private final boolean sizeToFit;

    private ControlPanel(Builder builder) {
        this.controls = builder.controls;
        this.position = builder.position;
        this.alignment = builder.alignment;
        this.processing = builder.processing;
        this.controlSize = builder.controlSize;
        this.sizeToFit = builder.sizeToFit;
    }

    @Override
    public void onMousePressed() {
        for (Control c : controls) {
            c.mousePressed(isMouseOverControl(c));
        }
    }

    @Override
    public void onMouseReleased() {
        for (Control c : controls) {
            if (isMouseOverControl(c))
                c.mouseReleased();
        }
    }

    private int getLongestDimension(int dimension) {
        if (sizeToFit) {
            return controls.size() * controlSize;
        } else {
            return dimension;
        }
    }

    @Override
    public void draw(PGraphics buffer) {
        // Determine the width and height based on the position.
        int width = (position == PanelPosition.LEFT || position == PanelPosition.RIGHT) ? controlSize : getLongestDimension(buffer.width);
        int height = (position == PanelPosition.TOP || position == PanelPosition.BOTTOM) ? controlSize : getLongestDimension(buffer.height);

        buffer.pushStyle();
        buffer.fill(CONTROL_COLOR, FILL_ALPHA); // semi-transparent panel

        float x = (position == PanelPosition.RIGHT) ? buffer.width - controlSize : 0;
        float y = (position == PanelPosition.BOTTOM) ? buffer.height - controlSize : 0;

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

        for (Control c : controls) {
            c.mouseHover(isMouseOverControl(c));
            c.draw(buffer);
        }

        buffer.popStyle();
    }

    private void arrangeControls(float startX, float startY) {
        float x = startX, y = startY;

        for (Control c : controls) {
            c.setPosition(new PVector(x, y));

            if (position == PanelPosition.LEFT || position == PanelPosition.RIGHT) {
                y += controlSize;
            } else {
                x += controlSize;
            }
        }
    }

    public boolean mousePressedOverMe() {
        for (Control c : controls) {
           if (isMouseOverControl(c)) {
             return true;
           }
        }
        return false;
    }

    private boolean isMouseOverControl(Control c) {
        PVector controlPosition = c.getPosition(); // This method needs to exist in your Control class
        int controlSize = CONTROL_SIZE;

        try {
            Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            Point windowPosition = ((java.awt.Component) processing.getSurface().getNative()).getLocationOnScreen();

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

    enum PanelPosition {
        LEFT, TOP, RIGHT, BOTTOM
    }

    enum PanelAlignment {
        CENTER, LEFT, RIGHT, TOP, BOTTOM
    }

    public static class Builder {
        protected PanelPosition position;
        protected PanelAlignment alignment;
        protected PApplet processing;
        protected List<Control> controls;
        protected int controlSize;

        protected boolean sizeToFit;

        public Builder(PanelPosition position, PApplet processing) {
            this.position = position;
            this.processing = processing;
            this.controls = new ArrayList<Control>();
            this.controlSize = CONTROL_SIZE;
            this.alignment = PanelAlignment.CENTER;
            this.sizeToFit = false;
        }

        public Builder addControl(String iconName, KeyCallback callback) throws IOException {
            PImage icon = getIcon(iconName);
            Control control = new Control(icon, this.controlSize, this.position, callback);
            controls.add(control);
            return this;
        }

        private PImage getIcon(String iconName) {
            PImage icon = processing.loadImage("icon/" + iconName);
            icon.resize(this.controlSize - 5, this.controlSize - 5);
            return icon;
        }

        public Builder addControl(String iconName, String toggledIconName, KeyCallback callback) throws IOException {
            PImage icon = getIcon(iconName);
            PImage toggledIcon = getIcon(toggledIconName);
            Control control = new Control(icon, toggledIcon, this.controlSize, this.position, callback);
            callback.setAssociatedControl(control);
            controls.add(control);
            return this;
        }

        public Builder sizeToFit(boolean sizeToFit) {
            this.sizeToFit = sizeToFit;
            return this;
        }

        public Builder alignment(PanelAlignment alignment) {
            this.alignment = alignment;
            return this;
        }

        public ControlPanel build() {
            return new ControlPanel(this);
        }
    }
}