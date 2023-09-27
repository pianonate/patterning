package patterning

import patterning.pattern.Behavior
import patterning.pattern.DisplayState
import processing.core.PMatrix3D
import processing.core.PVector

class ThreeD(private val canvas: Canvas, private val displayState: DisplayState) {
    private val rotationMatrix = PMatrix3D()
    private val combinedMatrix = PMatrix3D()

    private val rotationDisplayModes = setOf(Behavior.ThreeDYaw, Behavior.ThreeDPitch, Behavior.ThreeDRoll)

    private var currentAngles = RotationAngles(0f, 0f, 0f)

    var rectCorners: List<PVector> = listOf()
        private set

    /**
     * called from draw to move active rotations forward
     */
    fun rotateActiveRotations() {
        if (rotationDisplayModes.none { displayState expects it }) return

        val matrix = PMatrix3D(rotationMatrix)

        rotationDisplayModes.forEach { displayMode ->
            when (displayMode) {
                Behavior.ThreeDYaw -> if (displayState expects displayMode) currentAngles.updateYaw(matrix)
                Behavior.ThreeDPitch -> if (displayState expects displayMode) currentAngles.updatePitch(matrix)
                Behavior.ThreeDRoll -> if (displayState expects displayMode) currentAngles.updateRoll(matrix)
                else -> {} // do nothing - added because there are actually many display modes even if not invoked here
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
        rotationMatrix.reset()
        updateCombinedMatrix()

        rotationDisplayModes.forEach { displayMode ->
            displayState.disable(displayMode)
        }
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


    /**
     * just re-use a pre-allocated PVector array - when using the profiler
     * and then invoking the perftest, PVector accounted for 12GB! of memory allocations
     * with this re-use in place, the memory dropped to negligible
     * amazing - and the intelliJ profiler is pretty badass
     */
    private val reusableCorners = Array(4) { PVector() }

    private fun transformCorners(corners: Array<PVector>, matrix: PMatrix3D): List<PVector> {
        return corners.mapIndexed { index, it ->
            matrix.mult(it, reusableCorners[index])
            reusableCorners[index]
        }
    }

    private fun getCornersArray(left: Float, top: Float, width: Float, height: Float): Array<PVector> {
        // Directly modify the elements in the reusable array.
        reusableCorners[0].set(left, top, 0f)
        reusableCorners[1].set(left + width, top, 0f)
        reusableCorners[2].set(left + width, top + height, 0f)
        reusableCorners[3].set(left, top + height, 0f)
        return reusableCorners
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
        private val rotationIncrement = (Math.PI * 2 / 120).toFloat()

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
