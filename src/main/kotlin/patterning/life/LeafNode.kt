package patterning.life

import patterning.util.FlexibleInteger
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

    override val bounds: BoundsLong = if (population.isOne())
        Bounds(
            top = FlexibleInteger.NEGATIVE_ONE,
            left = FlexibleInteger.NEGATIVE_ONE,
            bottom = FlexibleInteger.NEGATIVE_ONE,
            right = FlexibleInteger.NEGATIVE_ONE
        ).toBoundsLong()
    else
        Bounds(
            top = FlexibleInteger.ZERO,
            left = FlexibleInteger.ZERO,
            bottom = FlexibleInteger.ZERO,
            right = FlexibleInteger.ZERO
        ).toBoundsLong()

    override fun toString(): String {
        return if (population.isOne()) "Living Leaf" else "Dead Leaf"
    }
}