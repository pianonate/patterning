import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.util.*;
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
    private static final int HOVER_TEXT_SIZE = 14;
    private static final float HOVER_TEXT_MAX_WIDTH = 250;
    private static final int ANIMATION_DURATION = 300;
    private final int size;
    private final ControlPanel.PanelPosition panelPosition;
    private final boolean toggleControl;
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
    private float animateStartTime;
    private boolean isAnimating = false;
    private long hoverStartTime = -1;
    private float animationProgress = 0.0f;


    Control(PImage icon, PImage toggledIcon, boolean toggleControl, int size, ControlPanel.PanelPosition panelPosition, KeyCallback callback) {
        this(icon, toggleControl, size, panelPosition, callback); // Call the existing constructor
        this.toggledIcon = toggledIcon; // Set the alternate icon
    }


    Control(PImage icon, boolean toggleControl, int size, ControlPanel.PanelPosition panelPosition, KeyCallback callback) {
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

            if (isHovering) {
                hoverStartTime = System.currentTimeMillis();
            }
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
        if (null != toggledIcon) {
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


   /* private void drawHovering(PGraphics buffer) {

        if (isHovering) {

            buffer.pushStyle(); // Save the current style settings
            drawControlHighlight(buffer, HOVER_COLOR);

            // Calculate the position of the hover text based on the  control's position and the
            // parent panelPosition
            float hoverRectX = position.x;
            float hoverRectY = position.y;

            buffer.textSize(HOVER_TEXT_SIZE);

            List<String> lines = wrapText(hoverText, buffer);

            float textHeight = (buffer.textAscent() + buffer.textDescent() + (TEXT_HEIGHT_BUFFER * 2)) * lines.size();
            float textWidth = lines.stream().map(buffer::textWidth).max(Float::compare).orElse(0f) + (TEXT_WIDTH_BUFFER * 2);

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
            float currentY = hoverRectY + TEXT_HEIGHT_BUFFER;

            // Draw the rectangle for the hover text
            buffer.rect(hoverRectX, hoverRectY, textWidth, textHeight, CORNER_RADIUS);

            buffer.textAlign(PApplet.LEFT, PApplet.TOP);
            buffer.fill(HOVER_TEXT_COLOR); // white text

            for (String line : lines) {
                buffer.text(line, hoverTextX, currentY);
                currentY += buffer.textAscent() + buffer.textDescent() + (TEXT_HEIGHT_BUFFER * 2);
            }

            buffer.popStyle(); // Restore the style settings
        }
    }*/

   private void drawHovering(PGraphics buffer) {
        if (isHovering) {
            // Initialize hover start time if this is the first frame of the hover
            if (hoverStartTime == -1) {
                hoverStartTime = System.currentTimeMillis();
            }

            // Calculate animation progress based on elapsed time
            long elapsed = System.currentTimeMillis() - hoverStartTime;
            animationProgress = PApplet.constrain((float) elapsed / ANIMATION_DURATION, 0, 1);

            drawControlHighlight(buffer, HOVER_COLOR);

            // Calculate the position of the hover text based on the control's position and the parent panelPosition
            float hoverRectX = position.x;
            float hoverRectY = position.y;

            buffer.textSize(HOVER_TEXT_SIZE);

            List<String> lines = wrapText(hoverText, buffer);

            float textHeight = (buffer.textAscent() + buffer.textDescent() + (TEXT_HEIGHT_BUFFER * 2)) * lines.size();
            float textWidth = lines.stream().map(buffer::textWidth).max(Float::compare).orElse(0f) + (TEXT_WIDTH_BUFFER * 2);

            switch (panelPosition) {
                case LEFT -> hoverRectX = position.x + size + HOVER_TEXT_DISTANCE;
                case TOP -> hoverRectY = position.y + size + HOVER_TEXT_DISTANCE;
                case RIGHT -> hoverRectX = position.x - textWidth - HOVER_TEXT_DISTANCE;
                case BOTTOM -> hoverRectY = position.y - size - HOVER_TEXT_DISTANCE;
            }

            if (hoverRectX + textWidth > buffer.width && (panelPosition != ControlPanel.PanelPosition.LEFT)) {
                hoverRectX = Math.max(buffer.width - textWidth, 0);
            }

            // Create an off-screen graphics buffer
            PGraphics offscreen = buffer.parent.createGraphics(buffer.width, buffer.height);
            offscreen.beginDraw();
            offscreen.clear();

            float hoverTextX = hoverRectX + TEXT_WIDTH_BUFFER;
            float currentY = hoverRectY + TEXT_HEIGHT_BUFFER;

            offscreen.fill(HOVER_COLOR); // gray

            // Draw the rectangle for the hover text
            offscreen.rect(hoverRectX, hoverRectY, textWidth, textHeight, CORNER_RADIUS);

            offscreen.textFont(PatternDrawer.font, HOVER_TEXT_SIZE);
            offscreen.textAlign(PApplet.LEFT, PApplet.TOP);
            offscreen.fill(HOVER_TEXT_COLOR); // white text

            for (String line : lines) {
                offscreen.text(line, hoverTextX, currentY);
                currentY += offscreen.textAscent() + offscreen.textDescent() + (TEXT_HEIGHT_BUFFER * 2);
            }

            offscreen.endDraw();

            // Clip the off-screen buffer to the portion of the tooltip that should be visible at this time and draw it onto the main buffer
            switch (panelPosition) {
                case LEFT ->
                        buffer.image(offscreen, 0, 0, (int) (hoverRectX + textWidth * animationProgress), buffer.height, 0, 0, (int) (hoverRectX + textWidth * animationProgress), buffer.height);
                case TOP ->
                        buffer.image(offscreen, 0, 0, buffer.width, (int) (hoverRectY + textHeight * animationProgress), 0, 0, buffer.width, (int) (hoverRectY + textHeight * animationProgress));
                case RIGHT ->
                        buffer.image(offscreen, (int) (hoverRectX + textWidth * (1 - animationProgress)), 0, buffer.width - (int) (hoverRectX + textWidth * (1 - animationProgress)), buffer.height, (int) (hoverRectX + textWidth * (1 - animationProgress)), 0, buffer.width, buffer.height);
                case BOTTOM ->
                        buffer.image(offscreen, 0, (int) (hoverRectY + textHeight * (1 - animationProgress)), buffer.width, buffer.height - (int) (hoverRectY + textHeight * (1 - animationProgress)), 0, (int) (hoverRectY + textHeight * (1 - animationProgress)), buffer.width, buffer.height);
            }

        } else if (hoverStartTime != -1) {
            // Reset hover start time if the hover has ended
            hoverStartTime = -1;
        }
    }


    private void drawPressed(PGraphics buffer) {
        if (isPressed || highlight) {
            drawControlHighlight(buffer, MOUSE_PRESSED_COLOR);
        }
    }

    void drawIcon(PGraphics buffer) {
        float x = position.x + (float) (size - icon.width) / 2;
        float y = position.y + (float) (size - icon.height) / 2;
        buffer.image(currentIcon, x, y);
    }

    private void drawControlHighlight(PGraphics buffer, int color) {
        // highlight the control with a semi-transparent rect
        buffer.fill(color, color, color, ControlPanel.FILL_ALPHA); // Semi-transparent gray
        float roundedRectSize = size;
        buffer.rect(position.x, position.y, roundedRectSize, roundedRectSize, CORNER_RADIUS); // Rounded rectangle with radius 7
    }

    private List<String> wrapText(String text, PGraphics buffer) {
        List<String> words = new ArrayList<>(Arrays.asList(text.split(" ")));
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        while (!words.isEmpty()) {
            String word = words.get(0);
            if (word.startsWith("(shortcut:")) {
                if (words.size() > 1 && buffer.textWidth(line + word + " " + words.get(1)) < Control.HOVER_TEXT_MAX_WIDTH) {
                    line.append(word).append(" ").append(words.get(1)).append(" ");
                    words.remove(0);
                    words.remove(0);
                } else {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
            } else if (buffer.textWidth(line + word) < Control.HOVER_TEXT_MAX_WIDTH) {
                line.append(word).append(" ");
                words.remove(0);
            } else {
                lines.add(line.toString());
                line = new StringBuilder();
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines;
    }

    @Override
    public void notifyKeyPress(KeyObservable o, Object arg) {
        // here's where you do your thing when you've been observed


        // this is just for the play button right now, which gets a different icon if invoked
        if (toggledIcon != null) {
            toggleIcon();
        }

        // if you're the kind of control that is persistent, then you get to keep displaying
        if (toggleControl) {
            highlight = !highlight;
        } else {
            // if you're the type of control that is merely invoked by a keyboard mapping
            // then highlight for a duration
            // rock on
            highlightForPeriod();
        }
    }

    private void highlightForPeriod() {
        highlight = true;

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        highlight = false;
                    }
                },
                Control.HIGHLIGHT_SHORTCUT_KEY_INVOKED_DURATION
        );
    }

}
