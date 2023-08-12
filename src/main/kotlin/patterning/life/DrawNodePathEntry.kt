package patterning.life

import patterning.util.FlexibleDecimal

data class DrawNodePathEntry(
    val node: Node,
    val size: FlexibleDecimal,
    val left: FlexibleDecimal,
    val top: FlexibleDecimal,
    val direction: Direction
)