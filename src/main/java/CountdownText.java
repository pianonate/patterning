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

    // former
    // 0xFF000000
    CountdownText(PGraphics buffer, Runnable runMethod, Runnable interruptMethod, String message) {
        this(buffer, runMethod, interruptMethod, message, 30, 0xFFFFFFFF, 1500, 3);
    }

    private CountdownText(PGraphics buffer, Runnable runMethod, Runnable interruptMethod, String message, int textSize, int textColor, int fadeInDuration, int countdownFrom) {
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

/*    public void draw() {
        if (isCountingDown) {
            buffer.textAlign(PApplet.CENTER, PApplet.CENTER);
            buffer.textSize(textSize);

            int currentColor = buffer.lerpColor(0xff000000, 0xffffffff, fadeValue / 255.0f); // interpolate between black and white based on fadeValue
            buffer.fill(currentColor);

            // Display the message and the countdown number
            String displayText = message + " " + currentCount;
            buffer.text(displayText, buffer.width / 2.0F, buffer.height / 2.0F);
        }
    }*/

    // todo - you got an updated algo to put a black border around the text so make sure that it tests against dense white drawings
    // todo - make all of these color choices global constants on the main drawer (not Patterning as it will
    //  have enough responsibilities and it extends PApplet anyway so that's a pain in arse to wade thorugh so much
    // then make it so that this code adapts to whatever is chosen as the background color
    public void draw() {
        if (isCountingDown) {
            buffer.textAlign(PApplet.CENTER, PApplet.CENTER);
            buffer.textSize(textSize);

            int currentColor = buffer.lerpColor(0xff000000, 0xffffffff, fadeValue/255.0f); // interpolate between black and white based on fadeValue

            // Display the message and the countdown number
            String displayText = message + " " + currentCount;

            // Draw black text slightly offset in each direction to create an outline effect
            int outlineColor = 0xff000000; // black
            float x = buffer.width / 2.0F;
            float y = buffer.height / 2.0F;
            float outlineOffset = 2.0F; // adjust this to change the thickness of the outline

            buffer.fill(outlineColor);
            buffer.text(displayText, x - outlineOffset, y - outlineOffset);
            buffer.text(displayText, x + outlineOffset, y - outlineOffset);
            buffer.text(displayText, x - outlineOffset, y + outlineOffset);
            buffer.text(displayText, x + outlineOffset, y + outlineOffset);

            // Draw the actual text in the calculated color
            buffer.fill(currentColor);
            buffer.text(displayText, x, y);
        }
    }


    public void interruptCountdown() {
        if (isCountingDown) {
            isCountingDown = false;
            interruptMethod.run();
        }
    }
}
