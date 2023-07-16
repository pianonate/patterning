package patterning

import java.nio.IntBuffer

/*
 great article:  https://www.dev-mind.blog/hashlife/ and code: https://github.com/ngmsoftware/hashlife

* this wikipedia article talks about hash caching, superspeed memoization and representation of the tree ads well as manual garbage collection
- better than chatgpt - refer to it when you get stuck: https://en.wikipedia.org/wiki/Hashlife
- also: https://web.archive.org/web/20220131050938/https://jennyhasahat.github.io/hashlife.html
 */

class LifeUniverse internal constructor() {
    private var lastId: Int

    // private var hashmap: HashMap<Int, Node>
    //private var hashmap: MutableMap<Int, InternalNode>
    private var hashmap = HashMap<Int, MutableList<InternalNode>>(HASHMAP_INITIAL_CAPACITY)
    private var emptyTreeCache: MutableMap<Int, InternalNode>

    // private val level2Cache: HashMap<Int, Node>
    private val level2Cache: MutableMap<Int, InternalNode>
    private val _bitcounts: ByteArray = ByteArray(0x758)
    private val ruleB: Int
    private val rulesS: Int
    private var generation: FlexibleInteger

    var root: InternalNode
    val rootBounds: Bounds
        get() = root.bounds

    val patternInfo = PatternInfo(this::updatePatternInfo)
    var step: Int = 0
        set(value) {
            if (value != field) {
                field = value
                uncache()
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

        // current rule setting
        ruleB = 1 shl 3
        rulesS = 1 shl 2 or (1 shl 3)

        // the final necessary setup bits

        // last id for nodes
        lastId = Node.startId

        //hashmap = HashMap()
        emptyTreeCache = HashMap()
        level2Cache = HashMap(0x10000)
        this.root = emptyTree(3)
        generation = FlexibleInteger.ZERO

        // number of generations to calculate at one time, written as 2^n
        step = 0
    }

    private fun evalMask(bitmask: Int): Int {
        val rule = if (bitmask and 32 != 0) rulesS else ruleB
        return rule shr _bitcounts[bitmask and 0x757].toInt() and 1
    }

    private fun level1Create(bitmask: Int): InternalNode {
        return createNode(
            if (bitmask and 1 != 0) Node.livingNode else Node.deadNode,
            if (bitmask and 2 != 0) Node.livingNode else Node.deadNode,
            if (bitmask and 4 != 0) Node.livingNode else Node.deadNode,
            if (bitmask and 8 != 0) Node.livingNode else Node.deadNode
        )
    }

    private fun moveField(fieldX: IntBuffer, fieldY: IntBuffer, offsetX: Int, offsetY: Int) {
        for (i in 0 until fieldX.capacity()) {
            val x = fieldX[i]
            val y = fieldY[i]
            fieldX.put(i, x + offsetX)
            fieldY.put(i, y + offsetY)
        }
    }

    private fun emptyTree(level: Int): InternalNode {

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

    private fun expandUniverse(node: InternalNode): InternalNode {
        val t = emptyTree(node.level - 1)
        return createNode(
            createNode(t, t, t, node.nw),
            createNode(t, t, node.ne, t),
            createNode(t, node.sw, t, t),
            createNode(node.se, t, t, t)
        )
    }

    // Preserve the tree, but remove all cached
    // generations forward
    // alsoQuick (currently removed but it cleared out the quickCache also)
    // came from the original algorithm and was used when
    // (I believe) that the rule set was changed to allow for different birth/survival rules
    // right now i don't know why it's otherwise okay to preserve
    // need to think more about this
    private fun uncache() {
        hashmap.values.forEach { nodeList ->
            nodeList.forEach {
                it.cache = null
            }
        }
    }

    // this is just used when setting up the field initially unless I'm missing
    // something
    private fun getBounds(fieldX: IntBuffer, fieldY: IntBuffer): Bounds {
        if (fieldX.capacity() == 0) {
            return Bounds(
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
                FlexibleInteger.ZERO,
            )
        }

        var minX = FlexibleInteger(fieldX[0])
        var maxX = minX
        var minY = FlexibleInteger(fieldY[0])
        var maxY = minY
        val len = fieldX.capacity()

        for (i in 1 until len) {
            val x = FlexibleInteger(fieldX[i])
            val y = FlexibleInteger(fieldY[i])

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

    fun setupLife(fieldX: IntBuffer, fieldY: IntBuffer) {
        Bounds.resetMathContext()
        val bounds = getBounds(fieldX, fieldY)
        val level = bounds.getLevelFromBounds()

        /* nothing coming in will be so ginormous that it will exceed Integer.MAX_VALUE */
        val offset = FlexibleInteger.pow2(level - 1).toInt()
        val count = fieldX.capacity()
        moveField(fieldX, fieldY, offset, offset)
        root = setupLifeRecurse(0, count - 1, fieldX, fieldY, level)
    }

    private fun setupLifeRecurse(
        start: Int,
        end: Int,
        fieldX: IntBuffer,
        fieldY: IntBuffer,
        recurseLevel: Int
    ): InternalNode {
        if (start > end) {
            return emptyTree(recurseLevel)
        }
        if (recurseLevel == 2) {
            return level2Setup(start, end, fieldX, fieldY)!!
        }

        val nextLevel = recurseLevel - 1
        val offset = 1 shl nextLevel
        val (part2, part3, part4) = partitionField(start, end, fieldX, fieldY, offset)

        return createNode(
            setupLifeRecurse(start, part2 - 1, fieldX, fieldY, nextLevel),
            setupLifeRecurse(part2, part3 - 1, fieldX, fieldY, nextLevel),
            setupLifeRecurse(part3, part4 - 1, fieldX, fieldY, nextLevel),
            setupLifeRecurse(part4, end, fieldX, fieldY, nextLevel)
        )
    }

    private fun partitionField(
        start: Int,
        end: Int,
        fieldX: IntBuffer,
        fieldY: IntBuffer,
        offset: Int
    ): Triple<Int, Int, Int> {
        val part3 = partition(start, end, fieldY, fieldX, offset)
        val part2 = partition(start, part3 - 1, fieldX, fieldY, offset)
        val part4 = partition(part3, end, fieldX, fieldY, offset)

        return Triple(part2, part3, part4)
    }

    private fun partition(
        start: Int, end: Int,
        testField: IntBuffer, otherField: IntBuffer,
        offset: Int
    ): Int {
        var leftIndex = start
        var rightIndex = end

        val leftPartition = mutableListOf<Pair<Int, Int>>()
        val rightPartition = mutableListOf<Pair<Int, Int>>()

        for (i in start..end) {
            val pair = Pair(testField[i], otherField[i])
            if (testField[i] and offset == 0) {
                leftPartition.add(pair)
                leftIndex++
            } else {
                rightPartition.add(pair)
                rightIndex--
            }
        }

        // Overwrite the original IntBuffers with the sorted elements
        (leftPartition + rightPartition).forEachIndexed { index, pair ->
            testField.put(start + index, pair.first)
            otherField.put(start + index, pair.second)
        }

        return leftIndex
    }

    private fun level2Setup(start: Int, end: Int, fieldX: IntBuffer, fieldY: IntBuffer): InternalNode? {
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

    private fun nodeLevel2Next(node: InternalNode): InternalNode {

        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se

        if (nw is InternalNode && ne is InternalNode && sw is InternalNode && se is InternalNode) {
            val bitmask = nw.nw.population.shiftLeft(15)
                .or(nw.ne.population.shiftLeft(14))
                .or(ne.nw.population.shiftLeft(13))
                .or(ne.ne.population.shiftLeft(12))
                .or(nw.sw.population.shiftLeft(11))
                .or(nw.se.population.shiftLeft(10))
                .or(ne.sw.population.shiftLeft(9))
                .or(ne.se.population.shiftLeft(8))
                .or(sw.nw.population.shiftLeft(7))
                .or(sw.ne.population.shiftLeft(6))
                .or(se.nw.population.shiftLeft(5))
                .or(se.ne.population.shiftLeft(4))
                .or(sw.sw.population.shiftLeft(3))
                .or(sw.se.population.shiftLeft(2))
                .or(se.sw.population.shiftLeft(1))
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
    private fun createNode(nw: Node, ne: Node, sw: Node, se: Node): InternalNode {
        val hash = Node.calcHash(nw.id, ne.id, sw.id, se.id)
        val nodeList = hashmap[hash] ?: mutableListOf()

        for (node in nodeList) {
            if (node.nw == nw && node.ne == ne && node.sw == sw && node.se == se) {
                return node
            }
        }

        val newInternalNode = InternalNode(nw, ne, sw, se, lastId++)
        nodeList.add(newInternalNode)

        hashmap[hash] = nodeList
        return newInternalNode
    }

    private fun updatePatternInfo() {
        patternInfo.addOrUpdate("level", root.level)
        patternInfo.addOrUpdate("step", FlexibleInteger.pow2(step).get())
        patternInfo.addOrUpdate("generation", generation.get())
        patternInfo.addOrUpdate("population", root.population.get())
        patternInfo.addOrUpdate("hashmap", hashmap.size)
        patternInfo.addOrUpdate("lastId", lastId)
        val bounds = rootBounds
        patternInfo.addOrUpdate("width", bounds.width.get())
        patternInfo.addOrUpdate("height", bounds.height.get())
        patternInfo.addOrUpdate("mc", Bounds.mathContext.precision)

    }

    fun nextGeneration() {
        var currentRoot = this.root

        while (currentRoot.level <= step + 2 ||
            currentRoot.nw.population != ((currentRoot.nw as InternalNode).se as InternalNode).se.population ||
            currentRoot.ne.population != ((currentRoot.ne as InternalNode).sw as InternalNode).sw.population ||
            currentRoot.sw.population != ((currentRoot.sw as InternalNode).ne as InternalNode).ne.population ||
            currentRoot.se.population != ((currentRoot.se as InternalNode).nw as InternalNode).nw.population
        ) {
            currentRoot = expandUniverse(currentRoot)
        }

        this.root = nextGenerationRecurse(currentRoot)

        generation += FlexibleInteger.pow2(step)
    }

    private fun nextGenerationRecurse(node: InternalNode): InternalNode {
        node.cache?.let { return it }

        if (step == node.level - 2) {
            return nodeQuickNextGeneration(node, 0)
        }

        if (node.level == 2) {
            node.quickCache = node.quickCache ?: nodeLevel2Next(node)
            return node.quickCache as InternalNode
        }

        val nw = node.nw as InternalNode
        val ne = node.ne as InternalNode
        val sw = node.sw as InternalNode
        val se = node.se as InternalNode

        @Suppress("IncorrectFormatting")
        val n00 = createNode(
            (nw.nw as InternalNode).se,
            (nw.ne as InternalNode).sw,
            (nw.sw as InternalNode).ne,
            (nw.se as InternalNode).nw
        )
        val n01 = createNode(nw.ne.se, (ne.nw as InternalNode).sw, nw.se.ne, (ne.sw as InternalNode).nw)
        val n02 = createNode(ne.nw.se, (ne.ne as InternalNode).sw, ne.sw.ne, (ne.se as InternalNode).nw)
        val n10 = createNode(nw.sw.se, nw.se.sw, (sw.nw as InternalNode).ne, (sw.ne as InternalNode).nw)
        val n11 = createNode(nw.se.se, ne.sw.sw, sw.ne.ne, (se.nw as InternalNode).nw)
        val n12 = createNode(ne.sw.se, ne.se.sw, se.nw.ne, (se.ne as InternalNode).nw)
        val n20 = createNode(sw.nw.se, sw.ne.sw, (sw.sw as InternalNode).ne, (sw.se as InternalNode).nw)
        val n21 = createNode(sw.ne.se, se.nw.sw, sw.se.ne, (se.sw as InternalNode).nw)
        val n22 = createNode(se.nw.se, se.ne.sw, se.sw.ne, (se.se as InternalNode).nw)

        val newNW = nextGenerationRecurse(createNode(n00, n01, n10, n11))
        val newNE = nextGenerationRecurse(createNode(n01, n02, n11, n12))
        val newSW = nextGenerationRecurse(createNode(n10, n11, n20, n21))
        val newSE = nextGenerationRecurse(createNode(n11, n12, n21, n22))

        return createNode(newNW, newNE, newSW, newSE).also { node.cache = it }

    }

    private fun nodeQuickNextGeneration(node: InternalNode, recurseDepth: Int): InternalNode {
        var depth = recurseDepth

        if (node.quickCache != null) {
            return node.quickCache as InternalNode
        }
        if (node.level == 2) {
            return nodeLevel2Next(node).also { node.quickCache = it }
        }
        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se
        depth += 1
        val n00 = nodeQuickNextGeneration(nw as InternalNode, depth)
        val n01 = nodeQuickNextGeneration(
            createNode(nw.ne, (ne as InternalNode).nw, nw.se, ne.sw),
            depth
        )
        val n02 = nodeQuickNextGeneration(ne, depth)
        val n10 = nodeQuickNextGeneration(
            createNode(nw.sw, nw.se, (sw as InternalNode).nw, sw.ne),
            depth
        )
        val n11 = nodeQuickNextGeneration(
            createNode(nw.se, ne.sw, sw.ne, (se as InternalNode).nw),
            depth
        )
        val n12 = nodeQuickNextGeneration(
            createNode(ne.sw, ne.se, se.nw, se.ne),
            depth
        )
        val n20 = nodeQuickNextGeneration(sw, depth)
        val n21 = nodeQuickNextGeneration(
            createNode(sw.ne, se.nw, sw.se, se.sw),
            depth
        )
        val n22 = nodeQuickNextGeneration(se, depth)
        return createNode(
            nodeQuickNextGeneration(createNode(n00, n01, n10, n11), depth),
            nodeQuickNextGeneration(createNode(n01, n02, n11, n12), depth),
            nodeQuickNextGeneration(createNode(n10, n11, n20, n21), depth),
            nodeQuickNextGeneration(createNode(n11, n12, n21, n22), depth)
        ).also { node.quickCache = it }
    }

    companion object {
        private val HASHMAP_INITIAL_CAPACITY = FlexibleInteger.pow2(24).toInt()
    }
}