import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;


class Control implements Drawable, KeyObserver {
    private static final int CORNER_RADIUS = 10;
    private static final int TEXT_HEIGHT_BUFFER = 5;
    private static final int TEXT_WIDTH_BUFFER = 10;
    private static final int HOVER_TEXT_DISTANCE = 5;
    private static final int HOVER_COLOR = 80;
    private static final int HOVER_TEXT_COLOR = 255;
    private static final int MOUSE_PRESSED_COLOR = 225;
    private static final int HIGHLIGHT_SHORTCUT_KEY_INVOKED_DURATION = 1000;

    private final int size;
    private final ControlPanel.PanelPosition panelPosition;
    PImage icon;  // The PNG icon
    PImage toggledIcon; // right now only used with play / pause
    boolean iconToggled = false;
    PImage currentIcon;

    KeyCallback callback;
    String hoverText;
    boolean isHovering = false;

    boolean isHoveringPrevious = false;
    boolean isPressed = false;
    private PVector position;
    private boolean highlight = false;
    private boolean toggleControl = false;

    Control(PImage icon, boolean toggleControl, int size, ControlPanel.PanelPosition panelPosition, KeyCallback callback) throws IOException {
        this.icon = icon;
        this.size = size;
        this.currentIcon = icon;
        this.callback = callback;
        this.toggleControl = toggleControl;

        String keyCombos = callback.getValidKeyCombosForCurrentOS().stream()
                .map(KeyCombo::toString)
                .collect(Collectors.joining(", "));

        this.hoverText = callback.getUsageText() + " (shortcut: " + keyCombos + ")";
        this.panelPosition = panelPosition;
    }

    Control(PImage icon, PImage toggledIcon, boolean toggleControl, int size, ControlPanel.PanelPosition panelPosition, KeyCallback callback) throws IOException {
        this(icon, false, size, panelPosition, callback); // Call the existing constructor
        this.toggledIcon = toggledIcon; // Set the alternate icon
    }

    PVector getPosition() {
        return this.position;
    }

    void setPosition(PVector position) {
        this.position = position;
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


    void mousePressed(boolean isPressed) {
        this.isPressed = isPressed;

        if (isPressed) {
            isHovering = false;
            isHoveringPrevious = true;
        }
    }


    // if a mouse is released over the control then that means it was clicked
    void mouseReleased() {
        callback.invokeFeature();
        if (null!=toggledIcon) {
            toggleIcon();
        }
        this.isPressed = false;

        if (toggleControl) {
            highlight = !highlight;
        }
    }

    void toggleIcon() {
        currentIcon = (iconToggled) ? icon : toggledIcon;
        iconToggled = !iconToggled;
    }


    @Override
    public void draw(PGraphics buffer) {
        drawHovering(buffer);
        drawPressed(buffer);
        drawIcon(buffer);
    }

    @Override
    public void notifyKeyPress(KeyObservable o, Object arg) {
        // here's where you do your thing when you've been observed


        // this is just for the play button right now, which gets a different icon if invoked
        if (toggledIcon !=null) {
            toggleIcon();
        }

        // if you're the kind of control that is persistent, then you get to keep displaying
        if (toggleControl) {
            highlight = !highlight;
        } else {
            // if you're the type of control that is merely invoked by a keyboard mapping
            // then highlight for a duration
            // rock on
            highlightForPeriod(HIGHLIGHT_SHORTCUT_KEY_INVOKED_DURATION);
        }
    }

    private void highlightForPeriod(int duration) {
        highlight = true;

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        highlight = false;
                    }
                },
                duration
        );
    }

    private void drawPressed(PGraphics buffer) {
        if (isPressed || highlight) {
            drawControlHighlight(buffer, MOUSE_PRESSED_COLOR);
        }
    }

    private void drawControlHighlight(PGraphics buffer, int color) {
        // higlight the control with a semi-transparent rect
        buffer.fill(color, color, color, ControlPanel.FILL_ALPHA); // Semi-transparent gray
        float roundedRectSize = size;
        buffer.rect(position.x, position.y, roundedRectSize, roundedRectSize, CORNER_RADIUS); // Rounded rectangle with radius 7
    }

    private void drawHovering(PGraphics buffer) {

        if (isHovering) {

            buffer.pushStyle(); // Save the current style settings
            drawControlHighlight(buffer, HOVER_COLOR);

            // Calculate the position of the hover text based on the  control's position and the
            // parent panelPosition
            float hoverRectX = position.x;
            float hoverRectY = position.y;

            buffer.textSize(16);
            float textWidth = buffer.textWidth(hoverText) + (TEXT_WIDTH_BUFFER * 2);
            float textHeight = buffer.textAscent() + buffer.textDescent() + (TEXT_HEIGHT_BUFFER * 2);

            switch (panelPosition) {
                case LEFT -> hoverRectX = position.x + size + HOVER_TEXT_DISTANCE;
                case TOP -> hoverRectY = position.y + size + HOVER_TEXT_DISTANCE;
                case RIGHT -> hoverRectX = position.x - textWidth - HOVER_TEXT_DISTANCE;
                case BOTTOM -> hoverRectY = position.y - size - HOVER_TEXT_DISTANCE;
            }

            if (hoverRectX + textWidth > buffer.width && (panelPosition != ControlPanel.PanelPosition.LEFT)) {
                hoverRectX = Math.max(buffer.width - textWidth, 0);
            }

            float hoverTextX = hoverRectX + TEXT_WIDTH_BUFFER;
            float hoverTextY = hoverRectY + TEXT_HEIGHT_BUFFER;

            buffer.rect(hoverRectX, hoverRectY, textWidth, textHeight, CORNER_RADIUS);

            // Draw the hover text at the calculated position
            buffer.textAlign(PApplet.LEFT, PApplet.TOP);
            buffer.fill(HOVER_TEXT_COLOR); // white text
            buffer.text(hoverText, hoverTextX, hoverTextY);

            buffer.popStyle(); // Restore the style settings
        }
    }


    void drawIcon(PGraphics buffer) {
        float x = position.x + (float) (size - icon.width) / 2;
        float y = position.y + (float) (size - icon.height) / 2;
        buffer.image(currentIcon, x, y);
    }

}
