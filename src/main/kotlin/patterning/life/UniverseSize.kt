package patterning.life

import patterning.Canvas
import patterning.util.FlexibleDecimal
import java.util.concurrent.ConcurrentHashMap

// the cell width times 2 ^ level will give you the size of the whole universe
// you'll need it it to draw the viewport on screen
class UniverseSize(private val canvas: Canvas) {

    private val sizeMap = ConcurrentHashMap<Pair<Int, FlexibleDecimal>, FlexibleDecimal>()

    fun getSize(universeLevel: Int, zoomLevel: FlexibleDecimal): FlexibleDecimal {
        return universeSizeImpl(universeLevel, zoomLevel)
    }

    fun getHalf(universeLevel: Int, zoomLevel: FlexibleDecimal): FlexibleDecimal {
        return universeSizeImpl(universeLevel - 1, zoomLevel)
    }

    /**
     * surprisingly caching the result of the half size calculation provides
     * a remarkable speed boost - added sizeMap to track the results of computeIfAbsent()
     * it's pretty profound how many calls to BigDecimal.multiply we can avoid in
     * universeSizeImpl - the cache hit rate gets to 99.99999% pretty quickly
     * private val sizeMap: MutableMap<Int, BigDecimal> = HashMap()
     */
    private fun universeSizeImpl(universeLevel: Int, zoomLevel: FlexibleDecimal): FlexibleDecimal {
        if (universeLevel < 0) return FlexibleDecimal.ZERO

        // Create a compound key using Pair
        val key = Pair(universeLevel, zoomLevel)

        return sizeMap.computeIfAbsent(key) {
            zoomLevel.multiply(LifeUniverse.pow2(universeLevel).toFlexibleDecimal(), canvas.mc)
        }
    }

    override fun toString() = "sizeMap:${sizeMap.size}"

}