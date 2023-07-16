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
    var hashmapNext: InternalNode? = null

    override val bounds: Bounds = run {
        if (this.population.isZero()) {
            Bounds(
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO
            )
        } else {
            // Half the size of the current node.
            val offset = FlexibleInteger(LifeUniverse.pow2(level - 1))
            val smallOffset = FlexibleInteger(LifeUniverse.pow2(level - 2))

            // Initialize bounds to a small, out-of-bounds square.
            var bounds = Bounds(
                FlexibleInteger.MAX_VALUE,
                FlexibleInteger.MAX_VALUE,
                FlexibleInteger.MIN_VALUE,
                FlexibleInteger.MIN_VALUE,
            )

            if (level < 2) {
                // Translate the bounds of each child node to the coordinate system of the parent node.
                bounds = calculateChildBounds(nw, FlexibleInteger.ZERO, FlexibleInteger.ZERO, bounds)
                bounds = calculateChildBounds(ne, FlexibleInteger.ZERO, offset, bounds)
                bounds = calculateChildBounds(sw, offset, FlexibleInteger.ZERO, bounds)
                bounds = calculateChildBounds(se, offset, offset, bounds)
            } else {
                // Translate the bounds of each child node to the coordinate system of the parent node.
                bounds = calculateChildBounds(nw, smallOffset.negate(), smallOffset.negate(), bounds)
                bounds = calculateChildBounds(ne, smallOffset.negate(), smallOffset, bounds)
                bounds = calculateChildBounds(sw, smallOffset, smallOffset.negate(), bounds)
                bounds = calculateChildBounds(se, smallOffset, smallOffset, bounds)
            }
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
        return if (child.population > FlexibleInteger.ZERO) {
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

    fun nodeBounds(): Bounds {
        if (this.population.isZero()) {
            return Bounds(
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO
            )
        }

        // BigInteger offset = BigInteger.valueOf(2).pow(root.level - 1);
        val offset = FlexibleInteger(LifeUniverse.pow2(this.level - 1))
        val bounds = Bounds(offset, offset, offset.negate(), offset.negate())
        nodeGetBoundary(
            this,
            offset.negate(),
            offset.negate(),
            MASK_TOP or MASK_LEFT or MASK_BOTTOM or MASK_RIGHT,
            bounds
        )
        return bounds
    }

    private fun nodeGetBoundary(
        node: Node,
        left: FlexibleInteger,
        top: FlexibleInteger,
        findMask: Int,
        boundary: Bounds
    ) {
        if (node.population.isZero() || findMask == 0) {
            return
        }
        if (node.level == 0) {
            boundary.left = boundary.left.min(left)
            boundary.right = boundary.right.max(left)
            boundary.top = boundary.top.min(top)
            boundary.bottom = boundary.bottom.max(top)
        } else {
            val offset = FlexibleInteger(LifeUniverse.pow2(node.level - 1))
            val doubledOffset = FlexibleInteger(LifeUniverse.pow2(node.level))
            if (left >= boundary.left &&
                left + doubledOffset <= boundary.right &&
                top >= boundary.top &&
                top + doubledOffset <= boundary.bottom
            ) {
                // This square is already inside the found boundary
                return
            }
            var findNW = findMask
            var findSW = findMask
            var findNE = findMask
            var findSE = findMask
            if ((node as InternalNode).nw.population.isNotZero()) {
                findSW = findSW and MASK_TOP.inv()
                findNE = findNE and MASK_LEFT.inv()
                findSE = findSE and (MASK_TOP or MASK_LEFT).inv()
            }
            if (node.sw.population.isNotZero()) {
                findSE = findSE and MASK_LEFT.inv()
                findNW = findNW and MASK_BOTTOM.inv()
                findNE = findNE and (MASK_BOTTOM or MASK_LEFT).inv()
            }
            if (node.ne.population.isNotZero()) {
                findNW = findNW and MASK_RIGHT.inv()
                findSE = findSE and MASK_TOP.inv()
                findSW = findSW and (MASK_TOP or MASK_RIGHT).inv()
            }
            if (node.se.population.isNotZero()) {
                findSW = findSW and MASK_RIGHT.inv()
                findNE = findNE and MASK_BOTTOM.inv()
                findNW = findNW and (MASK_BOTTOM or MASK_RIGHT).inv()
            }
            nodeGetBoundary(node.nw, left, top, findNW, boundary)
            nodeGetBoundary(node.sw, left, top + offset, findSW, boundary)
            nodeGetBoundary(node.ne, left + offset, top, findNE, boundary)
            nodeGetBoundary(node.se, left + offset, top + offset, findSE, boundary)
        }
    }

    override fun hashCode(): Int = hash

    override fun toString(): String {
        return "id=$id, level=$level, population=$population" // , bounds=(${bounds.top},${bounds.left},${bounds.bottom},${bounds.right}), cache=$cache, quickCache=$quickCache, hashmapNext=$hashmapNext"
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
        if (quickCache != other.quickCache) return false
        return hashmapNext == other.hashmapNext
    }

    companion object {
        private const val MASK_LEFT = 1
        private const val MASK_TOP = 2
        private const val MASK_RIGHT = 4
        private const val MASK_BOTTOM = 8
    }

}