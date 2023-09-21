package patterning.life


import patterning.util.FlexibleInteger
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

    override val bounds: BoundsLong = run {
        if (this.population.isZero()) {
            Bounds(
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO
            ).toBoundsLong()
        } else {

            // Initialize bounds to crazy size - currently set to the universe limit
            var bounds = Bounds(
                FlexibleInteger.create(LifeUniverse.MAX_VALUE),
                FlexibleInteger.create(LifeUniverse.MAX_VALUE),
                FlexibleInteger.create(LifeUniverse.MIN_VALUE),
                FlexibleInteger.create(LifeUniverse.MIN_VALUE),
            ).toBoundsLong()

            val offset =
                if (level == 1) FlexibleInteger.ONE else FlexibleInteger.create(LifeUniverse.pow2(level - 2))
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
        bounds: BoundsLong
    ): BoundsLong {
        return if (child.population.isNotZero()) {
            val childBounds = child.bounds

            val translatedBounds = BoundsLong(
                childBounds.top + topBottomOffset.toLong(),
                childBounds.left + leftRightOffset.toLong(),
                childBounds.bottom + topBottomOffset.toLong(),
                childBounds.right + leftRightOffset.toLong()
            )
            BoundsLong(
                minOf(bounds.top, translatedBounds.top),
                minOf(bounds.left, translatedBounds.left),
                maxOf(bounds.bottom, translatedBounds.bottom),
                maxOf(bounds.right, translatedBounds.right)
            )
        } else {
            bounds
        }
    }

    /* fun countUnusedInMap(hashMap: StatMap<Int, MutableList<TreeNode>>): Int {
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
     } */

    /*    private fun traverse(root: Node) {
            val stack = Stack<Node>()
            stack.push(root)

            while (stack.isNotEmpty()) {
                val node = stack.pop()

                // Process the node here

                // Add children nodes to the stack if they exist
                if (node is TreeNode) {
                    stack.push(node.nw)
                    stack.push(node.ne)
                    stack.push(node.sw)
                    stack.push(node.se)
                }
            }
        }*/

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
        /* if (hash != other.hash) return false
         if (nextGenerationCache != other.nextGenerationCache) return false
         return (level2NextCache == other.level2NextCache)*/
    }
}