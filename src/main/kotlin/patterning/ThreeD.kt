package patterning

import kotlin.math.cos
import kotlin.math.sin
import patterning.util.FlexibleDecimal

class ThreeD(val canvas: Canvas) {

    data class RotationAngles(
        var yaw: Float = 0f,
        var pitch: Float = 0f,
        var roll: Float = 0f
    ) {
        private val mappedRotationMatrixCache = HashMap<String, Array<Array<FlexibleDecimal>>>()
        private val rotationIncrement = (Math.PI * 2 / 360).toFloat()

        private fun updateAngle(angle: Float): Float {
            var newAngle = angle + rotationIncrement
            newAngle %= (Math.PI * 2).toFloat()
            return newAngle
        }

        fun updateYaw() {
            yaw = updateAngle(yaw)
        }

        fun updatePitch() {
            pitch = updateAngle(pitch)
        }

        fun updateRoll() {
            roll = updateAngle(roll)
        }

        fun reset() {
            yaw = 0f
            pitch = 0f
            roll = 0f
        }

        fun getMappedRotationMatrix(): Array<Array<FlexibleDecimal>> {
            val key = generateCacheKey(yaw, pitch, roll)
            return mappedRotationMatrixCache.getOrPut(key) {
                getRotationMatrix()
                    .map { row -> row.map { FlexibleDecimal.create(it) }.toTypedArray() }
                    .toTypedArray()
            }
        }

        private fun getRotationMatrix(): Array<FloatArray> {
            return multiplyMatrices(
                multiplyMatrices(yawMatrix(), pitchMatrix()),
                rollMatrix()
            )
        }

        private fun yawMatrix(): Array<FloatArray> = arrayOf(
            floatArrayOf(cos(yaw), 0f, sin(yaw)),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(-sin(yaw), 0f, cos(yaw))
        )

        private fun pitchMatrix(): Array<FloatArray> = arrayOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, cos(pitch), -sin(pitch)),
            floatArrayOf(0f, sin(pitch), cos(pitch))
        )

        private fun rollMatrix(): Array<FloatArray> = arrayOf(
            floatArrayOf(cos(roll), -sin(roll), 0f),
            floatArrayOf(sin(roll), cos(roll), 0f),
            floatArrayOf(0f, 0f, 1f)
        )

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

    var isYawing: Boolean = false
    var isPitching: Boolean = false
    var isRolling: Boolean = false

    private var currentAngles = RotationAngles(0f, 0f, 0f)

    fun advance() {
        if (isYawing) {
            currentAngles.updateYaw()
        }
        if (isPitching) {
            currentAngles.updatePitch()
        }
        if (isRolling) {
            currentAngles.updateRoll()
        }
    }

    fun resetAngles() {
        currentAngles.reset()
    }

    fun resetAnglesAndStop() {
        resetAngles()
        isPitching = false
        isYawing = false
        isRolling = false
    }

    // In ThreeD.kt
    fun isRectInView(
        left: FlexibleDecimal,
        top: FlexibleDecimal,
        width: FlexibleDecimal,
        height: FlexibleDecimal
    ): Boolean {

        val rotationMatrix = currentAngles.getMappedRotationMatrix()
        val key = generateCacheKey(rotationMatrix, left, top, width, height)

        return isRectInViewMap.getOrPut(key) {
            val transformedCorner1 = multiplyMatrixWithVector(rotationMatrix, arrayOf(left, top, FlexibleDecimal.ZERO))
            val transformedCorner2 =
                multiplyMatrixWithVector(rotationMatrix, arrayOf(left + width, top, FlexibleDecimal.ZERO))
            val transformedCorner3 =
                multiplyMatrixWithVector(rotationMatrix, arrayOf(left, top + height, FlexibleDecimal.ZERO))
            val transformedCorner4 =
                multiplyMatrixWithVector(rotationMatrix, arrayOf(left + width, top + height, FlexibleDecimal.ZERO))

            val minX = minOf(transformedCorner1[0], transformedCorner2[0], transformedCorner3[0], transformedCorner4[0])
            val maxX = maxOf(transformedCorner1[0], transformedCorner2[0], transformedCorner3[0], transformedCorner4[0])
            val minY = minOf(transformedCorner1[1], transformedCorner2[1], transformedCorner3[1], transformedCorner4[1])
            val maxY = maxOf(transformedCorner1[1], transformedCorner2[1], transformedCorner3[1], transformedCorner4[1])

            (maxX >= FlexibleDecimal.ZERO && minX < canvas.width) &&
                    (maxY >= FlexibleDecimal.ZERO && minY < canvas.height)
        }

    }

    private val isRectInViewMap = HashMap<String, Boolean>()

    private fun multiplyMatrixWithVector(
        matrix: Array<Array<FlexibleDecimal>>,
        vector: Array<FlexibleDecimal>
    ): Array<FlexibleDecimal> {
        val result = Array(3) { FlexibleDecimal.ZERO }
        for (i in 0..2) {
            for (j in 0..2) {
                result[i] += matrix[i][j].multiply(vector[j], canvas.mc)
            }
        }
        return result
    }

    companion object {
        private fun generateCacheKey(vararg components: Any): String {
            var result = 17
            for (component in components) {
                result = 31 * result + component.hashCode()
            }
            return result.toString()
        }
    }
}
