import processing.core.PApplet;
import processing.core.PGraphics;

public class TextDisplay {

    private static final int MARGIN = 10; // 10 pixel buffer for top positions
    private static final int FADE_IN_DURATION = 500;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SIZE = 30;
    private static final int COUNTDOWN_FROM = 3;
    // non-countdown variables
    protected String message;
    private final int textSize;
    private final int textColor;
    private final int fadeInDuration;
    private final Position position;
    private long startTime;
    private int fadeValue;
    protected boolean isDisplaying = false;

    // Countdown variables
    private final Runnable runMethod;
    private final int countdownFrom;
    private long currentCount;
    private final String initialMessage;
    private boolean isCountingDown = false;

    private final long duration;

    protected TextDisplay(TextDisplay.Builder builder) {
        this.message = builder.message;
        this.position = builder.position;
        this.textSize = builder.textSize;
        this.textColor = builder.textColor;
        this.fadeInDuration = builder.fadeInDuration;
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
        this.startTime = System.currentTimeMillis(); // start displaying immediately
    }

    public void stopDisplay() {
        this.isDisplaying = false;
        this.isCountingDown = false;
    }

    // todo - you got an updated algo to put a black border around the text so make sure that it tests against dense white drawings
    // todo - make all of these color choices global constants on the main drawer (not Patterning as it will
    //  have enough responsibilities and it extends PApplet anyway so that's a pain in arse to wade through so much
    // then make it so that this code adapts to whatever is chosen as the background color
    public void draw(PGraphics buffer) {
        if (!isDisplaying) {
            return;
        }

        // used for fading in the text
        long elapsedTime = System.currentTimeMillis() - startTime;

        if (duration > 0 && elapsedTime > duration) {
            stopDisplay();
            return;
        }

        float x = 0.0F;
        float y = 0.0F;

        switch (position) {

            case BOTTOM_RIGHT -> {
                x = buffer.width - MARGIN;
                y = buffer.height - MARGIN;
                buffer.textAlign(PApplet.RIGHT, PApplet.BOTTOM);
            }

            case CENTER -> {
                x = buffer.width / 2.0F;
                y = buffer.height / 2.0F;
                buffer.textAlign(PApplet.CENTER, PApplet.CENTER);

            }
            case TOP_LEFT -> {
                x = MARGIN;
                y = MARGIN;
                buffer.textAlign(PApplet.LEFT, PApplet.TOP);
            }
            case TOP_CENTER -> {
                x = buffer.width / 2.0F;
                y = MARGIN;
                buffer.textAlign(PApplet.CENTER, PApplet.TOP);

            }
            case TOP_RIGHT -> {
                x = buffer.width - MARGIN;
                y = MARGIN;
                buffer.textAlign(PApplet.RIGHT, PApplet.TOP);

            }
        }

        fadeValue = PApplet.constrain((int) PApplet.map(elapsedTime, 0, fadeInDuration, 0, 255), 0, 255);

        // handle countdown logic
        countdown(elapsedTime);

        buffer.textSize(textSize);

        int currentColor = buffer.lerpColor(0xff000000, textColor, fadeValue / 255.0f);

        // Draw black text slightly offset in each direction to create an outline effect
        int outlineColor = 0xff000000; // black
        float outlineOffset = 1.0F;

        int adjustedTextSize = textSize;

        while ((buffer.textWidth(message) + (2 * MARGIN) > buffer.width)
                || ((buffer.textAscent() + buffer.textDescent()) + (2 * MARGIN) > buffer.height)) {
            adjustedTextSize--;
            adjustedTextSize =  Math.max(adjustedTextSize, 1); // Prevent the textSize from going below 1
            buffer.textSize(adjustedTextSize);
        }

        buffer.fill(outlineColor);
        buffer.text(message, x - outlineOffset, y - outlineOffset);
        buffer.text(message, x + outlineOffset, y - outlineOffset);
        buffer.text(message, x - outlineOffset, y + outlineOffset);
        buffer.text(message, x + outlineOffset, y + outlineOffset);

        // Draw the actual text in the calculated color
        buffer.fill(currentColor);
        buffer.text(message, x, y);
    }

    private void countdown(long elapsedTime) {

        if (isCountingDown && elapsedTime > fadeInDuration) {
            long countdownElapsed = elapsedTime - fadeInDuration;
            long newCount = countdownFrom - countdownElapsed / 1000;

            if (newCount < currentCount) {
                currentCount = newCount;
                setMessage(initialMessage + ": " + (currentCount + 1));
            }

            if (currentCount < 0) {
                interruptCountdown();
            }
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Countdown methods
    public void startCountdown() {
        setMessage(initialMessage);
        this.startDisplay();
        this.currentCount = countdownFrom;
        isCountingDown = true;
    }

    public void interruptCountdown() {
        if (isDisplaying) {
            stopDisplay();
            if (runMethod != null) runMethod.run();
        }
    }

    public enum Position {

        BOTTOM_RIGHT,
        CENTER,
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT
    }

    public static class Builder {
        protected final String message;
        protected final Position position;
        protected int textSize = TEXT_SIZE;
        protected int textColor = TEXT_COLOR;
        protected int fadeInDuration = FADE_IN_DURATION;
        protected int fadeValue = 0;

        // Countdown variables
        private Runnable runMethod;
        private int countdownFrom = COUNTDOWN_FROM;

        protected long duration = -1;

        public Builder(String message, Position position) {
            this.message = message;
            this.position = position;
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
}