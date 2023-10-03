package patterning

import kotlin.math.abs
import patterning.pattern.Visual
import patterning.pattern.VisualsManager
import processing.core.PMatrix3D
import processing.core.PVector

class ThreeD(private val canvas: Canvas, private val visuals: VisualsManager) {

    enum class Translate {
        ToScreen, ToModel
    }

    private val modelToScreenRotation = PMatrix3D()
    private val screenToModelRotation = PMatrix3D()
    private val modelToScreenCoords = PMatrix3D()
    private val screenToModelCoords = PMatrix3D()
    private var translationCenter: PVector? = null

    fun setTranslationCenter(newCenter: PVector) {
        this.translationCenter = newCenter
    }

    private val rotationDisplayModes = setOf(Visual.ThreeDYaw, Visual.ThreeDPitch, Visual.ThreeDRoll)
    private val activeRotations: MutableList<Visual> = mutableListOf()

    private val currentAngles = RotationAngle(0f, 0f, 0f)

    var rectCorners: List<PVector> = listOf()
        private set

    init {
        updateMatrices()
    }

    fun updateActiveRotations(visual: Visual) {
        if (visuals requires visual) {
            activeRotations.add(visual)
        } else {
            activeRotations.remove(visual)
        }
    }

    /**
     * called from draw to move active rotations forward
     */
    fun rotate() {
        if (activeRotations.isEmpty()) return

        // Apply the regular rotations
        applyRotations(modelToScreenRotation, activeRotations)

        updateMatrices()
    }

    private fun applyRotations(matrix: PMatrix3D, rotations: List<Visual>, isInverse: Boolean = false) {
        rotations.forEach { visual ->
            val singleRotationMatrix = PMatrix3D()

            // for rotationMatrix update the angle
            // for the follow reverse don't update the angle
            when (visual) {
                Visual.ThreeDYaw -> currentAngles.updateYaw(singleRotationMatrix)
                Visual.ThreeDPitch -> currentAngles.updatePitch(singleRotationMatrix)
                Visual.ThreeDRoll -> currentAngles.updateRoll(singleRotationMatrix)
                else -> {} // do nothing
            }


            if (isInverse) {

                singleRotationMatrix.invert()

            }
            matrix.apply(singleRotationMatrix)
        }
    }

    fun translatePVector(vector: PVector, direction: Translate): PVector {
        val translated = PVector()
        when (direction) {
           Translate.ToScreen -> modelToScreenCoords.mult(vector, translated)
           Translate.ToModel -> screenToModelCoords.mult(vector, translated)
        }
        return translated
    }

    private fun updateMatrices() {
        // Create translation matrices
        val center = translationCenter ?: PVector(canvas.width / 2f, canvas.height / 2f)

        val translationToCenter = PMatrix3D()
        translationToCenter.translate(center.x, center.y)

        val translationBack = PMatrix3D()
        translationBack.translate(-center.x, -center.y)

        // Combine all transformations
        modelToScreenCoords.set(translationToCenter)
        modelToScreenCoords.apply(modelToScreenRotation)
        modelToScreenCoords.apply(translationBack)

        // Update reverse combined matrix
        screenToModelCoords.set(modelToScreenCoords)
        screenToModelCoords.invert()

        screenToModelRotation.set(modelToScreenRotation)
        screenToModelRotation.invert()

        validateMatrices("translation", modelToScreenCoords, screenToModelCoords)
        validateMatrices("translation", modelToScreenRotation, screenToModelRotation)
    }

    private fun validateMatrices(name: String, matrix: PMatrix3D, reverseMatrix: PMatrix3D) {
        val identityCandidate = PMatrix3D(matrix)
        identityCandidate.apply(reverseMatrix)

        val epsilon = .001f // Tolerance for floating-point comparisons

        val fields = listOf(
            identityCandidate.m00, identityCandidate.m01, identityCandidate.m02, identityCandidate.m03,
            identityCandidate.m10, identityCandidate.m11, identityCandidate.m12, identityCandidate.m13,
            identityCandidate.m20, identityCandidate.m21, identityCandidate.m22, identityCandidate.m23,
            identityCandidate.m30, identityCandidate.m31, identityCandidate.m32, identityCandidate.m33
        )

        // Check diagonal elements for being close to 1
        for (i in 0..3) {
            val value = fields[i * 4 + i]
            if (abs(value - 1f) > epsilon) {
                throw IllegalArgumentException("$name are not inverses. Failed at diagonal ($i, $i): $value")
            }
        }

        // Check off-diagonal elements for being close to 0
        for (row in 0..3) {
            for (col in 0..3) {
                if (row != col) {
                    val value = fields[row * 4 + col]
                    if (abs(value) > epsilon) {
                        throw IllegalArgumentException("$name are not inverses. Failed at off-diagonal ($row, $col): $value")
                    }
                }
            }
        }
    }



    fun getTransformedCorners(left: Float, top: Float, width: Float, height: Float): List<PVector> {
        val corners = getCornersArray(left, top, width, height)
        return transformCorners(corners, modelToScreenCoords)
    }

    fun getBackCornersAtDepth(depth: Float): List<PVector> {
        val depthVector = PVector(0f, 0f, -depth)
        modelToScreenRotation.mult(depthVector, depthVector)
        return rectCorners.map { PVector(it.x + depthVector.x, it.y + depthVector.y, it.z + depthVector.z) }
    }

    fun getTransformedLineCoords(startX: Float, startY: Float, endX: Float, endY: Float): Pair<PVector, PVector> {
        val start = PVector(startX, startY, 0f)
        val end = PVector(endX, endY, 0f)

        val transformedStart = PVector()
        val transformedEnd = PVector()

        modelToScreenCoords.mult(start, transformedStart)
        modelToScreenCoords.mult(end, transformedEnd)

        return Pair(transformedStart, transformedEnd)
    }

    fun reset() {
        currentAngles.reset()
        modelToScreenRotation.reset()
        activeRotations.clear()
        rotationDisplayModes.forEach { displayMode ->
            visuals.disable(displayMode)
        }

        updateMatrices()

    }


    fun isRectInView(left: Float, top: Float, width: Float, height: Float): Boolean {

        val corners = getCornersArray(left, top, width, height)

        // Step 1: Transform 2D points to 3D points based on the current rotation matrix.
        val transformedCorners = transformCorners(corners, modelToScreenCoords)

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
}
