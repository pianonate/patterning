package patterning

class Node {

    val nw: Node?
    val ne: Node?
    val sw: Node?
    val se: Node?
    var id: Int = 0
    val level: Int
    val population: FlexibleInteger
    var cache: Node? = null
    var quickCache: Node? = null
    var hashmapNext: Node? = null

    // falseLeaf, trueLeaf constructors
    constructor(id: Int, population: FlexibleInteger, level: Int) {
        this.id = id
        this.population = population
        this.level = level
        this.nw = null
        this.ne = null
        this.sw = null
        this.se = null
    }

    constructor(nw: Node, ne: Node, sw: Node, se: Node, id: Int) {
        this.nw = nw
        this.ne = ne
        this.sw = sw
        this.se = se
        this.id = id
        level = nw.level + 1
        population = nw.population + ne.population + sw.population + se.population
    }

    /* private fun hasChanged(): Boolean {
        return hasChanged(this)
    }

    fun countNodes(): Int {
        var count = 1
        if (nw != null) {
            count += nw!!.countNodes()
            count += ne!!.countNodes()
            count += sw!!.countNodes()
            count += se!!.countNodes()
        }
        return count
    }

    fun countChangedNodes(): Int {
        var count = if (hasChanged()) 1 else 0
        if (nw != null) {
            count += nw!!.countChangedNodes()
            count += ne!!.countChangedNodes()
            count += sw!!.countChangedNodes()
            count += se!!.countChangedNodes()
        }
        return count
    }*/

    companion object {
        private val changedNodes = HashSet<Node>()
        fun addChanged(node: Node) {
            changedNodes.add(node)
        }

        fun clearChanged() {
            changedNodes.clear()
        }

        /*        private fun hasChanged(node: Node): Boolean {
            return changedNodes.contains(node)
        }*/
    }
}