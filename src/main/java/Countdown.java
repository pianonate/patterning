import processing.core.PGraphics;

public class Countdown extends TextDisplay {
    private static final int FADE_IN_DURATION = 500;
    private static final int COUNTDOWN_FROM = 3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SIZE = 30;
    private final Runnable runMethod;
    private final int countdownFrom;
    private long currentCount;

    private final String initialMessage;  // new property to store the initial message

    Countdown(PGraphics buffer, Runnable runMethod, String message) {
        // Set default values for position, textSize, textColor, fadeInDuration, and countdownFrom
        this(buffer, runMethod, message, Position.CENTER, TEXT_SIZE, TEXT_COLOR, FADE_IN_DURATION, COUNTDOWN_FROM);
    }
    Countdown(PGraphics buffer, Runnable runMethod, String message, Position position, int textSize, int textColor, int fadeInDuration, int countdownFrom) {
        super(buffer, message, position, textSize, textColor, fadeInDuration);
        this.runMethod = runMethod;
        this.countdownFrom = countdownFrom;
        this.currentCount = countdownFrom;
        this.initialMessage = message;
    }

    @Override
    public void draw() {
        if (isDisplaying) {

            long elapsedTime = System.currentTimeMillis() - startTime;

            // only start showing the countDown after the fadeIn has completed
            if (elapsedTime > fadeInDuration) {
                long countdownElapsed = elapsedTime - fadeInDuration;
                long newCount = countdownFrom - countdownElapsed / 1000;

                if (newCount < currentCount) {
                    currentCount = newCount;
                    setMessage(initialMessage + ": " + (currentCount + 1) );
                }

                // If the countdown has reached zero, run the method and stop the display.
                if (currentCount < 0) {
                    runMethod.run();
                    stopDisplay();
                }
            }

            super.draw();
        }
    }

    public void startCountdown() {
        setMessage(initialMessage);
        this.startDisplay();
        this.currentCount = countdownFrom;
    }

    public void interruptCountdown() {
        if (isDisplaying) {
            stopDisplay();
            runMethod.run();
        }
    }
}
