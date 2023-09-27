package patterning

import patterning.pattern.Visual
import patterning.pattern.VisualsManager
import processing.core.PMatrix3D
import processing.core.PVector

class ThreeD(private val canvas: Canvas, private val visuals: VisualsManager) {
    private val rotationMatrix = PMatrix3D()
    private val combinedMatrix = PMatrix3D()

    private val rotationDisplayModes = setOf(Visual.ThreeDYaw, Visual.ThreeDPitch, Visual.ThreeDRoll)

    private var currentAngles = RotationAngles(0f, 0f, 0f)

    var rectCorners: List<PVector> = listOf()
        private set

    /**
     * called from draw to move active rotations forward
     */
    fun rotate() {
        if (rotationDisplayModes.none { visuals requires it }) return

        val matrix = PMatrix3D(rotationMatrix)

        rotationDisplayModes.forEach { visual ->
            when (visual) {
                Visual.ThreeDYaw -> if (visuals requires visual) currentAngles.updateYaw(matrix)
                Visual.ThreeDPitch -> if (visuals requires visual) currentAngles.updatePitch(matrix)
                Visual.ThreeDRoll -> if (visuals requires visual) currentAngles.updateRoll(matrix)
                else -> {} // do nothing - added because there are actually many display modes even if not invoked here
            }
        }

        rotationMatrix.set(matrix)
        updateCombinedMatrix()
    }

    fun getTransformedCorners(left: Float, top: Float, width: Float, height: Float): List<PVector> {
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
            visuals.disable(displayMode)
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

    // doesn't work - fundamentally hard problem
    /*fun getMouseMoveOffsets(mouseBefore:PVector, mouseAfter:PVector, g: PGraphics): PVector {

        val matrix = PMatrix3D(combinedMatrix)

        // Create a copy of the before position
        val transformedMouseBefore = PVector(mouseBefore.x, mouseBefore.y)

        // Create a copy of the after position
        val transformedMouseAfter = PVector(mouseAfter.x, mouseAfter.y)

        matrix.invert()

        // Apply the rotation matrix
        matrix.mult(mouseBefore, transformedMouseBefore)
        matrix.mult(mouseAfter, transformedMouseAfter)

        val x = transformedMouseAfter.x
        val y = transformedMouseAfter.y
        val z = transformedMouseAfter.z
        val screenX = g.screenX(x,y,z)
        val screenY = g.screenY(x,y,z)

       // val offsetVector =  PVector(transformedMouseAfter.x - transformedMouseBefore.x, transformedMouseAfter.y - transformedMouseBefore.y)
        val offsetVector =  PVector(screenX,screenY)

        return offsetVector

    }*/


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
