public class ThrottleController implements FrameRateListener {
    private final static float DEGRADATION_FACTOR = 1.5f;
    private final static int SCALING_FACTOR = 2;
    private final static int SKIP_FRAMES = 1;

    private final String throttleName;
    private final float lowerFrameRateThreshold;
    private final float higherFrameRateThreshold;
    private final int recoveryThreshold;

    private int scalingFactor = 0;
    private int framesToSkip = 0;
    private int currentSkip = 0;
    private long lastRecoveryTime = 0;
    private boolean hasImproved = false;
    private float lastFrameRate = 0;
    private boolean hasChangedDirection = false;
    private boolean hasRecovered = true;
    private long lastOutputTime = 0;

    private boolean shouldProceed;

    public ThrottleController(String throttleName, float lowerFrameRateThreshold, float higherFrameRateThreshold, int recoveryThreshold) {
        this.throttleName = throttleName;
        this.lowerFrameRateThreshold = lowerFrameRateThreshold;
        this.higherFrameRateThreshold = higherFrameRateThreshold;
        this.recoveryThreshold = recoveryThreshold;
    }

    public boolean shouldProceed() {
        return shouldProceed;
    }

    @Override
    public void onFrameRateUpdate(float frameRate) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRecoveryTime;
        boolean improving = frameRate > lastFrameRate;

        if (frameRate <= higherFrameRateThreshold) {
            hasRecovered=false;

            if (hasImproved != improving) {
                hasChangedDirection = true;
                hasImproved = improving;
            } else {
                hasChangedDirection = false;
            }

            if (hasChangedDirection || (currentTime - lastOutputTime) >= 1000) {
                /* System.out.println(String.format("frameRate: %.1f %s for: %s framesToSkip: %d currentSkip: %d",
                        frameRate,
                        improving ? "improving" : "not improving",
                        throttleName,
                        framesToSkip,
                        currentSkip)); */

                lastOutputTime = currentTime;
            }

            if (frameRate < lowerFrameRateThreshold && elapsedTime >= recoveryThreshold) {
                scalingFactor += SCALING_FACTOR;
                framesToSkip += scalingFactor;
                lastRecoveryTime = currentTime;
            } else if (improving) {
                framesToSkip = framesToSkip > 0 ? (int) (framesToSkip / DEGRADATION_FACTOR) : 0;
            } else {
                scalingFactor += (int) SCALING_FACTOR / 2;
                framesToSkip += scalingFactor;
            }


        } else { // frameRate > higherFrameRateThreshold

            if (!hasRecovered) {
                scalingFactor = 0;
                framesToSkip = 0;
                lastRecoveryTime = currentTime;
                hasRecovered = true;
                System.out.printf("frameRate: %.1f for %s recovered%n", frameRate, throttleName);
            }
            hasImproved = true;
        }

        lastFrameRate = frameRate;

        if (currentSkip >= framesToSkip) {
            currentSkip = 0;
           shouldProceed = true;
        } else {
            currentSkip += SKIP_FRAMES;
            shouldProceed = false;
        }
    }
}
