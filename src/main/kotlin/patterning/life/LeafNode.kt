package patterning.life

import patterning.util.isOne

class LeafNode(
    override var id: Int,
    override val population: Long,
) : Node {
    override val level: Int = 0
    override val nw: Node = this
    override val ne: Node = this
    override val sw: Node = this
    override val se: Node = this

    override val bounds: Bounds = if (population.isOne())
        Bounds(
            top = -1L,
            left = -1L,
            bottom = -1L,
            right = -1L
        )
    else
        Bounds(
            top = 0L,
            left = 0L,
            bottom = 0L,
            right = 0L
        )

    override fun toString(): String {
        return if (population.isOne()) "Living Leaf" else "Dead Leaf"
    }
}