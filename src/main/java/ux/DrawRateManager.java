package ux;

import processing.core.PApplet;

public class DrawRateManager {
    public static final float MAX_FRAME_RATE = 120.0F;
    private static final float DRAW_RATE_STARTING = 30f;
    private static final double DRAW_RATE_INCREMENT = 0.1;
    private float targetDrawRate;
    private float currentDrawRate;
    private float drawCounter;

    private boolean drawImmediately = false;

    private float currentFrameRate;

    // The single instance of the class
    private static DrawRateManager instance = null;

    // this is used by PApplet in patterning.Patterning class - it's also used here
    // as a limit to the maximum draw rate which can't happen faster than the max
    // frame rate, n'est-ce pas?

    private DrawRateManager() {
        this.currentDrawRate = DRAW_RATE_STARTING;
        this.targetDrawRate = DRAW_RATE_STARTING;
        this.drawCounter = 0;
    }

    public void drawImmediately() {
        drawImmediately = true;
    }

    public void updateTargetDrawRate(float newDrawRate) {
        this.targetDrawRate = PApplet.constrain(newDrawRate, 1/currentFrameRate, currentFrameRate);
    }

    public boolean shouldDraw() {
        if (drawImmediately) {
            drawImmediately = false; // Reset it for the next calls
            return true;
        }

        drawCounter += currentDrawRate / currentFrameRate;
        if (drawCounter >= 1.0) {
            drawCounter--;
            return true;
        }
        return false;
    }

    public float getCurrentDrawRate() {
        return currentDrawRate;
    }

    public void adjustDrawRate(float frameRate) {
        this.currentFrameRate = frameRate; // Update the current frame rate
        if (currentDrawRate < targetDrawRate) {
            currentDrawRate += DRAW_RATE_INCREMENT; // Increment step
        } else if (currentDrawRate > targetDrawRate) {
            currentDrawRate -= DRAW_RATE_INCREMENT; // Decrement step
        }
        currentDrawRate = PApplet.constrain(currentDrawRate, 1/currentFrameRate, currentFrameRate);
    }

    // Static method to return the instance of the class
    public static DrawRateManager getInstance() {
        if (instance == null) {
            instance = new DrawRateManager();
        }
        return instance;
    }

}
