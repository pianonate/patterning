package ux;

import actions.KeyCallback;
import actions.KeyCombo;
import actions.KeyObservable;
import actions.KeyObserver;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.util.*;
import java.util.stream.Collectors;

class OldControl implements  KeyObserver {
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
    private final int size;
    private final OldPanelPosition panelPosition;
    PImage icon;  // The PNG icon
    PGraphics hoverBuffer;
    Transition hoverTransition;
    KeyCallback callback;
    String hoverText;
    boolean isHovering = false;
    boolean isHoveringPrevious = false;
    boolean isPressed = false;
    private PVector position;
    protected boolean highlight = false;

    OldControl(PImage icon, int size, OldPanelPosition panelPosition, KeyCallback callback) {
        this.icon = icon;
        this.size = size;
        this.callback = callback;

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
        this.isPressed = false;
    }

    public void draw(PGraphics buffer) {
    //public void draw() {
        if (null == hoverBuffer) {
            makeHoverBuffer(buffer);
        }
        drawHovering(buffer);
        drawPressed(buffer);
        drawIcon(buffer);
    }

    private List<String> wrapText(String text, PGraphics buffer) {
        List<String> words = new ArrayList<>(Arrays.asList(text.split(" ")));
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        while (!words.isEmpty()) {
            String word = words.get(0);
            if (word.startsWith("(shortcut:")) {
                if (words.size() > 1 && buffer.textWidth(line + word + " " + words.get(1)) < OldControl.HOVER_TEXT_MAX_WIDTH) {
                    line.append(word).append(" ").append(words.get(1)).append(" ");
                    words.remove(0);
                    words.remove(0);
                } else {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
            } else if (buffer.textWidth(line + word) < OldControl.HOVER_TEXT_MAX_WIDTH) {
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

    private void makeHoverBuffer(PGraphics buffer) {

        buffer.textSize(HOVER_TEXT_SIZE);
        List<String> lines = wrapText(hoverText, buffer);

        float textHeight = (buffer.textAscent() + buffer.textDescent() + (TEXT_HEIGHT_BUFFER * 2)) * lines.size();
        float textWidth = lines.stream().map(buffer::textWidth).max(Float::compare).orElse(0f) + (TEXT_WIDTH_BUFFER * 2);

        // Create the off-screen buffer
        // createGraphics doesn't inherit the font of the parent.  it's a damn shame
        hoverBuffer = buffer.parent.createGraphics((int) textWidth, (int) textHeight);
        hoverBuffer.beginDraw();
        hoverBuffer.clear();

        hoverBuffer.textSize(HOVER_TEXT_SIZE);
        hoverBuffer.textFont(PatternDrawer.font, HOVER_TEXT_SIZE);
        hoverBuffer.textAlign(PApplet.LEFT, PApplet.TOP);
        hoverBuffer.fill(HOVER_COLOR);
        hoverBuffer.rect(0, 0, textWidth, textHeight, CORNER_RADIUS);
        hoverBuffer.fill(HOVER_TEXT_COLOR);

        float currentY = TEXT_HEIGHT_BUFFER;
        for (String line : lines) {
            hoverBuffer.text(line, TEXT_WIDTH_BUFFER, currentY);
            currentY += hoverBuffer.textAscent() + hoverBuffer.textDescent() + (TEXT_HEIGHT_BUFFER * 2);
        }

        hoverBuffer.endDraw();

        Transition.TransitionDirection direction = switch (panelPosition) {
            case LEFT -> Transition.TransitionDirection.RIGHT;
            case TOP -> Transition.TransitionDirection.DOWN;
            case RIGHT -> Transition.TransitionDirection.LEFT;
            case BOTTOM -> Transition.TransitionDirection.UP;
        };

        hoverTransition = new Transition(direction, Transition.TransitionType.SLIDE);
    }

    private void drawHovering(PGraphics buffer) {
        if (isHovering) {
            drawControlHighlight(buffer, HOVER_COLOR);

            // Calculate the position of the hover graphics based on the control's position and the parent panelPosition
            float hoverRectX = position.x;
            float hoverRectY = position.y;

            switch (panelPosition) {
                case LEFT -> hoverRectX = position.x + size + HOVER_TEXT_DISTANCE;
                case TOP -> hoverRectY = position.y + size + HOVER_TEXT_DISTANCE;
                case RIGHT -> hoverRectX = position.x - HOVER_TEXT_DISTANCE;
                case BOTTOM -> hoverRectY = position.y - HOVER_TEXT_DISTANCE;
            }

            hoverTransition.transition(buffer, hoverBuffer, hoverRectX, hoverRectY);
        } else {
            hoverTransition.reset();
        }
    }


    private void drawPressed(PGraphics buffer) {
        if (isPressed || highlight) {
            drawControlHighlight(buffer, MOUSE_PRESSED_COLOR);
        }
    }

    protected PImage getIcon() {
        return icon;
    }

    void drawIcon(PGraphics buffer) {
        PImage currentIcon = getIcon();
        float x = position.x + (float) (size - currentIcon.width) / 2;
        float y = position.y + (float) (size - currentIcon.height) / 2;
        buffer.image(currentIcon, x, y);
    }

    private void drawControlHighlight(PGraphics buffer, int color) {
        // highlight the control with a semi-transparent rect
        buffer.fill(color, color, color, OldControlPanel.FILL_ALPHA); // Semi-transparent gray
        float roundedRectSize = size;
        buffer.rect(position.x, position.y, roundedRectSize, roundedRectSize, CORNER_RADIUS); // Rounded rectangle with radius 7
    }

    @Override
    public void notifyKeyPress(KeyObservable o) {
        // here's where you do your thing when you've been observed

        // if you're the type of control that is merely invoked by a keyboard mapping
        // then highlight for a duration. this applies to most oldControls
        highlightForPeriod();

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
                OldControl.HIGHLIGHT_SHORTCUT_KEY_INVOKED_DURATION
        );
    }

}
