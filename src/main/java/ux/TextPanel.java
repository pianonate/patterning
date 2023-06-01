package ux;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.util.*;
import java.util.function.IntSupplier;

public class TextPanel extends Panel implements Drawable {

    private final static UXThemeManager theme = UXThemeManager.getInstance();
    private final static DrawableManager drawableManager = DrawableManager.getInstance();
    public final boolean outline;
    // sizes
    private final int textMargin = theme.getDefaultTextMargin();
    private final int doubleTextMargin = textMargin * 2;
    private final float textSize;
    // optional capabilities
    private final OptionalInt textWidth;
    private final Optional<IntSupplier> textWidthSupplier;
    private final boolean wrap;
    private final OptionalInt fadeInDuration;
    private final OptionalInt fadeOutDuration;
    private final OptionalLong displayDuration; // long to compare to System.currentTimeMillis()
    // text countdown variables
    private final OptionalInt countdownFrom;
    private final Runnable runMethod;
    private final String initialMessage;
    private final boolean keepKeyboardShortcutTogether;
    private long transitionTime;
    private int fadeValue;
    // the message
    private String message;
    private String lastMessage;
    private List<String> messageLines;
    private State state;

    protected TextPanel(TextPanel.Builder builder) {
        super(builder);
        // construct the TextPanel with the default Panel constructor
        // after that we'll figure out the variations we need to support
        // super(builder.alignHorizontal, builder.vAlign);

        this.message = builder.message;
        this.lastMessage = builder.message;

        // just for keyboard shortcuts for now
        this.keepKeyboardShortcutTogether = builder.keepKeyboardShortcutTogether;
        this.outline = builder.outline;

        this.textSize = builder.textSize;
        this.textWidth = builder.textWidth;
        this.textWidthSupplier = builder.textWidthSupplier;
        this.wrap = builder.wrap;

        this.displayDuration = builder.displayDuration;

        this.fadeInDuration = builder.fadeInDuration;
        this.fadeOutDuration = builder.fadeOutDuration;

        // text countdown variables
        this.runMethod = builder.runMethod;
        this.countdownFrom = builder.countdownFrom;
        this.initialMessage = builder.message;


        // Always wrap the text, even if it results in a single line
        updatePanelBuffer(graphicsSupplier.get(), true);

        //this.setFill(0xFFFF0000);

        // automatically start the display unless we're a countdown
        // which needs to be manually invoked by the caller...
        if (countdownFrom.isPresent()) {
            startCountdown();
        } else {
            startDisplay();
        }
    }

    private void updatePanelBuffer(PGraphics parentBuffer, boolean shouldUpdate) {
        if (shouldUpdate) {
            panelBuffer = getTextPanelBuffer(parentBuffer);
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

    protected PGraphics getTextPanelBuffer(PGraphics parentBuffer) {

        // ensure that the text size is set to the correct value for the wrapping exercise
        setFont(parentBuffer, textSize);

        String testMessage = (countdownFrom.isPresent()) ?
                getCountdownMessage(countdownFrom.getAsInt()) : message;

        messageLines = wrapText(testMessage, parentBuffer);


        // Adjust the text size if it exceeds the bounds of the screen
        float adjustedTextSize = getAdjustedTextSize(parentBuffer, this.messageLines, textSize);

        // Compute the maximum width and total height of all lines in case there is
        // word wrapping
        float maxWidth = 0;
        float totalHeight = 0;
        for (String line : this.messageLines) {
            if (parentBuffer.textWidth(line) > maxWidth) {
                maxWidth = parentBuffer.textWidth(line);
            }
            totalHeight += parentBuffer.textAscent() + parentBuffer.textDescent();
        }

        width = (int) Math.ceil(maxWidth + doubleTextMargin);

        // Adjust the width and height according to the size of the wrapped text
        height = (int) Math.ceil(totalHeight + textMargin);


        PGraphics textBuffer = parentBuffer.parent.createGraphics(width, height);

        // set the font for this PGraphics as it will not change
        textBuffer.beginDraw();
        //textBuffer.textAlign(PApplet.LEFT, PApplet.TOP);
        setFont(textBuffer, adjustedTextSize);

        textBuffer.endDraw();

        return textBuffer;
    }

    public void setMessage(String message) {
        lastMessage = this.message;
        this.message = message;
    }

    // for sizes to be correctly calculated, the font must be the same
    // on both the parent and the new textBuffer
    // necessary because createGraphics doesn't inherit the font from the parent
    private void setFont(PGraphics buffer, float textSize) {

        TextPanelInformer informer = (TextPanelInformer) graphicsSupplier;

        boolean shouldInitialize = !informer.isDrawing();

        if (shouldInitialize) buffer.beginDraw();
        buffer.textFont(buffer.parent.createFont(theme.getFontName(), textSize));
        buffer.textSize(textSize);
        if (shouldInitialize) buffer.endDraw();


    }

    private String getCountdownMessage(long count) {
        return initialMessage + ": " + count;
    }

    private List<String> wrapText(String theMessage, PGraphics buffer) {
        List<String> words = new ArrayList<>(Arrays.asList(theMessage.split(" ")));
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        OptionalInt textWidthValue = getTextWidth();

        if (textWidthValue.isEmpty() || !this.wrap) {
            lines.add(theMessage);
            return lines;
        }

        while (!words.isEmpty()) {
            String word = words.get(0);
            float prospectiveLineWidth = buffer.textWidth(line + word);

            // If the word alone is wider than the wordWrapWidth, it should be put on its own line
            if (prospectiveLineWidth > textWidthValue.getAsInt() && line.length() == 0) {
                line.append(word).append(" ");
                words.remove(0);
            }
            // Otherwise, if it fits with the current line, add it to the line
            else if (prospectiveLineWidth <= textWidthValue.getAsInt()) {
                line.append(word).append(" ");
                words.remove(0);
            }
            // If it doesn't fit, move to the next line
            else {
                lines.add(line.toString().trim());
                line = new StringBuilder();
            }

            if (keepKeyboardShortcutTogether) {
                // Check if there are exactly two words remaining and they don't fit on the current line
                if (words.size() == 2 && buffer.textWidth(line.toString() + words.get(0) + " " + words.get(1)) > textWidthValue.getAsInt()) {
                    // Add the current line to the lines list
                    lines.add(line.toString().trim());
                    line = new StringBuilder();

                    // Add the last word to the new line
                    line.append(words.get(0)).append(" ");
                    words.remove(0);
                }
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString().trim());
        }

        return lines;
    }

    // used to make sure that a long line will fit on the screen at this size
    private float getAdjustedTextSize(PGraphics parentBuffer, List<String> lines, float startingSize) {
        // inherently we can have word wrapped text so make sure that we find the longest line to get the width

        parentBuffer.textSize(startingSize);

        float adjustedTextSize = startingSize;
        String longestLine = "";

        // Determine the longest line
        for (String line : lines) {
            if (parentBuffer.textWidth(line) > parentBuffer.textWidth(longestLine)) {
                longestLine = line;
            }
        }

        // fit within the width minus a margin
        while ((parentBuffer.textWidth(longestLine) > (parentBuffer.width - doubleTextMargin))
                || ((parentBuffer.textAscent() + parentBuffer.textDescent()) > (parentBuffer.height - doubleTextMargin))) {
            adjustedTextSize -= .05; // smooth baby
            adjustedTextSize = Math.max(adjustedTextSize, 1); // Prevent the textSize from going below 1
            parentBuffer.textSize(adjustedTextSize);
        }
        return adjustedTextSize;
    }

    private OptionalInt getTextWidth() {
        if (textWidth.isPresent()) {
            return textWidth;
        } else
            return textWidthSupplier.map(intSupplier -> OptionalInt.of(intSupplier.getAsInt())).orElseGet(OptionalInt::empty);
    }

    private boolean shouldUpdatePanelBuffer() {
        return resized && textWidthSupplier.isPresent();
    }

    protected void panelSubclassDraw() {

        // used for fading in the text and the various states
        // a ux.TextPanel can advance through
        state.update();

        // we update the size of the buffer containing the text
        // if we've resized && there is a supplier of an integer telling us the size of the text can change
        // for example the countdown text is half the screen width so we want to give it a new buffer
        boolean shouldUpdate = resized && textWidthSupplier.isPresent();
        updatePanelBuffer(graphicsSupplier.get(), shouldUpdate);

        // and if the test actually changed - or in the case of updating the buffer size on resize
        // let's update the word wrapping and font size
        if (!Objects.equals(lastMessage, message) || shouldUpdate) {
            messageLines = wrapText(message, graphicsSupplier.get());
            setFont(panelBuffer, getAdjustedTextSize(graphicsSupplier.get(), messageLines, textSize));
        }

        drawMultiLineText();
    }

    void drawMultiLineText() {
        // Get the colors every time in case the UX theme changes
        int outlineColor = theme.getTextColorStart(); // black
        // Interpolate between "black" 0xff000000 and "white" (0xffffffff)
        // fade values goes from 0 to 255 to make this happen
        int currentColor = panelBuffer.lerpColor(outlineColor, theme.getTextColor(), fadeValue / 255.0F);

        // Draw black text slightly offset in each direction to create an outline effect
        float outlineOffset = 1.0F;
        int margin = textMargin;

        panelBuffer.beginDraw();

        panelBuffer.textAlign(this.hAlign.toPApplet(), this.vAlign.toPApplet());

        // Determine where to start drawing the text based on the alignment
        float x = margin;
        float y = 0;


        switch (hAlign) {
            case LEFT -> x = margin;
            case CENTER -> x = panelBuffer.width / 2f;
            case RIGHT -> x = panelBuffer.width - margin;
        }

// Determine the starting y position based on the alignment
        float lineHeight = panelBuffer.textAscent() + panelBuffer.textDescent();
        float totalTextHeight = lineHeight * messageLines.size();

        switch (vAlign) {
            case CENTER -> y = (panelBuffer.height / 2f) - (totalTextHeight / 2f) + doubleTextMargin;
            case BOTTOM -> y = panelBuffer.height - textMargin;
        }


        for (int i = 0; i < messageLines.size(); i++) {
            String line = messageLines.get(i);
            float lineY = y + (lineHeight * i);

            if (outline) {
                panelBuffer.fill(outlineColor);
                panelBuffer.text(line, x - outlineOffset, lineY - outlineOffset);
                panelBuffer.text(line, x + outlineOffset, lineY - outlineOffset);
            }

            // Draw the actual text in the calculated color
            panelBuffer.fill(currentColor);
            panelBuffer.text(line, x, lineY);
        }

        panelBuffer.endDraw();
    }

    public void interruptCountdown() {
        if (runMethod != null) runMethod.run();

        removeFromDrawableList();
    }

    private void removeFromDrawableList() {
        drawableManager.remove(this);
    }

    private interface State {
        void update();

        @SuppressWarnings("unused")
        void transition();
    }

    public static class Builder extends Panel.Builder<Builder> {

        private static final UXThemeManager theme = UXThemeManager.getInstance();
        private final String message;
        private boolean outline = true;
        private boolean wrap = false;
        private float textSize = theme.getDefaultTextSize();
        private OptionalInt fadeInDuration = OptionalInt.empty();
        private OptionalInt fadeOutDuration = OptionalInt.empty();

        private OptionalLong displayDuration = OptionalLong.empty();
        // Countdown variables
        private OptionalInt countdownFrom = OptionalInt.empty();

        private OptionalInt textWidth = OptionalInt.empty();
        private Optional<IntSupplier> textWidthSupplier = Optional.empty();

        private Runnable runMethod;
        private boolean keepKeyboardShortcutTogether = false;

        public Builder(TextPanelInformer informer, String message, AlignHorizontal alignHorizontal, AlignVertical vAlign) {
            super(informer, alignHorizontal, vAlign);
            this.message = message;
        }

        public Builder(TextPanelInformer informer, String message, PVector position, AlignHorizontal hAlign, AlignVertical vAlign) {
            super(informer, position, hAlign, vAlign);
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

        public Builder textWidth(int textWidth) {
            this.textWidth = OptionalInt.of(textWidth);
            if (textWidthSupplier.isPresent())
                throw new IllegalStateException("Cannot set both wordWrapWidth and wordWrapWidthSupplier");
            return this;
        }

        public Builder textWidth(Optional<IntSupplier> textWidth) {
            textWidthSupplier = textWidth;
            if (this.textWidth.isPresent())
                throw new IllegalStateException("Cannot set both wordWrapWidth and wordWrapWidthSupplier");
            return this;
        }

        public Builder wrap() {
            this.wrap = true;
            return this;
        }

        public Builder keepShortCutTogether() {
            this.keepKeyboardShortcutTogether = true;
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

        public Builder outline(boolean outline) {
            this.outline = outline;
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