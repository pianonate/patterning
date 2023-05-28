package ux;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.*;

public class TextPanel extends Panel implements Drawable {

    private final static UXThemeManager theme = UXThemeManager.getInstance();
    private final static DrawableManager drawableManager = DrawableManager.getInstance();

    private final int textMargin = theme.getDefaultTextMargin();
    private final int doubleTextMargin = textMargin * 2;
    private final float textSize;
    private final OptionalInt fadeInDuration;
    private final OptionalInt fadeOutDuration;
    private final OptionalLong displayDuration; // long to compare to System.currentTimeMillis()

    // text countdown variables
    private final OptionalInt countdownFrom;
    private final Runnable runMethod;
    private final String initialMessage;

    // non-countdown variables
    public String message;
    public String lastMessage;
    private State state;
    private long transitionTime;
    private int fadeValue;

    protected TextPanel(TextPanel.Builder builder) {
        super(builder);
        // construct the TextPanel with the default Panel constructor
        // after that we'll figure out the variations we need to support
        // super(builder.hAlign, builder.vAlign);

        this.message = builder.message;

        this.textSize = builder.textSize;

        this.displayDuration = builder.displayDuration;

        this.fadeInDuration = builder.fadeInDuration;
        this.fadeOutDuration = builder.fadeOutDuration;

        // text countdown variables
        this.runMethod = builder.runMethod;
        this.countdownFrom = builder.countdownFrom;
        this.initialMessage = builder.message;

        // automatically start the display unless we're a countdown
        // which needs to be manually invoked by the caller...
        if (countdownFrom.isPresent()) {
            startCountdown();
        } else {
            startDisplay();
        }
    }

    // Countdown methods
    private void startCountdown() {
        setMessage(initialMessage);
        startDisplay();
    }

    private void startDisplay() {
        this.state = new FadeInState();
        this.transitionTime = System.currentTimeMillis(); // start displaying immediately
    }

    public void setMessage(String message) {
        lastMessage = this.message;
        this.message = message;
    }

    private List<String> wrapText(PGraphics buffer) {
        List<String> words = new ArrayList<>(Arrays.asList(message.split(" ")));
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        if (buffer.textWidth(message) <= 255) {
            lines.add(message);
            return lines;
        }

        while (!words.isEmpty()) {
            String word = words.get(0);
            if (buffer.textWidth(line + word) < 255) {
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
    protected PGraphics getPanelBuffer(PGraphics parentBuffer) {

        parentBuffer.textAlign(PApplet.LEFT, PApplet.TOP);

       // parentBuffer.textSize(textSize);

        String testMessage = (countdownFrom.isPresent()) ?
                getCountdownMessage(countdownFrom.getAsInt()) : message;

        // the original buffer passed in to textDisplay is created with whatever font
        // is on PApplet (the default font is Verdana, 12pt)

        // todo: see if this is a bug - i.e., maybe you should just set it to the font that you want
        // then run this and see if you fix the PATTERNING issue
        float adjustedTextSize = getAdjustedTextSize(parentBuffer, testMessage, textSize);

        width = (int) Math.ceil(parentBuffer.textWidth(testMessage) + doubleTextMargin);
        height = (int) Math.ceil(parentBuffer.textAscent() + parentBuffer.textDescent() + textMargin);

        PGraphics textBuffer = parentBuffer.parent.createGraphics(width + doubleTextMargin, height);

        // set the font for this PGraphics as it will not change
        textBuffer.beginDraw();
        textBuffer.textAlign(PApplet.LEFT, PApplet.TOP);
        setFont(textBuffer, adjustedTextSize);
        textBuffer.textSize(adjustedTextSize);

        textBuffer.endDraw();

        return textBuffer;
    }

    // for sizes to be correctly calculated, the font must be the same
    // on both the parent and the new textBuffer
    // necessary because createGraphics doesn't inherit the font from the parent
    private void setFont(PGraphics buffer, float textSize) {
        buffer.textFont(buffer.parent.createFont(theme.getFontName(), textSize));
    }


    private float getAdjustedTextSize(PGraphics parentBuffer, String testMessage, float startingSize) {

        float adjustedTextSize = startingSize;
        setFont(parentBuffer, startingSize);

        while ((parentBuffer.textWidth(testMessage) > (parentBuffer.width - doubleTextMargin))
                || ((parentBuffer.textAscent() + parentBuffer.textDescent()) > (parentBuffer.height - doubleTextMargin))) {
            adjustedTextSize -= .01; // smooth baby
            adjustedTextSize = Math.max(adjustedTextSize, 1); // Prevent the textSize from going below 1
            parentBuffer.textSize(adjustedTextSize);
        }

        return adjustedTextSize;
    }

    @Override
    protected boolean shouldGetPanelBuffer(PGraphics parentBuffer) {

        if (null == this.panelBuffer) {
            return true;
        }

        // if the text has change then we need to get a new buffer
        if (!Objects.equals(lastMessage, message)) {
            return true;
        }

        // if we've resized then text needs to adjust to
        return resized;
    }

    @Override
    protected void panelSubclassDraw() {

        // used for fading in the text and the various states
        // a ux.TextPanel can advance through
        state.update();

        int outlineColor = theme.getTextColorStart(); // black

        // currently interpolates between "black" 0xff000000 and "white" (0xffffffff)
        // fade values goes from 0 to 255 to make this happen

        // you need to get these colors every time in case the UX theme changes
        int currentColor = panelBuffer.lerpColor(outlineColor, theme.getTextColor(), fadeValue / 255.0F);

        // Draw black text slightly offset in each direction to create an outline effect
        float outlineOffset = 1.0F;

        panelBuffer.fill(outlineColor);
        panelBuffer.text(message, textMargin - outlineOffset,  - outlineOffset);
        panelBuffer.text(message, textMargin + outlineOffset,  - outlineOffset);

        // Draw the actual text in the calculated color
        panelBuffer.fill(currentColor);
        panelBuffer.text(message, textMargin, 0);

    }

    private String getCountdownMessage(long count) {
        return initialMessage + ": " + count;
    }

    public void interruptCountdown() {
        if (runMethod != null) runMethod.run();

        removeFromDrawableList();
    }

    private void removeFromDrawableList() {
        drawableManager.requestRemoval(this);
    }

    private interface State {
        void update();

        void transition();
    }

    public static class Builder extends Panel.Builder<Builder> {

        private static final UXThemeManager theme = UXThemeManager.getInstance();
        private final String message;
        private float textSize = theme.getDefaultTextSize();
        private OptionalInt fadeInDuration = OptionalInt.empty();
        private OptionalInt fadeOutDuration = OptionalInt.empty();

        private OptionalLong displayDuration = OptionalLong.empty();
        // Countdown variables
        private OptionalInt countdownFrom = OptionalInt.empty();

        private Runnable runMethod;

        public Builder(String message, HAlign hAlign, VAlign vAlign) {
            super(hAlign, vAlign);
            this.message = message;
        }

        public Builder textSize(int textSize) {
            this.textSize = textSize;
            return this;
        }

        public Builder fadeInDuration(int fadeInDuration) {
            this.fadeInDuration = OptionalInt.of(fadeInDuration);
            return this;
        }

        public Builder fadeOutDuration(int fadeOutDuration) {
            this.fadeOutDuration = OptionalInt.of(fadeOutDuration);
            return this;
        }

        public Builder countdownFrom(int countdownFrom) {
            this.countdownFrom = OptionalInt.of(countdownFrom);
            return this;
        }

        public Builder runMethod(Runnable runMethod) {
            this.runMethod = runMethod;
            return this;
        }

        public Builder displayDuration(long displayDuration) {
            this.displayDuration = OptionalLong.of(displayDuration);
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public TextPanel build() {
            return new TextPanel(this);
        }
    }

    private class FadeInState implements State {
        @Override
        public void update() {

            long elapsedTime = System.currentTimeMillis() - transitionTime;
            if (fadeInDuration.isPresent()) {
                fadeValue = PApplet.constrain((int) PApplet.map(elapsedTime, 0, fadeInDuration.getAsInt(), 0, 255), 0, 255);
            } else {
                // fade values range from 0 to 255 so the lerpColor will generate a value from 0 to 1
                fadeValue = 255;
            }
            if (fadeInDuration.isEmpty() || elapsedTime >= fadeInDuration.getAsInt()) {
                transition();
            }
        }

        @Override
        public void transition() {
            if (countdownFrom.isPresent()) {
                state = new CountdownState();
                transitionTime = System.currentTimeMillis();
                state.update(); // Force an immediate update after transitioning to the CountdownState
            } else {
                state = new DisplayState();
            }
            transitionTime = System.currentTimeMillis();
        }
    }

    private class DisplayState implements State {
        @Override
        public void update() {
            long elapsedTime = System.currentTimeMillis() - transitionTime;
            if (displayDuration.isPresent() && elapsedTime > displayDuration.getAsLong()) {
                transition();
            }
        }

        @Override
        public void transition() {
            if (fadeOutDuration.isPresent()) {
                state = new FadeOutState();
                transitionTime = System.currentTimeMillis();
            } else {
                removeFromDrawableList();
            }
        }
    }

    private class CountdownState implements State {

        // can only be here if countdownFrom.isPresent()
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        long newCount = countdownFrom.getAsInt();

        public CountdownState() {
            setMessage(getCountdownMessage(newCount));
        }

        @Override
        public void update() {
            long elapsedTime = System.currentTimeMillis() - transitionTime;
            if (elapsedTime >= 1000) { // a second has passed
                transitionTime = System.currentTimeMillis(); // reset transitionTime
                newCount--;
                if (newCount <= 0) {
                    // Stop the countdown when it reaches 0
                    transition();
                } else {
                    setMessage(initialMessage + ": " + newCount);
                }
            }
        }

        @Override
        public void transition() {
            interruptCountdown();
            state = new FadeOutState();
            transitionTime = System.currentTimeMillis();
        }
    }

    private class FadeOutState implements State {
        @Override
        public void update() {
            long elapsedTime = System.currentTimeMillis() - transitionTime;

            // can't be in FadeOutState unless we have a fadeOutDuration - need for the IDE warning
            //noinspection OptionalGetWithoutIsPresent
            fadeValue = PApplet.constrain((int) PApplet.map(elapsedTime, 0, fadeOutDuration.getAsInt(), 255, 0), 0, 255);
            if (elapsedTime >= fadeOutDuration.getAsInt()) {
                transition();
            }
        }

        @Override
        public void transition() {
            removeFromDrawableList();
        }
    }
}