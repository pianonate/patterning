package patterning.life


import patterning.util.isNotZero
import patterning.util.isZero

class TreeNode(
    override val nw: Node,
    override val ne: Node,
    override val sw: Node,
    override val se: Node,
    override var id: Int,
    val aliveSince: Int
) : Node {


    override val level: Int = nw.level + 1
    override val population: Long = nw.population + ne.population + sw.population + se.population

    private val hash = Node.calcHash(nw.id, ne.id, sw.id, se.id)
    private var cacheVersion: Int = -1 // Version when cache was last set to valid

    var nextGenCache: TreeNode? = null
        get() = if (isValidCache()) field else null
        set(value) = run {
            field = value
            cacheVersion = Node.globalCacheVersion
        }

    var nextGenStepCache: TreeNode? = null

    private fun isValidCache(): Boolean = cacheVersion == Node.globalCacheVersion

    val populatedChildrenCount: Byte = (listOf(nw, ne, sw, se).count { it.population.isNotZero() }).toByte()

    override val bounds: Bounds = run {
        if (this.population.isZero()) {
            Bounds(
                top = 0L,
                left = 0L,
                bottom = 0L,
                right = 0L
            )
        } else {

            // Initialize bounds to crazy size - currently set to the universe limit
            var bounds = Bounds(
                LifeUniverse.MAX_VALUE,
                LifeUniverse.MAX_VALUE,
                LifeUniverse.MIN_VALUE,
                LifeUniverse.MIN_VALUE,
            )

            val offset =
                if (level == 1) 1L else LifeUniverse.pow2(level - 2)
            val negatedOffset = if (level == 1) 0L else -offset

            bounds = calculateChildBounds(nw, negatedOffset, negatedOffset, bounds)
            bounds = calculateChildBounds(ne, negatedOffset, offset, bounds)
            bounds = calculateChildBounds(sw, offset, negatedOffset, bounds)
            bounds = calculateChildBounds(se, offset, offset, bounds)
            bounds
        }
    }

    private fun calculateChildBounds(
        child: Node,
        topBottomOffset: Long,
        leftRightOffset: Long,
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
                minOf(bounds.top, translatedBounds.top),
                minOf(bounds.left, translatedBounds.left),
                maxOf(bounds.bottom, translatedBounds.bottom),
                maxOf(bounds.right, translatedBounds.right)
            )
        } else {
            bounds
        }
    }

    override fun toString(): String {
        return "id=$id, level=$level, population=$population, born=$aliveSince"
    }

    override fun hashCode(): Int = hash


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TreeNode

        if (id != other.id) return false
        if (level != other.level) return false
        if (population != other.population) return false

        if (nw != other.nw) return false
        if (ne != other.ne) return false
        if (sw != other.sw) return false
        if (se != other.se) return false

        return (hash == other.hash)

    }
}