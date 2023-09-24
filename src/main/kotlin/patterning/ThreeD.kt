package patterning

import processing.core.PMatrix3D
import processing.core.PVector

class ThreeD(val canvas: Canvas) {
    private val rotationMatrix = PMatrix3D()
    private val combinedMatrix = PMatrix3D()

    enum class Rotation { YAW, PITCH, ROLL }

    private val activeRotations = mutableListOf<Rotation>()
    private var currentAngles = RotationAngles(0f, 0f, 0f)

    var rectCorners: List<PVector> = listOf()
        private set

    var isYawing: Boolean
        get() = Rotation.YAW in activeRotations
        set(value) {
            if (value) {
                activeRotations.add(Rotation.YAW)
            } else {
                activeRotations.remove(Rotation.YAW)
            }
        }

    var isPitching: Boolean
        get() = Rotation.PITCH in activeRotations
        set(value) {
            if (value) {
                activeRotations.add(Rotation.PITCH)
            } else {
                activeRotations.remove(Rotation.PITCH)
            }
        }

    var isRolling: Boolean
        get() = Rotation.ROLL in activeRotations
        set(value) {
            if (value) {
                activeRotations.add(Rotation.ROLL)
            } else {
                activeRotations.remove(Rotation.ROLL)
            }
        }

    /**
     * called from draw to move active rotations forward
     */
    fun rotateActiveRotations() {

        if (activeRotations.isEmpty()) return

        val matrix = PMatrix3D(rotationMatrix)
        activeRotations.forEach { rotation ->
            when (rotation) {
                Rotation.YAW -> currentAngles.updateYaw(matrix)
                Rotation.PITCH -> currentAngles.updatePitch(matrix)
                Rotation.ROLL -> currentAngles.updateRoll(matrix)
            }
        }
        rotationMatrix.set(matrix)
        updateCombinedMatrix()
    }

    fun getTransformedRectCorners(left: Float, top: Float, width: Float, height: Float): List<PVector> {
        val corners = getCornersArray(left, top, width, height)
        return transformCorners(corners, combinedMatrix)
    }

    fun getBackCornersAtDepth(depth: Float): List<PVector> {
        val depthVector = PVector(0f, 0f, -depth)
        rotationMatrix.mult(depthVector, depthVector)
        return rectCorners.map { PVector(it.x + depthVector.x, it.y + depthVector.y, it.z + depthVector.z) }
    }

    fun getTransformedLineCoords(startX: Float, startY: Float, endX: Float, endY: Float): Pair<PVector, PVector> {
        val start = PVector(startX, startY, 0f)
        val end = PVector(endX, endY, 0f)

        val transformedStart = PVector()
        val transformedEnd = PVector()

        combinedMatrix.mult(start, transformedStart)
        combinedMatrix.mult(end, transformedEnd)

        return Pair(transformedStart, transformedEnd)
    }

    fun reset() {
        currentAngles.reset()
        activeRotations.clear()
        rotationMatrix.reset()
        updateCombinedMatrix()
    }

    fun isRectInView(left: Float, top: Float, width: Float, height: Float): Boolean {

        val corners = getCornersArray(left, top, width, height)

        // Step 1: Transform 2D points to 3D points based on the current rotation matrix.
        val transformedCorners = transformCorners(corners, combinedMatrix)

        rectCorners = transformedCorners

        // Step 2: Check if these transformed points fit within your canvas dimensions.
        val tLeft = transformedCorners.minOf { it.x }
        val tRight = transformedCorners.maxOf { it.x }
        val tTop = transformedCorners.minOf { it.y }
        val tBottom = transformedCorners.maxOf { it.y }

        return (tRight >= 0 && tLeft <= canvas.width) && (tBottom >= 0 && tTop <= canvas.height)
    }

    private fun transformCorners(corners: Array<PVector>, matrix: PMatrix3D): List<PVector> {
        return corners.map {
            val result = PVector()
            matrix.mult(it, result)
            result
        }
    }

    private fun getCornersArray(left: Float, top: Float, width: Float, height: Float): Array<PVector> {
        return arrayOf(
            PVector(left, top, 0f),           // top-left
            PVector(left + width, top, 0f),   // top-right
            PVector(left + width, top + height, 0f), // bottom-right
            PVector(left, top + height, 0f)  // bottom-left
        )
    }


    private fun updateCombinedMatrix() {
        // Create translation matrices
        val translationMatrixToCenter = PMatrix3D()
        translationMatrixToCenter.translate(canvas.width / 2f, canvas.height / 2f)

        val translationMatrixBack = PMatrix3D()
        translationMatrixBack.translate(-canvas.width / 2f, -canvas.height / 2f)

        // Combine all transformations
        combinedMatrix.set(translationMatrixToCenter)
        combinedMatrix.apply(rotationMatrix)
        combinedMatrix.apply(translationMatrixBack)
    }

    data class RotationAngles(
        var yaw: Float = 0f,
        var pitch: Float = 0f,
        var roll: Float = 0f
    ) {
        private val rotationIncrement = (Math.PI * 2 / 360).toFloat()

        fun updateYaw(mat: PMatrix3D) {
            mat.rotateY(rotationIncrement)
        }

        fun updatePitch(mat: PMatrix3D) {
            mat.rotateX(rotationIncrement)
        }

        fun updateRoll(mat: PMatrix3D) {
            mat.rotateZ(rotationIncrement)
        }

        fun reset() {
            yaw = 0f
            pitch = 0f
            roll = 0f
        }
    }
}
