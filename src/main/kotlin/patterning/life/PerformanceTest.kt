package patterning.life

import patterning.Properties
import patterning.RunningState
import patterning.TestModeObserver
import processing.data.JSONObject

class PerformanceTest(private val lifePattern: LifePattern, private val properties: Properties) : TestModeObserver {
    private val performanceResults = JSONObject()
    private val patternCount = 9
    private val framesPerPattern = 200L

    // State for the ongoing test, if any
    private var currentPatternIndex = 1
    private var frameCount = 0L
    private var patternStartTime = 0L
    private var patternMemoryBefore = 0L
    private var testStartTime = 0L
    private var testing = false

    private val runtime = Runtime.getRuntime()

    init {
        RunningState.addTestModeObserver(this)
    }

    override fun onTestModeEnter() {
        // Reset state for new test
        testing = true
        currentPatternIndex = 1
        frameCount = 0
        testStartTime = System.currentTimeMillis()
        patternStartTime = testStartTime
        patternMemoryBefore = runtime.totalMemory() - runtime.freeMemory()
    }

    fun execute() {
        if (!testing) {
            return
        }

        // Advance frame count
        frameCount++

        if (frameCount % framesPerPattern == 1L) {
            // First frame of a new pattern
            lifePattern.setNumberedLifeForm(number = currentPatternIndex, testing = true)
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
            performanceResults.setJSONObject(currentPatternIndex.toString(), patternResults)

            // Advance to next pattern (if not the end of the test)
            if (currentPatternIndex < patternCount) {
                currentPatternIndex++
            }
        }

        if (frameCount == framesPerPattern * patternCount) {
            // Test is over
            val totalDuration = System.currentTimeMillis() - testStartTime
            val totalMemoryUsed = runtime.totalMemory() - runtime.freeMemory()

            performanceResults.setLong(
                "totalMemory",
                totalMemoryUsed
            )
            performanceResults.setLong("totalDuration", totalDuration)
            properties.performanceTestResults = performanceResults
            testing = false
            RunningState.endTest()
        }
    }
}