package patterning.life

import java.math.BigDecimal
import patterning.util.FlexibleInteger
import patterning.util.StatMap

// the cell width times 2 ^ level will give you the size of the whole universe
// you'll need it it to draw the viewport on screen
 class Cell(initialSize: Float = DEFAULT_CELL_WIDTH) {
    var currentZoom: Float = 1.0f
    var targetZoom: Float = 1.0f

     private var bigSizeCached: BigDecimal = BigDecimal.ZERO // Cached value initialized with initial size
    val cellBorderWidth
        get() = size * WIDTH_RATIO


    // surprisingly caching the result of the half size calculation provides
    // a remarkable speed boost - added CachedMap to track the results of getOrPut()
    // it's pretty profound how many calls to BigDecimal.multiply we can avoid in
    // universeSizeImpl - the cache hit rate gets to 99.99999% pretty quickly
    // private val sizeMap: MutableMap<Int, BigDecimal> = HashMap()
    private val sizeMap = StatMap(mutableMapOf<Int, BigDecimal>())

    var size: Float = initialSize
        set(value) {
            field = when {
                value > CELL_WIDTH_ROUNDING_THRESHOLD && !zoomingIn -> value.toInt().toFloat()
                value > CELL_WIDTH_ROUNDING_THRESHOLD -> value.toInt().plus(1).toFloat()
                value == 0.0f -> Float.MIN_VALUE // at very large levels, fit to screen will calculate a cell size of 0 - we need it to have a minimum value in this case
                else -> value
            }
            bigSizeCached = size.toBigDecimal() // Update the cached value
            sizeMap.clear()
        }

    init {
        size = initialSize
    }

    var bigSize: BigDecimal = BigDecimal.ZERO
        get() = bigSizeCached
        private set // Make the setter private to disallow external modification

    fun universeSize(level: Int): BigDecimal {
        return universeSizeImpl(level)
    }

    fun halfUniverseSize(level: Int): BigDecimal {
        return universeSizeImpl(level - 1)
    }

    // todo also cache this value the way you cache getHalfSize()
    private fun universeSizeImpl(level: Int): BigDecimal {
        if (level < 0) return BigDecimal.ZERO

        // these values are calculated so often that caching really seems to help
        // cell size as a big decimal times the requested size of universe at a given level
        // using MathContext to make sure we don't lose precision
        return sizeMap.getOrPut(level) { bigSizeCached.multiply(FlexibleInteger.pow2(level).bigDecimal, LifePattern.mc) }

    }

    private var zoomingIn: Boolean = false

    fun zoom(zoomIn: Boolean) {
        zoomingIn = zoomIn
        val factor = if (zoomIn) 1.25f else 0.8f
        size *= factor
    }

    override fun toString() = "Cell{size=$size}"

    companion object {
        private const val DEFAULT_CELL_WIDTH = 4.0f
        private const val CELL_WIDTH_ROUNDING_THRESHOLD = 1.6f
        const val WIDTH_RATIO = .05f
    }
}