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
}