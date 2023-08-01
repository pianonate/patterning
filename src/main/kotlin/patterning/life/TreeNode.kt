package patterning.life

import patterning.util.FlexibleInteger
import patterning.util.StatMap

class TreeNode(
    val nw: Node,
    val ne: Node,
    val sw: Node,
    val se: Node,
    override var id: Int,
    val generation: FlexibleInteger
) : Node {

    override val level: Int = nw.level + 1
    override val population: FlexibleInteger = nw.population + ne.population + sw.population + se.population

    private val hash = Node.calcHash(nw.id, ne.id, sw.id, se.id)
    private var cacheVersion: Int = -1 // Version when cache was last set to valid

    var nextGenerationCache: TreeNode? = null
        get() = if (isValidCache()) field else null
        set(value) = run {
            field = value
            cacheVersion = Node.globalVersion
        }

    var level2NextCache: TreeNode? = null

    private fun isValidCache(): Boolean = cacheVersion == Node.globalVersion

    val populatedChildrenCount: Int = listOf(nw, ne, sw, se).count { it.population > FlexibleInteger.ZERO }

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

    fun countUnusedInMap(hashMap: StatMap<Int, MutableList<TreeNode>>): Int {
        val size = hashMap.size
        return countUnreferencedNodes(hashMap)
    }

    private fun countUnreferencedNodes(hashMap: StatMap<Int, MutableList<TreeNode>>): Int {
        val visited = mutableSetOf<TreeNode>()
        traverseTree(this, visited)

        var count = 0

        for ((_, nodeList) in hashMap) {
            for (node in nodeList) {
                if (node !in visited) {
                    count++
                }
            }
        }

        return count
    }

    private fun traverseTree(node: TreeNode, visited: MutableSet<TreeNode>) {
        if (node in visited) {
            return
        }

        visited.add(node)

        for (child in listOf(node.nw, node.ne, node.sw, node.se)) {
            if (child.level > 0) {
                traverseTree(child as TreeNode, visited)
            }
        }
    }

    override fun toString(): String {
        return "id=$id, level=$level, population=$population, gen=$generation"
    }

    override fun hashCode(): Int = hash


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
        if (nextGenerationCache != other.nextGenerationCache) return false
        return (level2NextCache == other.level2NextCache)
    }
}