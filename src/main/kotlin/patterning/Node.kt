package patterning

interface Node {
    var id: Int
    val population: FlexibleInteger
    val level: Int

    companion object {
        val unbornNode = LeafNode(3, FlexibleInteger.ZERO)
        val aliveNode = LeafNode(2, FlexibleInteger.ONE)
        val startId = unbornNode.id + 1
    }
}

class LeafNode(
    override var id: Int,
    override val population: FlexibleInteger,
) : Node {
    override val level: Int = 0
}

class InternalNode(
    val nw: Node,
    val ne: Node,
    val sw: Node,
    val se: Node,
    override var id: Int
) : Node {

    override val level: Int = nw.level + 1
    override val population: FlexibleInteger = nw.population + ne.population + sw.population + se.population

    var cache: InternalNode? = null
    var quickCache: InternalNode? = null
    var hashmapNext: InternalNode? = null

}