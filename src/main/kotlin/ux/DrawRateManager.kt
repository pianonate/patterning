package ux

class DrawRateManager private constructor() {
    private var targetDrawRate: Float
    var lastKnownRequestedDrawRate: Float
        private set
    var currentDrawRate: Float
        private set
    private var numFramesToChangeRate = 0
    private var frameRateChangeIncrement = 0f
    private var rateChangeCounter = 0
    private var speedIndex // Position in the SPEED_UP_VALUES or SLOW_DOWN_VALUES array
            = 8
    private var drawCounter = 0f
    private var drawImmediately = false

    // sentinel to speed up or slow down
    private var speedChangeActive = false

    // we don't want the behavior to be automatically invoked until there has been
    // a change by a human to a specific value
    // better to store the last known human speedIndex request and
    // and try to return to it with the "auto" behavior
    // private boolean firstChangeInvoked = false;
    private var currentFrameRate = 0f

    // this is used by PApplet in patterning.Patterning class - it's also used here
    // as a limit to the maximum draw rate which can't happen faster than the max
    // frame rate, n'est-ce pas?
    init {
        currentDrawRate = DRAW_RATE_STARTING
        targetDrawRate = DRAW_RATE_STARTING
        lastKnownRequestedDrawRate = DRAW_RATE_STARTING
    }

    fun goFaster() {
        if (speedIndex < SPEED_VALUES.size - 1) {
            speedIndex++
            targetDrawRate = SPEED_VALUES[speedIndex].toFloat()
            lastKnownRequestedDrawRate = targetDrawRate
        }
        adjustSpeedForFrameRate()
        speedChangeActive = true
        // firstChangeInvoked = true;
    }

    fun goSlower() {
        if (speedIndex > SPEED_VALUES.size / 2) {
            speedIndex -= 2
        } else if (speedIndex > 0) {
            speedIndex--
        } else {
            speedIndex = 0
        }
        targetDrawRate = SPEED_VALUES[speedIndex].toFloat()
        lastKnownRequestedDrawRate = targetDrawRate
        adjustSpeedForFrameRate()
        speedChangeActive = true
        //  firstChangeInvoked = true;
    }

    private fun adjustSpeedForFrameRate() {

        // this lets adjustSpeedForFrameRate to adjust back
        // to the last human requested value
        // took a while to work out this logic!!
        if (targetDrawRate != lastKnownRequestedDrawRate && lastKnownRequestedDrawRate < currentFrameRate) {
            targetDrawRate = lastKnownRequestedDrawRate
        }

        // no need to adjust if we're within bounds
        if (targetDrawRate < currentFrameRate) {
            return
        }
        var closestIndex = -1
        var closestDiff = Float.MAX_VALUE
        for (i in SPEED_VALUES.indices) {
            if (SPEED_VALUES[i] <= currentFrameRate) {
                val diff = currentFrameRate - SPEED_VALUES[i]
                if (diff < closestDiff) {
                    closestDiff = diff
                    closestIndex = i
                }
            }
        }
        speedIndex = Math.max(0, closestIndex)
        targetDrawRate = SPEED_VALUES[speedIndex].toFloat()
    }

    fun drawImmediately() {
        drawImmediately = true
    }

    fun shouldDraw(): Boolean {
        if (drawImmediately) {
            drawImmediately = false // Reset it for the next calls
            return true
        }
        drawCounter += currentDrawRate / currentFrameRate
        if (drawCounter >= 1.0) {
            drawCounter--
            return true
        }
        return false
    }

    fun adjustDrawRate(frameRate: Float) {
        currentFrameRate = frameRate
        if (!speedChangeActive) {
            // check if either we targeted faster than the current frameRate
            // or if there has been an inadvertent change by this automated behavior
            // because the frameRate slowed down - and so the last known human value is
            // in either case we want to adjustSpeed to get back in sync
            speedChangeActive = if (frameRate < targetDrawRate || targetDrawRate != lastKnownRequestedDrawRate) {
                adjustSpeedForFrameRate()
                true
            } else return  // nothing to do so just bail
        }

        // fall through as we're in active speed change mode either by design or
        // by the automated test above

        // we want to speed change over a about the period of 1 second
        // so we just determine how many frames that will take (the frameRate, duh)
        // or faster if it's for a single step change

        // if the target is different, then initialized an increment
        if (numFramesToChangeRate == 0) {
            numFramesToChangeRate = if (targetDrawRate < SINGLE_STEP_SPEED_CHANGE_THRESHOLD) {
                1
            } else {
                // change immediately at lower rates
                // make sure that it's at least 1 otherwise nothing will happen
                Math.max(frameRate, 1f).toInt()
            }
            frameRateChangeIncrement = (targetDrawRate - currentDrawRate) / frameRate
        }
        rateChangeCounter++
        currentDrawRate = currentDrawRate + frameRateChangeIncrement

        // cleanup on completion
        if (rateChangeCounter == numFramesToChangeRate) {
            // the increment is floating point so just make them exactly the same
            currentDrawRate = targetDrawRate
            numFramesToChangeRate = 0
            rateChangeCounter = 0
            frameRateChangeIncrement = 0f
            speedChangeActive = false
        }
    }

    companion object {
        const val MAX_FRAME_RATE = 128.0f
        private const val DRAW_RATE_STARTING = 32f
        private const val SINGLE_STEP_SPEED_CHANGE_THRESHOLD = 4
        private val SPEED_VALUES = intArrayOf(1, 2, SINGLE_STEP_SPEED_CHANGE_THRESHOLD, 8, 16, 32, 64, 128)

        // The single instance of the class
        @JvmStatic
        var instance: DrawRateManager? = null
            // Static method to return the instance of the class
            get() {
                if (field == null) {
                    field = DrawRateManager()
                }
                return field
            }
            private set
    }
}