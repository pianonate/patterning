package patterning

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
        val startId = deadNode.id
        var globalVersion = 0

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

class LeafNode(
    override var id: Int,
    override val population: FlexibleInteger,
) : Node {
    override val level: Int = 0
    override val bounds: Bounds = if (population == FlexibleInteger.ONE)
        Bounds(
            top = FlexibleInteger.NEGATIVE_ONE,
            left = FlexibleInteger.NEGATIVE_ONE,
            bottom = FlexibleInteger.NEGATIVE_ONE,
            right = FlexibleInteger.NEGATIVE_ONE
        )
    else
        Bounds(
            top = FlexibleInteger.ZERO,
            left = FlexibleInteger.ZERO,
            bottom = FlexibleInteger.ZERO,
            right = FlexibleInteger.ZERO
        )

    override fun toString(): String {
        return if (population == FlexibleInteger.ONE) "Living Leaf" else "Dead Leaf"
    }
}

class TreeNode(
    val nw: Node,
    val ne: Node,
    val sw: Node,
    val se: Node,
    override var id: Int
) : Node {

    override val level: Int = nw.level + 1
    override val population: FlexibleInteger = nw.population + ne.population + sw.population + se.population

    private val hash = Node.calcHash(nw.id, ne.id, sw.id, se.id)

    var cacheVersion: Int = -1 // Version when cache was last set to valid

    var cache: TreeNode? = null
        get() = if (isValidCache()) field else null
        set(value) = run {
            field = value
            cacheVersion = Node.globalVersion
        }

    var quickCache: TreeNode? = null

    private fun isValidCache(): Boolean = cacheVersion == Node.globalVersion


    override val bounds: Bounds = run {
        if (this.population.isZero()) {
            Bounds(
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO
            )
        } else {

            // Initialize bounds to crazy size - currently set to the universe limit
            var bounds = Bounds(
                FlexibleInteger.MAX_VALUE,
                FlexibleInteger.MAX_VALUE,
                FlexibleInteger.MIN_VALUE,
                FlexibleInteger.MIN_VALUE,
            )

            val offset = if (level == 1) FlexibleInteger.ONE else FlexibleInteger.pow2(level - 2)
            val negatedOffset = if (level == 1) FlexibleInteger.ZERO else -offset

            bounds = calculateChildBounds(nw, negatedOffset, negatedOffset, bounds)
            bounds = calculateChildBounds(ne, negatedOffset, offset, bounds)
            bounds = calculateChildBounds(sw, offset, negatedOffset, bounds)
            bounds = calculateChildBounds(se, offset, offset, bounds)
            bounds.updateLargestDimension(bounds.width.max(bounds.height))
            bounds
        }
    }

    private fun calculateChildBounds(
        child: Node,
        topBottomOffset: FlexibleInteger,
        leftRightOffset: FlexibleInteger,
        bounds: Bounds
    ): Bounds {
        return if (child.population.isNotZero()) {
            val childBounds = child.bounds

            val translatedBounds = Bounds(
                childBounds.top + topBottomOffset,
                childBounds.left + leftRightOffset,
                childBounds.bottom + topBottomOffset,
                childBounds.right + leftRightOffset
            )
            Bounds(
                bounds.top.min(translatedBounds.top),
                bounds.left.min(translatedBounds.left),
                bounds.bottom.max(translatedBounds.bottom),
                bounds.right.max(translatedBounds.right)
            )
        } else {
            bounds
        }
    }

    override fun hashCode(): Int = hash

    override fun toString(): String {
        return "id=$id, level=$level, population=$population, bounds=$bounds"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TreeNode

        if (id != other.id) return false
        if (nw != other.nw) return false
        if (ne != other.ne) return false
        if (sw != other.sw) return false
        if (se != other.se) return false
        if (level != other.level) return false
        if (population != other.population) return false
        if (hash != other.hash) return false
        if (cache != other.cache) return false
        return (quickCache == other.quickCache)
    }
}