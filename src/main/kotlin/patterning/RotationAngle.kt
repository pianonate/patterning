package patterning

import processing.core.PMatrix3D

data class RotationAngle(
    var yaw: Float = 0f,
    var pitch: Float = 0f,
    var roll: Float = 0f
) {
    private val rotationIncrement = (Math.PI / 12).toFloat()
    private val twoPi = (Math.PI * 2).toFloat()


    fun updateYaw(mat: PMatrix3D) {
        yaw += rotationIncrement % twoPi
        mat.rotateY(rotationIncrement)
    }

    fun updatePitch(mat: PMatrix3D) {
        pitch += rotationIncrement % twoPi
        mat.rotateX(rotationIncrement)
    }

    fun updateRoll(mat: PMatrix3D) {
        roll += rotationIncrement % twoPi
        mat.rotateZ(rotationIncrement)
    }

    fun reset() {
        yaw = 0f
        pitch = 0f
        roll = 0f
    }
}