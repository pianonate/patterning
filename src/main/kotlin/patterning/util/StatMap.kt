package patterning.util

import java.math.BigDecimal
import java.math.MathContext

/* used to find out how useful the MutableMap is  at saving you lookups for particular use cases */
class StatMap<K, V> {
    val map: MutableMap<K, V>
    private val stats = CacheStats()

    constructor(initialCapacity: Int) {
        this.map = HashMap(initialCapacity)
    }

    constructor(map: MutableMap<K, V>) {
        this.map = map
    }

    data class CacheStats(
        var hits: FlexibleInteger = FlexibleInteger.ZERO,
        var misses: FlexibleInteger = FlexibleInteger.ZERO
    )

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

    fun incrementMisses() {
        stats.misses++
    }

    fun decrementHit() {
        if (stats.hits > FlexibleInteger.ZERO) {
            stats.hits--
        }
    }

    val values: Collection<V>
        get() = map.values

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
        stats.hits = FlexibleInteger.ZERO
        stats.misses = FlexibleInteger.ZERO
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