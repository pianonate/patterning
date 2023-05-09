import processing.core.PApplet; // todo: could make this a static field on GameOfLife so we don't have to pass it around...
import processing.core.PGraphics;

public class CountdownText {
    private final PGraphics buffer;
    String message;
    int textSize;
    int textColor;
    int fadeInDuration;
    int countdownFrom;
    long currentCount;
    boolean isCountingDown;
    private long startTime;
    int fadeValue;
    Runnable interruptMethod;
    Runnable runMethod;

    CountdownText(PApplet parent, PGraphics buffer, Runnable runMethod, Runnable interruptMethod, String message) {
        this(parent, buffer, runMethod, interruptMethod, message, 30, 0xFF000000, 1500, 3);
    }

    CountdownText(PApplet parent, PGraphics buffer, Runnable runMethod, Runnable interruptMethod, String message, int textSize, int textColor, int fadeInDuration, int countdownFrom) {
        this.buffer = buffer;
        this.runMethod = runMethod;
        this.interruptMethod = interruptMethod;
        this.message = message;
        this.textSize = textSize;
        this.textColor = textColor;
        this.fadeInDuration = fadeInDuration;
        this.countdownFrom = countdownFrom;
        this.currentCount = countdownFrom;
        this.isCountingDown = false;
        this.startTime = 0;
        this.fadeValue = 0;
    }

    void startCountdown() {
        startTime = System.currentTimeMillis();
        isCountingDown = true;
    }

    void update() {
        if (isCountingDown) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            fadeValue = PApplet.constrain((int)PApplet.map(elapsedTime, 0, fadeInDuration, 0, 255), 0, 255);

            if (elapsedTime > fadeInDuration) {
                long countdownElapsed = elapsedTime - fadeInDuration;
                long newCount = countdownFrom - countdownElapsed / 1000;

                if (newCount < currentCount) {
                    currentCount = newCount;
                }

                // kick off the thing you've been asked to kickoff
                if (currentCount < 0) {
                    isCountingDown = false;
                    runMethod.run();
                }
            }
        }
    }

    public void draw() {
        if (isCountingDown) {
            buffer.textAlign(PApplet.CENTER, PApplet.CENTER);
            buffer.textSize(textSize);
            buffer.fill(textColor, fadeValue);

            // Display the message and the countdown number
            String displayText = message + " " + currentCount;
            buffer.text(displayText, buffer.width / 2, buffer.height / 2);
        }
    }

    public void interruptCountdown() {
        if (isCountingDown) {
            isCountingDown = false;
            interruptMethod.run();
        }
    }
}
