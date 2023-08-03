package patterning.life

import patterning.util.FlexibleInteger

interface Node {
    val id: Int
    val population: FlexibleInteger
    val level: Int
    val bounds: Bounds

    // added globalVersion to avoid having to clear the cache whenever we increase the
    // step size on the universe - prior to this we iterated over the entire hashmap
    // and invalidated each cache entry - but when the hashmap was very large this took a long time
    // so now instead we increment the cache version number and only return a cache
    // entry if the cache was set under the current version number

    companion object {
        private const val DEAD_ID = 3
        private const val LIVING_ID = 2
        val deadNode = LeafNode(DEAD_ID, FlexibleInteger.ZERO)
        val livingNode = LeafNode(LIVING_ID, FlexibleInteger.ONE)
        val startId = deadNode.id + 1
        var globalCacheVersion = 0

        fun calcHash(nwId: Int, neId: Int, swId: Int, seId: Int): Int {
            var result = 17
            result = 31 * result xor nwId
            result = 31 * result xor neId
            result = 31 * result xor swId
            result = 31 * result xor seId
            return result
        }
    }
}