package patterning.life

import kotlinx.coroutines.runBlocking
import patterning.util.FlexibleInteger
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/*
 great article:  https://www.dev-mind.blog/hashlife/ and code: https://github.com/ngmsoftware/hashlife

* this wikipedia article talks about hash caching, superspeed memoization and representation of the tree ads well as manual garbage collection
- better than chatgpt - refer to it when you get stuck: https://en.wikipedia.org/wiki/Hashlife
- also: https://web.archive.org/web/20220131050938/https://jennyhasahat.github.io/hashlife.html
 */

class LifeUniverse internal constructor() {

    private var hashMap = ConcurrentHashMap<Int, MutableList<TreeNode>>(HASHMAP_INITIAL_CAPACITY)
    //private var hashMap = ConcurrentHashMap<Int, TreeNode>(HASHMAP_INITIAL_CAPACITY)

    private var emptyTreeCache: MutableMap<Int, TreeNode> = HashMap()
    private val level2Cache: MutableMap<Int, TreeNode> = HashMap(LEVEL_2_CACHE_INITIAL_CAPACITY)
    private val _bitcounts: ByteArray = ByteArray(0x758)
    private val birthRule = 1 shl 3
    private val survivalRule = 1 shl 2 or (1 shl 3)
    private var generation: FlexibleInteger = FlexibleInteger.ZERO
    private var birthFrame = 0

    var lastId: AtomicInteger = AtomicInteger(Node.startId)
        private set

    private val rootReference = runBlocking { AtomicReference(emptyTree(3)) }

    var root: TreeNode
        get() = rootReference.get()
        private set(value) = rootReference.set(value)

    val rootBounds: Bounds
        get() = root.bounds

    val lifeInfo = LifeInfo(this::updatePatternInfo)

    /*  step is number of generations to calculate at one time, written as 2^n

        Node.globalVersion controls cache management as setting a higher step
        invalidates the entire cache - i.e.:
        preserve the tree, but remove all cached generations forward

        alsoQuick (currently removed but it cleared out the quickCache also)
        came from the original algorithm and was used when
        (I believe) that the rule set was changed to allow for different birth/survival rules
        so you may need to bring back invalidating the quickCache the same way if you ever
        allow for changing birth/survival rules

        */
    var step: Int = 0
        set(value) {
            if (value != field) {
                field = value
                Node.globalCacheVersion = step
            }
        }

    init {
        _bitcounts[0] = 0
        _bitcounts[1] = 1
        _bitcounts[2] = 1
        _bitcounts[3] = 2
        _bitcounts[4] = 1
        _bitcounts[5] = 2
        _bitcounts[6] = 2
        _bitcounts[7] = 3
        _bitcounts[8] = 1
        _bitcounts[9] = 2
        _bitcounts[10] = 2
        _bitcounts[11] = 3
        _bitcounts[12] = 2
        _bitcounts[13] = 3
        _bitcounts[14] = 3
        _bitcounts[15] = 4
        for (i in 0x10..0x757) {
            _bitcounts[i] = (
                    _bitcounts[i and 0xF] +
                            _bitcounts[i shr 4 and 0xF] +
                            _bitcounts[i shr 8]).toByte()
        }
    }

    private fun evalMask(bitmask: Int): Int {
        val rule = if (bitmask and 32 != 0) survivalRule else birthRule
        return rule shr _bitcounts[bitmask and 0x757].toInt() and 1
    }

    private fun level1Create(bitmask: Int): TreeNode {
        return createNode(
            if (bitmask and 1 != 0) Node.livingNode else Node.deadNode,
            if (bitmask and 2 != 0) Node.livingNode else Node.deadNode,
            if (bitmask and 4 != 0) Node.livingNode else Node.deadNode,
            if (bitmask and 8 != 0) Node.livingNode else Node.deadNode
        )
    }

    private fun moveField(fieldX: ArrayList<Int>, fieldY: ArrayList<Int>, offsetX: Int, offsetY: Int) {
        for (i in 0 until fieldX.size) {
            val x = fieldX[i]
            val y = fieldY[i]

            fieldX[i] = x + offsetX
            fieldY[i] = y + offsetY

        }
    }

    private fun emptyTree(level: Int): TreeNode {

        emptyTreeCache[level]?.let { return it }

        val t: Node = if (level == 1) {
            Node.deadNode
        } else {
            emptyTree(level - 1)
        }

        return createNode(t, t, t, t).also { node ->
            emptyTreeCache[level] = node
        }
    }

    // this is just used when setting up the field initially unless I'm missing
    // something
    private fun getBounds(fieldX: ArrayList<Int>, fieldY: ArrayList<Int>): Bounds {
        if (fieldX.size == 0) {
            return Bounds(
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
            )
        }

        var minX = FlexibleInteger.create(fieldX[0])
        var maxX = minX
        var minY = FlexibleInteger.create(fieldY[0])
        var maxY = minY
        val len = fieldX.size

        for (i in 1 until len) {
            val x = FlexibleInteger.create(fieldX[i])
            val y = FlexibleInteger.create(fieldY[i])

            if (x < minX) {
                minX = x
            } else if (x > maxX) {
                maxX = x
            }

            if (y < minY) {
                minY = y
            } else if (y > maxY) {
                maxY = y
            }
        }

        return Bounds(
            minY,
            minX,
            maxY,
            maxX
        )
    }

    fun newLife(fieldX: ArrayList<Int>, fieldY: ArrayList<Int>) {
        val bounds = getBounds(fieldX, fieldY)
        val level = bounds.getLevelFromBounds()

        /* nothing coming in will be so ginormous that it will exceed Integer.MAX_VALUE */
        val offset = pow2(level - 1).toInt()
        val count = fieldX.size
        moveField(fieldX, fieldY, offset, offset)
        runBlocking { root = setupLifeRecurse(0, count - 1, fieldX, fieldY, level) }
    }

    private fun setupLifeRecurse(
        start: Int,
        end: Int,
        fieldX: ArrayList<Int>,
        fieldY: ArrayList<Int>,
        recurseLevel: Int
    ): TreeNode {
        if (start > end) {
            return emptyTree(recurseLevel)
        }
        if (recurseLevel == 2) {
            return level2Setup(start, end, fieldX, fieldY)!!
        }

        val offset = 1 shl (recurseLevel - 1)
        val part3 = partition(start, end, fieldY, fieldX, offset)
        val part2 = partition(start, part3 - 1, fieldX, fieldY, offset)
        val part4 = partition(part3, end, fieldX, fieldY, offset)

        return createNode(
            setupLifeRecurse(start, part2 - 1, fieldX, fieldY, recurseLevel - 1),
            setupLifeRecurse(part2, part3 - 1, fieldX, fieldY, recurseLevel - 1),
            setupLifeRecurse(part3, part4 - 1, fieldX, fieldY, recurseLevel - 1),
            setupLifeRecurse(part4, end, fieldX, fieldY, recurseLevel - 1)
        )
    }

    private fun partition(
        start: Int, end: Int,
        testField: ArrayList<Int>, otherField: ArrayList<Int>,
        offset: Int
    ): Int {

        val elements = (start..end).map { Pair(testField[it], otherField[it]) }

        val (leftPartition, rightPartition) = elements.partition { it.first and offset == 0 }

        val leftIndex = start + leftPartition.size

        // Overwrite the original ArrayList<Int> with the sorted elements
        (leftPartition + rightPartition).forEachIndexed { index, pair ->
            testField[start + index] = pair.first
            otherField[start + index] = pair.second
        }

        return leftIndex
    }

    private fun level2Setup(start: Int, end: Int, fieldX: ArrayList<Int>, fieldY: ArrayList<Int>): TreeNode? {
        var set = 0
        var x: Int
        var y: Int
        for (i in start..end) {
            x = fieldX[i]
            y = fieldY[i]

            // interleave 2-bit x and y values
            set = set or (1 shl (x and 1 or (y and 1 or (x and 2) shl 1) or (y and 2 shl 2)))
        }
        if (level2Cache.containsKey(set)) {
            return level2Cache[set]
        }
        val tree = createNode(
            level1Create(set),
            level1Create(set shr 4),
            level1Create(set shr 8),
            level1Create(set shr 12)
        )
        level2Cache[set] = tree
        return tree
    }

    private fun nodeLevel2Next(node: TreeNode): TreeNode {

        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se

        if (nw is TreeNode && ne is TreeNode && sw is TreeNode && se is TreeNode) {
            val bitmask = (nw.nw.population.toInt() shl (15))
                .or(nw.ne.population.toInt() shl (14))
                .or(ne.nw.population.toInt() shl (13))
                .or(ne.ne.population.toInt() shl (12))
                .or(nw.sw.population.toInt() shl (11))
                .or(nw.se.population.toInt() shl (10))
                .or(ne.sw.population.toInt() shl (9))
                .or(ne.se.population.toInt() shl (8))
                .or(sw.nw.population.toInt() shl (7))
                .or(sw.ne.population.toInt() shl (6))
                .or(se.nw.population.toInt() shl (5))
                .or(se.ne.population.toInt() shl (4))
                .or(sw.sw.population.toInt() shl (3))
                .or(sw.se.population.toInt() shl (2))
                .or(se.sw.population.toInt() shl (1))
                .or(se.se.population.toInt())
            val result = evalMask(bitmask shr 5) or (
                    evalMask(bitmask shr 4) shl 1) or (
                    evalMask(bitmask shr 1) shl 2) or (
                    evalMask(bitmask) shl 3)
            return level1Create(result)
        } else {
            throw IllegalStateException("Children nodes should be InternalNode type for nodeLevel2Next")
        }
    }

    // create or search for a node given its children
    private fun createNode(nw: Node, ne: Node, sw: Node, se: Node): TreeNode {
        val hash = Node.calcHash(nw.id, ne.id, sw.id, se.id)

        var newNode: TreeNode? = null

        // using compute for thread safety - it returns a nodelist so we either just return the one associated
        // with finding a matching node in it, or we create a new node, add it to the nodelist and then return that
        // back to the compute
        // finally whether we created a new one or found an existing match, we return that TreeNode to be
        // cached in the next generation cache
        hashMap.compute(hash) { _, oldNodeList ->
            val nodeList = oldNodeList ?: mutableListOf()

            for (node in nodeList) {

                if (node.nw == nw && node.ne == ne && node.sw == sw && node.se == se) {
                    newNode = node
                    return@compute nodeList
                }
            }

            newNode = TreeNode(nw, ne, sw, se, lastId.getAndIncrement() /*++*/, aliveSince = birthFrame)
            nodeList.add(newNode!!)

            nodeList // return the updated list
        }

        return newNode ?: throw Exception("New node should not be null")
    }

    /* private fun createNode(nw: Node, ne: Node, sw: Node, se: Node): TreeNode {
        val hash = Node.calcHash(nw.id, ne.id, sw.id, se.id)

        // using compute for thread safety - it returns a TreeNode
        return hashMap.compute(hash) { _, oldNode ->
            // If an oldNode exists and matches the children, return it
            if (oldNode != null && oldNode.nw == nw && oldNode.ne == ne && oldNode.sw == sw && oldNode.se == se) {
                return@compute oldNode
            }
            // Otherwise create a new TreeNode, add it to the HashMap, and return it
            return@compute TreeNode(nw, ne, sw, se, lastId++, birthFrame)
        } ?: throw Exception("New node should not be null")
    }*/

    private fun updatePatternInfo() {

        lifeInfo.addOrUpdate("level", FlexibleInteger.create(root.level))
        lifeInfo.addOrUpdate("step", pow2(step))
        lifeInfo.addOrUpdate("generation", generation)
        lifeInfo.addOrUpdate("population", root.population)
        lifeInfo.addOrUpdate("lastId", FlexibleInteger.create(lastId.get()))
        /* lifeInfo.addOrUpdate("normalRecurse", recurse.get())
         lifeInfo.addOrUpdate("stepRecurse", recurseStep.get())*/
        lifeInfo.addOrUpdate("width", root.bounds.width)
        lifeInfo.addOrUpdate("height", root.bounds.height)
    }

    /*    private var recurse = AtomicInteger(0)
        private var recurseStep = AtomicInteger(0)*/

    fun nextGeneration() {
        var currentRoot = this.root

        /*      recurse = AtomicInteger(0)
              recurseStep = AtomicInteger(0)*/

        // when you're super stepping you need the first argument re:step to grow it immediately large enough!
        // and if stepNextGeneration is expanding the universe then the population sizes won't match up
        // so so make sure we're big enough to handle the expansion
        while (currentRoot.level <= step + 2 ||
            currentRoot.nw.population != currentRoot.nw.se.se.population ||
            currentRoot.ne.population != currentRoot.ne.sw.sw.population ||
            currentRoot.sw.population != currentRoot.sw.ne.ne.population ||
            currentRoot.se.population != currentRoot.se.nw.nw.population
        ) {
            currentRoot = expandUniverse(currentRoot)
        }

        val nextRoot = nextGenerationRecurse(node = currentRoot)

        this.root = nextRoot

        generation += pow2(step)
        birthFrame += 1
    }

    private fun expandUniverse(node: Node): TreeNode {
        val t = emptyTree(node.level - 1)
        return createNode(
            createNode(t, t, t, node.nw),
            createNode(t, t, node.ne, t),
            createNode(t, node.sw, t, t),
            createNode(node.se, t, t, t)
        )
    }

    /*
        nextGenerationRecurse is general function that must work for all scenarios,
        not just the specific condition nodeQuickNextGeneration is optimized for.
        It employs a straightforward division of the grid into four 2x2 sub-grids.
        Because it's a more generic solution, it doesn't take the specific optimized
        path that nodeQuickNextGeneration does.
    */

    private fun nextGenerationRecurse(node: TreeNode): TreeNode {
        node.nextGenCache?.let { return it }
        // recurse.getAndIncrement()

        if (step == node.level - 2) {
            return stepGenerationRecurse(node)
        }

        if (node.level == 2) {
            // level two's are straightforward to create and not that many of them
            // so we cache them on the node itself
            node.nextGenStepCache = node.nextGenStepCache ?: nodeLevel2Next(node)
            return node.nextGenStepCache as TreeNode
        }

        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se

        val n00 =
            createNode(nw.nw.se, nw.ne.sw, nw.sw.ne, nw.se.nw)
        val n01 = createNode(nw.ne.se, ne.nw.sw, nw.se.ne, ne.sw.nw)
        val n02 = createNode(ne.nw.se, ne.ne.sw, ne.sw.ne, ne.se.nw)
        val n10 = createNode(nw.sw.se, nw.se.sw, sw.nw.ne, sw.ne.nw)
        val n11 = createNode(nw.se.se, ne.sw.sw, sw.ne.ne, se.nw.nw)
        val n12 = createNode(ne.sw.se, ne.se.sw, se.nw.ne, se.ne.nw)
        val n20 = createNode(sw.nw.se, sw.ne.sw, sw.sw.ne, sw.se.nw)
        val n21 = createNode(sw.ne.se, se.nw.sw, sw.se.ne, se.sw.nw)
        val n22 = createNode(se.nw.se, se.ne.sw, se.sw.ne, se.se.nw)

        return (createNode(
            nextGenerationRecurse(createNode(n00, n01, n10, n11)),
            nextGenerationRecurse(createNode(n01, n02, n11, n12)),
            nextGenerationRecurse(createNode(n10, n11, n20, n21)),
            nextGenerationRecurse(createNode(n11, n12, n21, n22))
        ).also { node.nextGenCache = it })
    }

    /* nodeStepGeneration following is specifically optimized for the scenario
        when the level of the current node is exactly two more than the
        number of generations to compute.It leverages this
        condition to perform fewer operations and directly compute
        the next generation. This is achieved by dividing the grid
        into nine overlapping 2x2 sub-grids,
        enabling the calculation of the next generation in fewer steps.*/

    private fun stepGenerationRecurse(node: TreeNode): TreeNode {
        // recurseStep.getAndIncrement()

        node.nextGenStepCache?.let { return it }

        if (node.level == 2) {
            return nodeLevel2Next(node).also { node.nextGenStepCache = it }
        }

        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se

        val n00 = stepGenerationRecurse(nw as TreeNode)
        val n01 = stepGenerationRecurse(createNode(nw.ne, ne.nw, nw.se, ne.sw))
        val n02 = stepGenerationRecurse(ne as TreeNode)
        val n10 = stepGenerationRecurse(createNode(nw.sw, nw.se, sw.nw, sw.ne))
        val n11 = stepGenerationRecurse(createNode(nw.se, ne.sw, sw.ne, se.nw))
        val n12 = stepGenerationRecurse(createNode(ne.sw, ne.se, se.nw, se.ne))
        val n20 = stepGenerationRecurse(sw as TreeNode)
        val n21 = stepGenerationRecurse(createNode(sw.ne, se.nw, sw.se, se.sw))
        val n22 = stepGenerationRecurse(se as TreeNode)

        return createNode(
            stepGenerationRecurse(createNode(n00, n01, n10, n11)),
            stepGenerationRecurse(createNode(n01, n02, n11, n12)),
            stepGenerationRecurse(createNode(n10, n11, n20, n21)),
            stepGenerationRecurse(createNode(n11, n12, n21, n22))
        ).also { node.nextGenStepCache = it }
    }

    companion object {
        private const val LEVEL_2_CACHE_INITIAL_CAPACITY = 0x10000

        private const val UNIVERSE_LEVEL_LIMIT = 2048

        private val _powers: HashMap<Int, FlexibleInteger> = HashMap()

        fun pow2(x: Int): FlexibleInteger {
            return _powers.getOrPut(x) { FlexibleInteger.create(BigInteger.valueOf(2).pow(x)) }
        }

        val MAX_VALUE by lazy { pow2(UNIVERSE_LEVEL_LIMIT) }
        val MIN_VALUE by lazy { -MAX_VALUE }
        private val HASHMAP_INITIAL_CAPACITY = pow2(24).toInt()
    }
}