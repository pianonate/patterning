package patterning

import java.math.BigInteger

class Node {
    @JvmField
    var nw: Node? = null
    @JvmField
    var ne: Node? = null
    @JvmField
    var sw: Node? = null
    @JvmField
    var se: Node? = null
    @JvmField
    var id: Int
    @JvmField
    val level: Int
    @JvmField
    val population: BigInteger
    val step: Int
    @JvmField
    var cache: Node? = null
    @JvmField
    var quickCache: Node? = null
    @JvmField
    var hashmapNext: Node? = null

    // falseLeaf, trueLeaf constructors
    constructor(id: Int, population: BigInteger, level: Int) {
        this.id = id
        this.population = population
        this.level = level
        step = 0
    }

    constructor(nw: Node, ne: Node, sw: Node, se: Node, id: Int, step: Int) {
        this.nw = nw
        this.ne = ne
        this.sw = sw
        this.se = se
        this.id = id
        this.step = step
        level = nw.level + 1
        population = nw.population.add(ne.population).add(sw.population).add(se.population)
    }

    fun hasChanged(): Boolean {
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
    }

    companion object {
        private val changedNodes = HashSet<Node>()
        @JvmStatic
        fun addChanged(node: Node) {
            changedNodes.add(node)
        }

        @JvmStatic
        fun clearChanged() {
            changedNodes.clear()
        }

        private fun hasChanged(node: Node): Boolean {
            return changedNodes.contains(node)
        }
    }
}