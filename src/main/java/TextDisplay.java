import processing.core.PApplet;
import processing.core.PGraphics;

public class TextDisplay implements Drawable {

    private static final int MARGIN = 10; // 10 pixel buffer for top positions
    private static final int UNSET_DURATION = -1;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SIZE = 30;
    private final int textSize;
    private final int textColor;
    private final int fadeInDuration;
    private final int fadeOutDuration;
    private final long duration;
    private final int hAlign;
    private final int vAlign;
    // Countdown variables
    private final Runnable runMethod;
    private final int countdownFrom;
    private final String initialMessage;
    // non-countdown variables
    protected String message;
    protected boolean isDisplaying = false;
    private State state;
    private long transitionTime;
    private long startTime;
    private int fadeValue;
    private long currentCount;
    private boolean isCountingDown = false;
    protected TextDisplay(TextDisplay.Builder builder) {
        this.message = builder.message;
        this.hAlign = builder.hAlign;
        this.vAlign = builder.vAlign;
        this.textSize = builder.textSize;
        this.textColor = builder.textColor;
        this.fadeInDuration = builder.fadeInDuration;
        this.fadeOutDuration = builder.fadeOutDuration;
        this.fadeValue = builder.fadeValue;

        // Countdown variables
        this.runMethod = builder.runMethod;
        this.countdownFrom = builder.countdownFrom;
        this.currentCount = builder.countdownFrom;
        this.initialMessage = builder.message;

        this.duration = builder.duration;

    }

    public void startDisplay() {
        this.isDisplaying = true;
        this.state = new FadeInState();
        this.transitionTime = System.currentTimeMillis(); // start displaying immediately
    }

    public void draw(PGraphics buffer) {
        if (!isDisplaying) {
            return;
        }

        float x = 0.0F;
        float y = 0.0F;

        switch (this.hAlign) {
            case PApplet.LEFT -> {
                x = 0;
            }
            case PApplet.CENTER -> {
                x = (float) buffer.width / 2;
            }
            case PApplet.RIGHT -> {
                x = buffer.width;
            }
        }
        switch (this.vAlign) {
            case PApplet.TOP -> {
                y = 0;
            }
            case PApplet.CENTER -> {
                y = (float) buffer.height / 2;
            }
            case PApplet.BOTTOM -> {
                y = buffer.height;
            }
        }

        buffer.textAlign(this.hAlign, this.vAlign);

        buffer.textSize(textSize);

        // used for fading in the text and the various states
        // a TextDisplay can advance through
        state.update();

        int currentColor = buffer.lerpColor(0xff000000, textColor, fadeValue / 255.0f);

        // Draw black text slightly offset in each direction to create an outline effect
        int outlineColor = 0xff000000; // black
        float outlineOffset = 1.0F;

        int adjustedTextSize = textSize;

        while ((buffer.textWidth(message) + (2 * MARGIN) > buffer.width)
                || ((buffer.textAscent() + buffer.textDescent()) + (2 * MARGIN) > buffer.height)) {
            adjustedTextSize--;
            adjustedTextSize = Math.max(adjustedTextSize, 1); // Prevent the textSize from going below 1
            buffer.textSize(adjustedTextSize);
        }

        buffer.fill(outlineColor);
        buffer.text(message, x - outlineOffset, y - outlineOffset);
        buffer.text(message, x + outlineOffset, y - outlineOffset);


        // Draw the actual text in the calculated color
        buffer.fill(currentColor);
        buffer.text(message, x, y);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Countdown methods
    public void startCountdown() {
        isCountingDown = true;
        currentCount = countdownFrom;
        setMessage(initialMessage);
        startDisplay();
    }

    public void interruptCountdown() {
        if (isDisplaying) {
            if (runMethod != null) runMethod.run();
            isDisplaying = false; // add this line
            state = new FadeInState(); // reset the state
        }
    }


    private interface State {
        void update();

        void transition();
    }

    public static class Builder {
        protected String message;
        protected int hAlign = PApplet.CENTER;
        protected int vAlign = PApplet.CENTER;
        protected int textSize = TEXT_SIZE;
        protected int textColor = TEXT_COLOR;
        protected int fadeInDuration = UNSET_DURATION;
        protected int fadeOutDuration = UNSET_DURATION;
        protected int fadeValue = 0;
        protected long duration = UNSET_DURATION;
        // Countdown variables
        private Runnable runMethod;
        private int countdownFrom = UNSET_DURATION;

        public Builder(String message, int hAlign, int vAlign) {
            this.message = message;
            this.hAlign = hAlign;
            this.vAlign = vAlign;
        }

        public Builder textSize(int textSize) {
            this.textSize = textSize;
            return this;
        }

        public Builder textColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder fadeInDuration(int fadeInDuration) {
            this.fadeInDuration = fadeInDuration;
            return this;
        }

        public Builder fadeOutDuration(int fadeOutDuration) {
            this.fadeOutDuration = fadeOutDuration;
            return this;
        }

        public Builder countdownFrom(int countdownFrom) {
            this.countdownFrom = countdownFrom;
            return this;
        }

        public Builder runMethod(Runnable runMethod) {
            this.runMethod = runMethod;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public TextDisplay build() {
            return new TextDisplay(this);
        }
    }

    private class FadeInState implements State {
        @Override
        public void update() {
            long elapsedTime = System.currentTimeMillis() - transitionTime;
            if (fadeInDuration > 0) {
                fadeValue = PApplet.constrain((int) PApplet.map(elapsedTime, 0, fadeInDuration, 0, 255), 0, 255);
            } else {
                fadeValue = 255;
            }
            if (elapsedTime >= fadeInDuration) {
                transition();
            }
        }

        @Override
        public void transition() {
            if (countdownFrom > 0) {
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
            if (duration > 0 && elapsedTime > duration) {
                transition();
            }
        }

        @Override
        public void transition() {
            if (fadeOutDuration > 0) {
                state = new FadeOutState();
                transitionTime = System.currentTimeMillis();
            } else {
                isDisplaying = false;
            }
        }
    }

    private class CountdownState implements State {
        long newCount = countdownFrom;

        public CountdownState() {
            setMessage(initialMessage + ": " + newCount);
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
            fadeValue = PApplet.constrain((int) PApplet.map(elapsedTime, 0, fadeOutDuration, 255, 0), 0, 255);
            if (elapsedTime >= fadeOutDuration) {
                transition();
            }
        }

        @Override
        public void transition() {
            isDisplaying = false;
        }
    }
}