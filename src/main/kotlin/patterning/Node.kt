package patterning

interface Node {
    val id: Int
    val population: FlexibleInteger
    val level: Int
    val bounds: Bounds

    companion object {
        private const val DEAD_ID = 3
        const val LIVING_ID = 2
        val deadNode = LeafNode(DEAD_ID, FlexibleInteger.ZERO)
        val livingNode = LeafNode(LIVING_ID, FlexibleInteger.ONE)
        val startId = deadNode.id + 1

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
            FlexibleInteger.NEGATIVE_ONE,
            FlexibleInteger.NEGATIVE_ONE,
            FlexibleInteger.NEGATIVE_ONE,
            FlexibleInteger.NEGATIVE_ONE
        )
    else
        Bounds(
            FlexibleInteger.ONE,
            FlexibleInteger.ONE,
            FlexibleInteger.ZERO,
            FlexibleInteger.ZERO
        )

    override fun toString(): String {
        return if (id == Node.LIVING_ID) "Living Leaf" else "Dead Leaf"
    }
}

class InternalNode(
    val nw: Node,
    val ne: Node,
    val sw: Node,
    val se: Node,
    override var id: Int
) : Node {

    override val level: Int = nw.level + 1
    override val population: FlexibleInteger = nw.population + ne.population + sw.population + se.population

    private val hash = Node.calcHash(nw.id, ne.id, sw.id, se.id)

    var cache: InternalNode? = null
    var quickCache: InternalNode? = null

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

            // level 1 requires special handling - the coordinate system doesn't need translation
            // so we just use zero for the offset
            val quarterSizeOffset = if (level == 1)
                FlexibleInteger.ZERO
            else
                FlexibleInteger.pow2(level - 2)

            val quarterSizeNegated = quarterSizeOffset.negate()
            bounds = calculateChildBounds(nw, quarterSizeNegated, quarterSizeNegated, bounds)
            bounds = calculateChildBounds(ne, quarterSizeNegated, quarterSizeOffset, bounds)
            bounds = calculateChildBounds(sw, quarterSizeOffset, quarterSizeNegated, bounds)
            bounds = calculateChildBounds(se, quarterSizeOffset, quarterSizeOffset, bounds)
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

        other as InternalNode

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