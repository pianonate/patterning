package patterning.life

import java.math.BigDecimal

data class DrawNodePathEntry(
    val node: Node,
    val size: BigDecimal,
    val left: BigDecimal,
    val top: BigDecimal,
    val direction: Direction
)