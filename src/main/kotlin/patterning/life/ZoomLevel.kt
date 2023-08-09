package patterning.life

import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import patterning.util.FlexibleInteger

// the cell width times 2 ^ level will give you the size of the whole universe
// you'll need it it to draw the viewport on screen
class ZoomLevel(initialLevel: Float = DEFAULT_ZOOM_LEVEL) {
    
    private val sizeMap = ConcurrentHashMap(mutableMapOf<Int, BigDecimal>())
    
    var level: Float = initialLevel
        set(value) {
            field = when {
                value == 0.0f -> Float.MIN_VALUE // at very large levels, fit to screen will calculate a cell size of 0 - we need it to have a minimum value in this case
                else -> value
            }
            bigLevelCached = level.toBigDecimal() // Update the cached value
            sizeMap.clear()
        }
    
    var bigLevelCached: BigDecimal = BigDecimal.ZERO // Cached value initialized with initial size
    
    
    init {
        level = initialLevel
    }
    
    var bigLevel: BigDecimal = BigDecimal.ZERO
        get() = bigLevelCached
        private set // Make the setter private to disallow external modification
    
    
    fun universeSize(universeLevel: Int): BigDecimal {
        return universeSizeImpl(universeLevel)
    }
    
    fun halfUniverseSize(universeLevel: Int): BigDecimal {
        return universeSizeImpl(universeLevel - 1)
    }
    
    /**
     * surprisingly caching the result of the half size calculation provides
     * a remarkable speed boost - added sizeMap to track the results of computeIfAbsent()
     * it's pretty profound how many calls to BigDecimal.multiply we can avoid in
     * universeSizeImpl - the cache hit rate gets to 99.99999% pretty quickly
     * private val sizeMap: MutableMap<Int, BigDecimal> = HashMap()
     */
    private fun universeSizeImpl(universeLevel: Int): BigDecimal {
        if (universeLevel < 0) return BigDecimal.ZERO
        
        // these values are calculated so often that caching really seems to help
        // cell size as a big decimal times the requested size of universe at a given level
        // using MathContext to make sure we don't lose precision
        return sizeMap.computeIfAbsent(universeLevel) {
            bigLevelCached.multiply(
                FlexibleInteger.pow2(universeLevel).bigDecimal,
                LifePattern.mc
            )
        }
    }
    
    override fun toString() = "zoom:$level"
    
    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 4.0f
    }
}