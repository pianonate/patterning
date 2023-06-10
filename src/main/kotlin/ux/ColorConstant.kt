package ux;

import processing.core.PApplet;

public class ColorConstant {
    private int currentColor;
    private int previousColor;
    private long transitionStartTime;
    private static final int TRANSITION_DURATION = UXThemeManager.getInstance().getThemeTransitionDuration();  // 2 seconds
    private boolean transitionInProgress = false;
    private PApplet p;
    private boolean isInitialized = false;

    public ColorConstant() {}

    public void setColor(int newColor, PApplet p) {
        if (isInitialized) {
            this.previousColor = this.currentColor;
            this.transitionInProgress = true;
            this.transitionStartTime = System.currentTimeMillis();
            this.p = p;
        } else {
            this.previousColor = newColor;
            this.isInitialized = true;
        }
        this.currentColor = newColor;
    }

    public int getColor() {
        if (p == null || !transitionInProgress) {
            return currentColor;
        }

        float t = (float) (System.currentTimeMillis()- transitionStartTime) / TRANSITION_DURATION;
        if (t < 1.0) {
            return p.lerpColor(previousColor, currentColor, t);
        } else {
            transitionInProgress = false;
            return currentColor;
        }
    }
}
