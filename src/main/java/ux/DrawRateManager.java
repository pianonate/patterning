package ux;

public class DrawRateManager {
    public static final float MAX_FRAME_RATE = 128.0F;
    private static final float DRAW_RATE_STARTING = 32F;
    private static final int SINGLE_STEP_SPEED_CHANGE_THRESHOLD = 4;
    private static final int[] SPEED_VALUES = {1, 2, SINGLE_STEP_SPEED_CHANGE_THRESHOLD, 8, 16, 32, 64,128};
    // The single instance of the class
    private static DrawRateManager instance = null;
    private float targetDrawRate;
    private float lastKnownHumanTargetDrawRate;
    private float currentDrawRate;
    private int numFramesToChangeRate = 0;
    private float frameRateChangeIncrement = 0;
    private int rateChangeCounter = 0;
    private int speedIndex; // Position in the SPEED_UP_VALUES or SLOW_DOWN_VALUES array
    private float drawCounter;
    private boolean drawImmediately = false;

    // sentinel to speed up or slow down
    private boolean speedChangeActive = false;
    // we don't want the behavior to be automatically invoked until there has been
    // a change by a human to a specific value
    // better to store the last known human speedIndex request and
    // and try to return to it with the "auto" behavior
   // private boolean firstChangeInvoked = false;
    private float currentFrameRate;

    // this is used by PApplet in patterning.Patterning class - it's also used here
    // as a limit to the maximum draw rate which can't happen faster than the max
    // frame rate, n'est-ce pas?

    private DrawRateManager() {
        this.currentDrawRate = DRAW_RATE_STARTING;
        this.targetDrawRate = DRAW_RATE_STARTING;
        this.lastKnownHumanTargetDrawRate = DRAW_RATE_STARTING;
        this.drawCounter = 0;
        this.speedIndex = 8;
    }

    // Static method to return the instance of the class
    public static DrawRateManager getInstance() {
        if (instance == null) {
            instance = new DrawRateManager();
        }
        return instance;
    }

    public void goFaster() {

        if (speedIndex < SPEED_VALUES.length - 1) {
            speedIndex++;
            targetDrawRate = SPEED_VALUES[speedIndex];
            lastKnownHumanTargetDrawRate = targetDrawRate;
        }

        adjustSpeedForFrameRate();

        speedChangeActive = true;
       // firstChangeInvoked = true;
    }

    public void goSlower() {
        if (speedIndex > SPEED_VALUES.length / 2) {
            speedIndex -= 2;
        } else if (speedIndex > 0) {
            speedIndex--;
        } else {
            speedIndex = 0;
        }
        targetDrawRate = SPEED_VALUES[speedIndex];
        lastKnownHumanTargetDrawRate = targetDrawRate;

        adjustSpeedForFrameRate();

        speedChangeActive = true;
      //  firstChangeInvoked = true;

    }

    private void adjustSpeedForFrameRate() {

        // this lets adjustSpeedForFrameRate to adjust back
        // to the last human requested value
        // took a while to work out this logic!!
        if ((targetDrawRate != lastKnownHumanTargetDrawRate) &&
            (lastKnownHumanTargetDrawRate < currentFrameRate)
        ) {
            targetDrawRate = lastKnownHumanTargetDrawRate;
        }

        // no need to adjust if we're within bounds
        if (targetDrawRate < currentFrameRate) {
            return;
        }

        int closestIndex = -1;
        float closestDiff = Float.MAX_VALUE;

        for (int i = 0; i < SPEED_VALUES.length; i++) {
            if (SPEED_VALUES[i] <= currentFrameRate) {
                float diff = currentFrameRate - SPEED_VALUES[i];
                if (diff < closestDiff) {
                    closestDiff = diff;
                    closestIndex = i;
                }
            }
        }

        speedIndex = closestIndex;
        targetDrawRate = SPEED_VALUES[speedIndex];
    }

    public void drawImmediately() {
        drawImmediately = true;
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

    public float getLastKnownRequestedDrawRate() {
        return lastKnownHumanTargetDrawRate;
    }

    public void adjustDrawRate(float frameRate) {

        currentFrameRate = frameRate;

        if (!speedChangeActive) {
            // check if either we targeted faster than the current frameRate
            // or if there has been an inadvertent change by this automated behavior
            // because the frameRate slowed down - and so the last known human value is
            // in either case we want to adjustSpeed to get back in sync
            if (frameRate < targetDrawRate || targetDrawRate != lastKnownHumanTargetDrawRate){
                adjustSpeedForFrameRate();
                speedChangeActive = true;
            } else return; // nothing to do so just bail
        }

        // fall through as we're in active speed change mode either by design or
        // by the automated test above

        // we want to speed change over a about the period of 1 second
        // so we just determine how many frames that will take (the frameRate, duh)
        // or faster if it's for a single step change

        // if the target is different, then initialized an increment
        if (numFramesToChangeRate == 0) {
            if (targetDrawRate < SINGLE_STEP_SPEED_CHANGE_THRESHOLD) {
                numFramesToChangeRate = 1;
            } else {
                // change immediately at lower rates
                // make sure that it's at least 1 otherwise nothing will happen
                numFramesToChangeRate = (int) Math.max(frameRate,1);
            }

            frameRateChangeIncrement = (targetDrawRate - currentDrawRate) / frameRate;
        }

        rateChangeCounter++;
        currentDrawRate = currentDrawRate + frameRateChangeIncrement;

        // cleanup on completion
        if (rateChangeCounter == numFramesToChangeRate) {
            // the increment is floating point so just make them exactly the same
            currentDrawRate = targetDrawRate;
            numFramesToChangeRate = 0;
            rateChangeCounter = 0;
            frameRateChangeIncrement = 0;
            speedChangeActive = false;
        }
    }
}