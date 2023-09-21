package patterning

import kotlin.math.cos
import kotlin.math.sin

class ThreeD {
    var isYawing: Boolean = false
    var isPitching: Boolean = false
    var isRolling: Boolean = false

    private var currentYawAngle: Float = 0f
    private var currentPitchAngle: Float = 0f
    private var currentRollAngle: Float = 0f

    private val rotationIncrement = (Math.PI * 2 / 360).toFloat() // or TWO_PI / 360f

    fun advance() {
        if (isYawing) {
            currentYawAngle += rotationIncrement
            currentYawAngle %= (Math.PI * 2).toFloat() // or TWO_PI
        }
        if (isPitching) {
            currentPitchAngle += rotationIncrement
            currentPitchAngle %= (Math.PI * 2).toFloat()
        }
        if (isRolling) {
            currentRollAngle += rotationIncrement
            currentRollAngle %= (Math.PI * 2).toFloat()
        }
    }

    fun resetAngles() {
        currentYawAngle = 0f
        currentPitchAngle = 0f
        currentRollAngle = 0f
    }

    fun resetAnglesAndStop() {
        resetAngles()
        isPitching = false
        isYawing = false
        isRolling = false
    }

    private fun combinedRotationMatrix(): Array<FloatArray> {
        return multiplyMatrices(
            multiplyMatrices(yawMatrix(currentYawAngle), pitchMatrix(currentPitchAngle)),
            rollMatrix(currentRollAngle)
        )
    }

    private fun yawMatrix(angle: Float): Array<FloatArray> {
        return arrayOf(
            floatArrayOf(cos(angle), 0f, sin(angle)),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(-sin(angle), 0f, cos(angle))
        )
    }

    private fun pitchMatrix(angle: Float): Array<FloatArray> {
        return arrayOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, cos(angle), -sin(angle)),
            floatArrayOf(0f, sin(angle), cos(angle))
        )
    }

    private fun rollMatrix(angle: Float): Array<FloatArray> {
        return arrayOf(
            floatArrayOf(cos(angle), -sin(angle), 0f),
            floatArrayOf(sin(angle), cos(angle), 0f),
            floatArrayOf(0f, 0f, 1f)
        )
    }

    private fun multiplyMatrices(a: Array<FloatArray>, b: Array<FloatArray>): Array<FloatArray> {
        val result = Array(3) { FloatArray(3) }

        for (i in 0..2) {
            for (j in 0..2) {
                result[i][j] = 0f
                for (k in 0..2) {
                    result[i][j] += a[i][k] * b[k][j]
                }
            }
        }

        return result
    }
}
