package patterning.life

import patterning.util.FlexibleInteger

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