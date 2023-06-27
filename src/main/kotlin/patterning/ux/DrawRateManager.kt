package patterning.ux

object DrawRateManager {
    const val MAX_FRAME_RATE = 128.0f
    private const val SINGLE_STEP_SPEED_CHANGE_THRESHOLD = 4
    private const val DRAW_RATE_STARTING = 32f
    private val speedValues = intArrayOf(1, 2, SINGLE_STEP_SPEED_CHANGE_THRESHOLD, 8, 16, 32, 64)
    private var speedIndex = speedValues.indexOf(DRAW_RATE_STARTING.toInt())

    private var frameCounter = 0
    // true if the frame counter is past the warm up period
    private val isWarmedUp get() = frameCounter >= DRAW_RATE_STARTING.toInt()


    private var targetDrawRate = DRAW_RATE_STARTING
    private var lastKnownRequestedDrawRate = DRAW_RATE_STARTING
    var currentDrawRate = DRAW_RATE_STARTING
        private set
    private var numFramesToChangeRate = 0
    private var frameRateChangeIncrement = 0f
    private var rateChangeCounter = 0

    private var drawCounter = 0f
    private var drawImmediately = false

    // sentinel to speed up or slow down
    private var speedChangeActive = false
    private var currentFrameRate = 0f

    var actualDrawRate = 0
    private var lastUpdateTime = System.currentTimeMillis()


    fun performDraw() {
        actualDrawRate++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= 1000) { // Check if a second has passed
            println("Actual draw rate: $actualDrawRate frames/sec")
            actualDrawRate = 0
            lastUpdateTime = currentTime
        }
    }

    fun goFaster() {
        if (speedIndex < speedValues.size - 1) {
            speedIndex++
            targetDrawRate = speedValues[speedIndex].toFloat().also {
                lastKnownRequestedDrawRate = it
            }
        }
        adjustSpeedForFrameRate()
        speedChangeActive = true
    }

    fun goSlower() {
        speedIndex = when {
            speedIndex > 0 -> speedIndex - 1
            else -> 0
        }
        targetDrawRate = speedValues[speedIndex].toFloat().also {
            lastKnownRequestedDrawRate = it
        }
        adjustSpeedForFrameRate()
        speedChangeActive = true
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

        // todo: look into at doing this code more functionally
        var closestIndex = -1
        var closestDiff = Float.MAX_VALUE

        for (i in speedValues.indices) {
            if (speedValues[i] <= currentFrameRate) {
                val diff = currentFrameRate - speedValues[i]
                if (diff < closestDiff) {
                    closestDiff = diff
                    closestIndex = i
                }
            }
        }

        speedIndex = 0.coerceAtLeast(closestIndex)
        targetDrawRate = speedValues[speedIndex].toFloat()

    }

    fun drawImmediately() {
        drawImmediately = true
    }

    fun shouldDraw(frameRate: Float): Boolean {

        adjustDrawRate(frameRate)

        if (!isWarmedUp) return true

        if (drawImmediately) {
            drawImmediately = false // Reset it for the next calls
            return true
        }
        drawCounter += currentDrawRate / currentFrameRate.coerceAtLeast(1f)
        if (drawCounter >= 1.0) {
            drawCounter--
            return true
        }
        return false
    }

    private fun adjustDrawRate(frameRate: Float) {
        // Increase the frame counter each time this method is called
        frameCounter++

        // Don't adjust the draw rate until the warm up period has passed
        if (!isWarmedUp) return

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
                frameRate.coerceAtLeast(1f).toInt()
            }
            frameRateChangeIncrement = (targetDrawRate - currentDrawRate) / frameRate
        }
        rateChangeCounter++
        currentDrawRate += frameRateChangeIncrement

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
}