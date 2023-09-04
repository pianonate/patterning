package patterning.life

import patterning.Properties
import patterning.state.RunningModeController
import patterning.state.RunningModeObserver
import patterning.util.formatWithCommas
import processing.data.JSONObject

class PerformanceTest(private val lifePattern: LifePattern, private val properties: Properties) :
    RunningModeObserver {
    private val performanceResults = JSONObject()
    private val patternCount = 9
    private val framesPerPattern = 400L

    // State for the ongoing test, if any
    private var currentPatternIndex = 1
    private var frameCount = 0L
    private var patternStartTime = 0L
    private var patternMemoryBefore = 0L
    private var testStartTime = 0L
    private var testing = false

    private val runtime = Runtime.getRuntime()

    init {
        RunningModeController.addModeChangeObserver(this)
    }

    override fun onRunningModeChange() {
        if (RunningModeController.isTesting) {
            enterTestMode()
        }
    }

    private fun enterTestMode() {
        // Reset state for new test
        testing = true
        currentPatternIndex = 1
        frameCount = 0
        testStartTime = System.currentTimeMillis()
        patternStartTime = testStartTime
        patternMemoryBefore = runtime.totalMemory() - runtime.freeMemory()
        println("testing framesPerPattern:${framesPerPattern}")
    }

    fun execute() {
        if (!testing) {
            return
        }

        // Advance frame count
        frameCount++

        if (frameCount % 25L == 0L) {
            lifePattern.handleStep(true)
        }

        if (frameCount % framesPerPattern == 1L) {
            // First frame of a new pattern
            lifePattern.setNumberedPattern(number = currentPatternIndex)
            patternStartTime = System.currentTimeMillis()
            patternMemoryBefore = runtime.totalMemory() - runtime.freeMemory()
        } else if (frameCount % framesPerPattern == 0L) {
            // Last frame of a pattern
            val patternMemoryAfter = runtime.totalMemory() - runtime.freeMemory()
            val patternMemoryUsed = patternMemoryAfter - patternMemoryBefore
            val patternDuration = System.currentTimeMillis() - patternStartTime

            val patternResults = JSONObject()
            patternResults.setLong("memory", patternMemoryUsed)
            patternResults.setLong("duration", patternDuration)
            println(
                """
                |pattern:${currentPatternIndex}
                |duration:${patternDuration.formatWithCommas()}
                |memory:${patternMemoryUsed.formatWithCommas()}
                |lastId:${lifePattern.lastId.formatWithCommas()}
                """.trimMargin().replace("\n", " ")
            )

            performanceResults.setJSONObject(currentPatternIndex.toString(), patternResults)
            //System.gc()

            // Advance to next pattern (if not the end of the test)
            if (currentPatternIndex < patternCount) {
                currentPatternIndex++
            }
        }

        if (frameCount == framesPerPattern * patternCount) {
            // Test is over
            val totalDuration = System.currentTimeMillis() - testStartTime
            println("testing complete - duration: ${totalDuration.formatWithCommas()}")
            val totalMemoryUsed = runtime.totalMemory() - runtime.freeMemory()

            performanceResults.setLong(
                "totalMemory",
                totalMemoryUsed
            )
            performanceResults.setLong("totalDuration", totalDuration)
            properties.performanceTestResults = performanceResults
            testing = false
            RunningModeController.endTest()

        }
    }
}