package patterning.life

import java.math.BigDecimal

class DrawNodePath(
    private val shouldContinue: (Node, BigDecimal, BigDecimal, BigDecimal) -> Boolean,
    private val updateLargestDimension: (Bounds) -> Unit
) {

    var offsetsMoved: Boolean = true
    private var level: Int = 0
    private val path: MutableList<DrawNodePathEntry> = mutableListOf()
    private val cell: Cell
        get() = LifePattern.cell

    init {
        path.add(DrawNodePathEntry(Node.deadNode, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Direction.NW))
    }

    fun getLowestEntryFromRoot(root: TreeNode): DrawNodePathEntry {

        val newLevel = root.level
        updateLargestDimension(root.bounds)

        val halfSizeOffset = -cell.halfUniverseSize(newLevel)
        val universeSize = cell.universeSize(newLevel)

        // every generation the root changes so we have to use the latest root
        // to walk through the nodePath to find the lowest node that has children visible on screen
        if (root.level != path[0].node.level
            || offsetsMoved
            || childrenSizesChanged(root)
        ) {

            // if the offsets moved, or the root.level doesn't match
            // or the sizes of the children in the path don't match the sizes of the children
            // in the new root, then we need to update the path
            clear()
            updateNodePath(root, halfSizeOffset, halfSizeOffset)
            offsetsMoved = false
            level = newLevel
        }

        path[0] = DrawNodePathEntry(
            root,
            universeSize,
            halfSizeOffset,
            halfSizeOffset,
            Direction.ROOT
        )

        return lowestEntry(root)
    }

    private fun childrenSizesChanged(new: TreeNode): Boolean {
        // Traverse down the quadtree from the newRoot
        var newTreeNode: TreeNode = new

        for (entry in path) {
            if (entry.node is LeafNode) {
                return false
            }

            val existingPathNode = entry.node as TreeNode

            // Traverse down to the child node according to the direction in the path entry
            newTreeNode = when (entry.direction) {
                Direction.ROOT -> newTreeNode
                Direction.NW -> newTreeNode.nw as TreeNode
                Direction.NE -> newTreeNode.ne as TreeNode
                Direction.SW -> newTreeNode.sw as TreeNode
                Direction.SE -> newTreeNode.se as TreeNode
            }

            // Compare the count of children with population in the currentNode and in the entry from the path
            if (newTreeNode.populatedChildrenCount != existingPathNode.populatedChildrenCount) {
                return true
            }
        }

        // If no changes were found, return false
        return false

    }

    private fun clear() {
        if (path.size > 1) {
            path.subList(1, path.size).clear()
        }
    }

    private fun lowestEntry(root: TreeNode): DrawNodePathEntry {
        var currentNode: Node = root
        var lastEntry = path[0]

        if (path.size == 1) {
            return lastEntry
        }

        for (entry in path) {
            lastEntry = entry
            currentNode = when (entry.direction) {
                Direction.ROOT -> currentNode
                Direction.NW -> (currentNode as TreeNode).nw
                Direction.NE -> (currentNode as TreeNode).ne
                Direction.SW -> (currentNode as TreeNode).sw
                Direction.SE -> (currentNode as TreeNode).se
            }
        }

        return DrawNodePathEntry(
            node = currentNode,
            size = lastEntry.size,
            left = lastEntry.left,
            top = lastEntry.top,
            direction = lastEntry.direction
        )
    }

    private fun updateNodePath(
        node: Node,
        left: BigDecimal,
        top: BigDecimal
    ) {
        if (node.population.isZero()) {
            return
        }

        if (node is TreeNode) {
            val halfSize = cell.halfUniverseSize(node.level)
            val leftHalfSize = left + halfSize
            val topHalfSize = top + halfSize

            // Check all children at each level, and their relative position adjustments
            val childrenAndOffsets = listOf(
                DrawNodePathEntry(node.nw, halfSize, left, top, Direction.NW),
                DrawNodePathEntry(node.ne, halfSize, leftHalfSize, top, Direction.NE),
                DrawNodePathEntry(node.sw, halfSize, left, topHalfSize, Direction.SW),
                DrawNodePathEntry(node.se, halfSize, leftHalfSize, topHalfSize, Direction.SE)
            )

            val intersectingChildrenAndOffsets = childrenAndOffsets.filter { child ->
                shouldContinue(
                    child.node,
                    child.size,
                    child.left,
                    child.top
                )
            }

            if (intersectingChildrenAndOffsets.size == 1) {

                val intersectingChild = intersectingChildrenAndOffsets.first()
                path.add(intersectingChild)
                updateNodePath(
                    intersectingChild.node,
                    intersectingChild.left,
                    intersectingChild.top
                )
            }
        }
    }
}