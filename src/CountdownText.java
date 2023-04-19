import processing.core.PApplet;

public class CountdownText {
    private final PApplet parent;
    String message;
    int textSize;
    int textColor;
    int fadeInDuration;
    int countdownFrom;
    int currentCount;
    boolean isCountingDown;
    private int startTime;
    int fadeValue;
    Runnable registeredMethod;

    CountdownText(PApplet parent, Runnable registeredMethod, String message) {
        this(parent, registeredMethod, message, 30, 0xFF000000, 1500, 3);
    }

    CountdownText(PApplet parent, Runnable registeredMethod, String message, int textSize, int textColor, int fadeInDuration, int countdownFrom) {
        this.parent = parent;
        this.registeredMethod = registeredMethod;
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
        startTime = parent.millis();
        isCountingDown = true;
    }

    void update() {
        if (isCountingDown) {
            int elapsedTime = parent.millis() - startTime;
            fadeValue = PApplet.constrain((int)PApplet.map(elapsedTime, 0, fadeInDuration, 0, 255), 0, 255);

            if (elapsedTime > fadeInDuration) {
                int countdownElapsed = elapsedTime - fadeInDuration;
                int newCount = countdownFrom - countdownElapsed / 1000;

                if (newCount < currentCount) {
                    currentCount = newCount;
                }

                if (currentCount < 0) {
                    isCountingDown = false;
                    registeredMethod.run();
                }
            }
        }
    }

    public void draw() {
        if (isCountingDown) {
            parent.textAlign(PApplet.CENTER, PApplet.CENTER);
            parent.textSize(textSize);
            parent.fill(textColor, fadeValue);

            // Display the message and the countdown number
            String displayText = message + " " + currentCount;
            parent.text(displayText, parent.width / 2, parent.height / 2);
        }
    }

    public void interruptCountdown() {
        if (isCountingDown) {
            isCountingDown = false;
            registeredMethod.run();
        }
    }
}
