package ux;

import processing.core.PApplet;
public class DrawRateController {
    public static final float MAX_FRAME_RATE = 120.0F;
    private static final float DRAW_RATE_STARTING = 30f;
    private static final double DRAW_RATE_INCREMENT = 0.1;
    private float targetDrawRate;
    private float currentDrawRate;
    private float drawCounter;

    // this is used by PApplet in patterning.Patterning class - it's also used here
    // as a limit to the maximum draw rate which can't happen faster than the max
    // frame rate, n'est-ce pas?

    public DrawRateController() {
        this.currentDrawRate = DRAW_RATE_STARTING;
        this.targetDrawRate = DRAW_RATE_STARTING;
        this.drawCounter = 0;
    }

    public void updateTargetDrawRate(float newDrawRate) {
        this.targetDrawRate = PApplet.constrain(newDrawRate, 1/MAX_FRAME_RATE, MAX_FRAME_RATE);
    }

    public boolean shouldDraw() {
        drawCounter += currentDrawRate / MAX_FRAME_RATE;
        if (drawCounter >= 1.0) {
            drawCounter--;
            return true;
        }
        return false;
    }

    public float getCurrentDrawRate() {
        return currentDrawRate;
    }

    public void adjustDrawRate() {
        if (currentDrawRate < targetDrawRate) {
            currentDrawRate += DRAW_RATE_INCREMENT; // Increment step
        } else if (currentDrawRate > targetDrawRate) {
            currentDrawRate -= DRAW_RATE_INCREMENT; // Decrement step
        }
        currentDrawRate = PApplet.constrain(currentDrawRate, 1/MAX_FRAME_RATE, MAX_FRAME_RATE);
    }
}
