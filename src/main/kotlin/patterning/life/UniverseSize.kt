package patterning.life

import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import patterning.util.FlexibleInteger

// the cell width times 2 ^ level will give you the size of the whole universe
// you'll need it it to draw the viewport on screen
class UniverseSize() {
    
    private val sizeMap = ConcurrentHashMap<Pair<Float, Int>, BigDecimal>()
    
    fun getSize(universeLevel: Int, zoomLevel: Float): BigDecimal {
        return universeSizeImpl(universeLevel, zoomLevel)
    }
    
    fun getHalf(universeLevel: Int, zoomLevel: Float): BigDecimal {
        return universeSizeImpl(universeLevel - 1, zoomLevel)
    }
    
    /**
     * surprisingly caching the result of the half size calculation provides
     * a remarkable speed boost - added sizeMap to track the results of computeIfAbsent()
     * it's pretty profound how many calls to BigDecimal.multiply we can avoid in
     * universeSizeImpl - the cache hit rate gets to 99.99999% pretty quickly
     * private val sizeMap: MutableMap<Int, BigDecimal> = HashMap()
     */
    private fun universeSizeImpl(universeLevel: Int, zoomLevel: Float): BigDecimal {
        if (universeLevel < 0) return BigDecimal.ZERO
        
        // Create a compound key using Pair
        val key = Pair(zoomLevel, universeLevel)
        
        return sizeMap.computeIfAbsent(key) {
            val bigLevel = zoomLevel.toBigDecimal()
            bigLevel.multiply(
                FlexibleInteger.pow2(universeLevel).bigDecimal)
        }
    }
    
    override fun toString() = "sizeMap:${sizeMap.size}"

}