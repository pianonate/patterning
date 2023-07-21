package patterning

import java.math.MathContext

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

    // currently only called on setup
    // which is fine but note that this will fail
    // if called at a level that has bounds larger than a Double can hold
    fun getLevelFromBounds(): Int {
        val coordinates = listOf(top, left, bottom, right)

        var max = coordinates.maxOf { coordinate ->
            maxOf(coordinate.addOne(), -coordinate)
        }

        max = maxOf(max, FlexibleInteger(4)) // Ensure the minimum max value is 4

        return max.getLevel() // ceil(ln(max.toDouble()) / ln(2.0)).toInt() + 1
    }

    fun updateLargestDimension(dimension: FlexibleInteger) {
        synchronized(Bounds) {
            if (dimension > largestDimension) {
                largestDimension = dimension
                // Assuming minBaseToExceed is a function on FlexibleInteger
                val precision = largestDimension.minPrecisionForDrawing()
                if (precision != previousPrecision) {
                    mathContext = MathContext(precision + PRECISION_BUFFER)
                    previousPrecision = precision
                }
            }
        }
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

    companion object {

        // empirically setting the mathContext to be exactly the minPrecisionForDrawing still results
        // in some rounding errors - we we make it a bit bigger
        //
        // i was wondering why empirically we needed a PRECISION_BUFFER to add to the precision
        // now that i'm thinking about it, this is probably the required precision for a float
        // which is what the cell.cellSize is - especially for really small numbers
        //  without it we'd be off by only looking at the integer part of the largest dimension
        // given all of the BigDecimal arithmetic
        private const val PRECISION_BUFFER = 10
        private var largestDimension: FlexibleInteger = FlexibleInteger(0)
        private var previousPrecision: Int = 0

        fun resetMathContext() {
            largestDimension = FlexibleInteger(0)
            previousPrecision = 0
        }

        // return a mathContext that will allow for the necessary precision to do BigDecimal arithmetic
        // on the largest dimension of the current universe
        var mathContext: MathContext = MathContext(0)
            private set
    }
}