package patterning.util

import java.util.concurrent.ConcurrentHashMap

/*
Modify the StatMap class to include a usageCount field for each entry, as mentioned earlier.

Implement a method, such as incrementUsage(key: K), that takes a key and increments the usage count for that key.

Create a method, such as performGarbageCollection(), that scans through the map, identifies entries with low usage counts, and removes them.

Schedule the performGarbageCollection() method to run at regular intervals using a ScheduledExecutorService. This way, the garbage collection process will happen automatically in the background.

When accessing a value from the map, make sure to call incrementUsage(key) to update the usage count for that entry.

When adding or updating a value in the map, ensure that you initialize or update the usageCount for that entry appropriately.

For thread safety, you may need to use appropriate synchronization mechanisms, such as locks or atomic operations, when accessing or modifying the usageCount field and the map.*/



/* used to find out how useful the MutableMap is  at saving you lookups for particular use cases */
class StatMap<K, V> : Iterable<Map.Entry<K, V>> {
    constructor(initialCapacity: Int) {
        this.map = ConcurrentHashMap(initialCapacity)
    }

    constructor(map: MutableMap<K, V>) {
        this.map = map
    }

    val size
        get() = map.size

    private val map: MutableMap<K, V>
    private val stats = CacheStats()
    private val usageCountMap: MutableMap<K, Int> = ConcurrentHashMap() // To store the usage count

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return map.iterator()
    }

    fun removeEntriesByKeySet(keySet: Set<K>) {
        map.keys.removeAll(keySet)
        usageCountMap.keys.removeAll(keySet)
    }

    data class CacheStats(
        var hits: FlexibleInteger = FlexibleInteger.ZERO,
        var misses: FlexibleInteger = FlexibleInteger.ZERO,
        var puts: FlexibleInteger = FlexibleInteger.ZERO,
    )

    val puts: FlexibleInteger
        get() = stats.puts

    val hits: FlexibleInteger
        get() = stats.hits

    val misses: FlexibleInteger
        get() = stats.misses

    val hitRate: Double
        get() {
            val total = (stats.hits + stats.misses).toDouble()
            return if (total == 0.0) {
                0.0
            } else {
                stats.hits.toDouble() / total
            }
        }

    fun decrementHit() {
        if (stats.hits > FlexibleInteger.ZERO) {
            stats.hits--
        }
    }

    operator fun get(key: K): V? {
        return map[key]?.also {
            stats.hits++
            incrementUsage(key) // Update the usage count when accessing a value
        } ?: run {
            stats.misses++
            null
        }
    }

    operator fun set(key: K, value: V) {
        map[key] = value
        stats.puts++
        incrementUsage(key) // Update the usage count when adding or updating a value
    }

    private fun incrementUsage(key: K) {
        usageCountMap.compute(key) { _, count -> count?.plus(1) ?: 1 }
    }

    fun getUsageCountHistogram(): Map<Int, Int> {
        return usageCountMap.values.groupingBy { it }.eachCount()
    }

    fun getValueWithHighestUsageCount(): V? {
        val entryWithHighestUsage = usageCountMap.maxByOrNull { it.value }
        return entryWithHighestUsage?.let { (keyWithHighestUsage, _) ->
            this[keyWithHighestUsage]
        }
    }

    fun getUsageCountForKey(key: K): Int {
        return usageCountMap[key] ?: 0
    }

    fun filterValuesByUsageCountOne(): List<V> {
        val filteredValues = mutableListOf<V>()

        for ((key, usageCount) in usageCountMap) {
            if (usageCount == 1) {
                map[key]?.let { value -> filteredValues.add(value) }
            }
        }

        return filteredValues
    }



    fun getOrPut(key: K, defaultValue: () -> V): V {
        val value = map[key]

        return if (value != null) {
            stats.hits++
            value
        } else {
            stats.misses++
            map.getOrPut(key, defaultValue)
        }
    }

    fun clear() {
        map.clear()
        clearStats()
    }

    fun clearStats() {
        stats.hits = FlexibleInteger.ZERO
        stats.misses = FlexibleInteger.ZERO
        stats.puts = FlexibleInteger.ZERO
    }

    override fun toString(): String {
        val hitRatePercent = hitRate * 100
        return "CachedMap(entries=${map.size}, hits=${stats.hits}, misses=${stats.misses}, hitRate=${
            "%.${hitRatePercent.countDecimalPlaces()}f".format(
                hitRatePercent
            )
        }%)"
    }
}