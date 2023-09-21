package patterning.life

import kotlin.math.ceil
import kotlin.math.ln
import patterning.util.addOne


data class Bounds(
    val top: Long,
    val left: Long,
    val bottom: Long,
    val right: Long
) {

    val width: Long
        get() = (right - left).addOne()

    val height: Long
        get() = (bottom - top).addOne()

    // currently only called on setup of a new life pattern
    fun getLevelFromBounds(): Int {
        val coordinates = listOf(top, left, bottom, right)

        var max = coordinates.maxOf { coordinate ->
            maxOf(coordinate.addOne(), -coordinate)
        }

        max = maxOf(max, 4L) // Ensure the minimum max value is 4

        return ceil(ln(max.toDouble()) / ln(2.0)).toInt() + 1
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other == null || other !is Bounds) {
            false
        } else {
            top == other.top &&
                    left == other.left &&
                    bottom == other.bottom &&
                    right == other.right
        }
    }

    override fun hashCode(): Int {
        var result = top.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }
}