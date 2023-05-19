import processing.core.PApplet;
import processing.core.PGraphics;

public class TextDisplay {

    protected final PGraphics buffer;
    protected String message;
    protected final int textSize;
    protected final int textColor;
    protected final int fadeInDuration;
    protected long startTime;
    protected int fadeValue;
    protected float x, y;
    protected boolean isDisplaying;

    private static final int MARGIN = 10; // 10 pixel buffer for top positions

    public enum Position {
        CENTER,
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT
    }

    public TextDisplay(PGraphics buffer, String message, Position position, int textSize, int textColor, int fadeInDuration) {
        this.buffer = buffer;
        this.message = message;
        this.textSize = textSize;
        this.textColor = textColor;
        this.fadeInDuration = fadeInDuration;
        this.fadeValue = 0;
        this.isDisplaying = false;

        switch (position) {
            case CENTER -> {
                this.x = buffer.width / 2.0F;
                this.y = buffer.height / 2.0F;
            }
            case TOP_LEFT -> {
                this.x = MARGIN;
                this.y = MARGIN;
            }
            case TOP_CENTER -> {
                this.x = buffer.width / 2.0F;
                this.y = MARGIN;
            }
            case TOP_RIGHT -> {
                this.x = buffer.width - MARGIN;
                this.y = MARGIN;
            }
        }
    }

    // The rest of the TextDisplay code remains the same...


    public void startDisplay() {
        this.isDisplaying = true;
        this.startTime = System.currentTimeMillis(); // start displaying immediately
    }

    public void stopDisplay() {
        this.isDisplaying = false;
    }

    // todo - you got an updated algo to put a black border around the text so make sure that it tests against dense white drawings
    // todo - make all of these color choices global constants on the main drawer (not Patterning as it will
    //  have enough responsibilities and it extends PApplet anyway so that's a pain in arse to wade through so much
    // then make it so that this code adapts to whatever is chosen as the background color
    public void draw() {
        if (!isDisplaying) {
            return;
        }

        // used for fading in the text
        long elapsedTime = System.currentTimeMillis() - startTime;
        fadeValue = PApplet.constrain((int)PApplet.map(elapsedTime, 0, fadeInDuration, 0, 255), 0, 255);

        buffer.textAlign(PApplet.CENTER, PApplet.CENTER);
        buffer.textSize(textSize);

        int currentColor = buffer.lerpColor(0xff000000, textColor, fadeValue/255.0f);

        // Draw black text slightly offset in each direction to create an outline effect
        int outlineColor = 0xff000000; // black
        float outlineOffset = 1.0F;

        buffer.fill(outlineColor);
        buffer.text(message, x - outlineOffset, y - outlineOffset);
        buffer.text(message, x + outlineOffset, y - outlineOffset);
        buffer.text(message, x - outlineOffset, y + outlineOffset);
        buffer.text(message, x + outlineOffset, y + outlineOffset);


        // Draw the actual text in the calculated color
        buffer.fill(currentColor);
        buffer.text(message, x, y);
    }

    public void setMessage(String message) {
        this.message = message;
    }
}