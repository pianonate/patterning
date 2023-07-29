package patterning.life

import patterning.util.FlexibleInteger

data class Bounds(
    val top: FlexibleInteger,
    val left: FlexibleInteger,
    val bottom: FlexibleInteger,
    val right: FlexibleInteger
) {

    val width: FlexibleInteger
        get() = (right - left).addOne()

    val height: FlexibleInteger
        get() = (bottom - top).addOne()

    // currently only called on setup of a new life pattern
    fun getLevelFromBounds(): Int {
        val coordinates = listOf(top, left, bottom, right)

        var max = coordinates.maxOf { coordinate ->
            maxOf(coordinate.addOne(), -coordinate)
        }

        max = maxOf(max, FlexibleInteger.FOUR) // Ensure the minimum max value is 4

        return max.getLevel()
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