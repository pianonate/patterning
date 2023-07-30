package patterning.life

import kotlin.system.measureTimeMillis
import patterning.Properties
import patterning.RunningState
import processing.data.JSONObject

class PerformanceTest(private val lifePattern: LifePattern, private val properties: Properties) {
    private val performanceResults = JSONObject()
    private val patternCount = 9
    private val framesPerPattern = 100L

    fun runTest() {
        val totalDuration = measureTimeMillis {
            for (patternIndex in 1..patternCount) {
                val patternMemoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                val patternDuration = measureTimeMillis {
                    RunningState.pause()
                    lifePattern.setNumberedLifeForm(patternIndex)
                    /* lifePattern.runPattern(framesPerPattern) */
                    RunningState.pause()
                }
                val patternMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                val patternMemoryUsed = patternMemoryAfter - patternMemoryBefore

                val patternResults = JSONObject()
                patternResults.setInt("duration", patternDuration.toInt())
                patternResults.setInt("memory", patternMemoryUsed.toInt())
                performanceResults.setJSONObject(patternIndex.toString(), patternResults)
            }
        }
        performanceResults.setInt("totalDuration", totalDuration.toInt())
        performanceResults.setInt("totalMemory", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).toInt())
        properties.performanceTestResults = performanceResults
    }
}