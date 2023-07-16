package patterning

import java.math.MathContext

data class Bounds(
    private var _top: FlexibleInteger,
    private var _left: FlexibleInteger,
    private var _bottom: FlexibleInteger,
    private var _right: FlexibleInteger
) {
    constructor(top: FlexibleInteger, left: FlexibleInteger) : this(top, left, top, left)

    var top: FlexibleInteger
        get() = _top
        set(value) {
            _top = value
        }

    var left: FlexibleInteger
        get() = _left
        set(value) {
            _left = value
        }

    var bottom: FlexibleInteger
        get() = _bottom
        set(value) {
            _bottom = value
        }

    var right: FlexibleInteger
        get() = _right
        set(value) {
            _right = value
        }

    val width: FlexibleInteger
        get() = (_right - _left).addOne()

    val height: FlexibleInteger
        get() = (_bottom - _top).addOne()

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


    companion object {

        // empirically setting the mathContext to be exactly the minPrecisionForDrawing still results
        // in some rounding errors - we we make it a bit bigger
        private const val PRECISION_BUFFER = 10
        private var largestDimension: FlexibleInteger = FlexibleInteger(0)
        private var previousPrecision: Int = 0

        var mathContext: MathContext = MathContext(0)
            private set
    }
}