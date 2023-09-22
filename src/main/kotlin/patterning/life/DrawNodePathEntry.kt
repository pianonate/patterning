package patterning.life


data class DrawNodePathEntry(
    val node: Node,
    val size: Float,
    val left: Float,
    val top: Float,
    val direction: Direction
)