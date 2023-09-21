package patterning

import processing.core.PMatrix3D
import processing.core.PVector

class ThreeD(val canvas: Canvas) {

    data class RotationAngles(
        var yaw: Float = 0f,
        var pitch: Float = 0f,
        var roll: Float = 0f
    ) {
        private val rotationIncrement = (Math.PI * 2 / 360).toFloat()

        private val mat = PMatrix3D()

        fun updateYaw() {
            yaw = updateAngle(yaw)
            mat.reset()
            mat.rotateX(pitch)
            mat.rotateY(yaw)
            mat.rotateZ(roll)
        }

        fun updatePitch() {
            pitch = updateAngle(pitch)
            mat.reset()
            mat.rotateX(pitch)
            mat.rotateY(yaw)
            mat.rotateZ(roll)
        }

        fun updateRoll() {
            roll = updateAngle(roll)
            mat.reset()
            mat.rotateX(pitch)
            mat.rotateY(yaw)
            mat.rotateZ(roll)
        }

        private fun updateAngle(angle: Float): Float {
            var newAngle = angle + rotationIncrement
            newAngle %= (Math.PI * 2).toFloat()
            return newAngle
        }

        fun reset() {
            yaw = 0f
            pitch = 0f
            roll = 0f
            mat.reset()
        }

        fun getRotationMatrix(): PMatrix3D {
            return mat
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
    // In ThreeD.kt
    fun isRectInView(left: Float, top: Float, width: Float, height: Float): Boolean {
        val rotationMatrix = currentAngles.getRotationMatrix()

        val corners = arrayOf(
            PVector(left, top, 0f),
            PVector(left + width, top, 0f),
            PVector(left, top + height, 0f),
            PVector(left + width, top + height, 0f)
        )

        val transformedCorners = corners.map {
            val result = PVector()
            rotationMatrix.mult(it, result)
            result
        }

        val minX = transformedCorners.minOf { it.x }
        val maxX = transformedCorners.maxOf { it.x }
        val minY = transformedCorners.minOf { it.y }
        val maxY = transformedCorners.maxOf { it.y }

        return (maxX >= 0 && minX < canvas.width.toFloat()) && (maxY >= 0 && minY < canvas.height.toFloat())
    }
}
