package patterning.util

import java.math.BigDecimal
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap

/* used to find out how useful the MutableMap is  at saving you lookups for particular use cases */
class StatMap<K, V> : Iterable<Map.Entry<K, V>> {
    constructor(initialCapacity: Int) {
        this.map = ConcurrentHashMap(initialCapacity)
    }

    constructor(map: MutableMap<K, V>) {
        this.map = map
    }

    private val map: MutableMap<K, V>
    private val stats = CacheStats()

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return map.iterator()
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
            val total = (stats.hits + stats.misses).toBigDecimal()
            return if (total == BigDecimal.ZERO) {
                0.0
            } else {
                stats.hits.toBigDecimal().divide(total, MathContext.DECIMAL128).toDouble()
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
        } ?: run {
            stats.misses++
            null
        }
    }

    operator fun set(key: K, value: V) {
        map[key] = value
        stats.puts++
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

// Extension function to count decimal places in a Double
fun Double.countDecimalPlaces(): Int {
    val str = this.toString()
    val index = str.indexOf(".")
    return if (index < 0) {
        0
    } else {
        str.length - index - 1
    }
}