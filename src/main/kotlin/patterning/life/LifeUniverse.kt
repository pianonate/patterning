package patterning.life

import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.IntBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import patterning.util.FlexibleInteger

/*
 great article:  https://www.dev-mind.blog/hashlife/ and code: https://github.com/ngmsoftware/hashlife

* this wikipedia article talks about hash caching, superspeed memoization and representation of the tree ads well as manual garbage collection
- better than chatgpt - refer to it when you get stuck: https://en.wikipedia.org/wiki/Hashlife
- also: https://web.archive.org/web/20220131050938/https://jennyhasahat.github.io/hashlife.html
 */

class LifeUniverse internal constructor() {
    private var lastId: Int = Node.startId

    private var hashMap = ConcurrentHashMap<Int, TreeNode>(HASHMAP_INITIAL_CAPACITY)
    private var emptyTreeCache: MutableMap<Int, TreeNode> = HashMap()
    private val level2Cache: MutableMap<Int, TreeNode> = HashMap(LEVEL_2_CACHE_INITIAL_CAPACITY)
    private val _bitcounts: ByteArray = ByteArray(0x758)
    private val birthRule = 1 shl 3
    private val survivalRule = 1 shl 2 or (1 shl 3)
    private var generation: FlexibleInteger = FlexibleInteger.ZERO
    private var birthFrame = 0

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

    private fun moveField(fieldX: IntBuffer, fieldY: IntBuffer, offsetX: Int, offsetY: Int) {
        for (i in 0 until fieldX.capacity()) {
            val x = fieldX[i]
            val y = fieldY[i]
            fieldX.put(i, x + offsetX)
            fieldY.put(i, y + offsetY)
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
    private fun getBounds(fieldX: IntBuffer, fieldY: IntBuffer): Bounds {
        if (fieldX.capacity() == 0) {
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
        val len = fieldX.capacity()

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

    fun setupLife(fieldX: IntBuffer, fieldY: IntBuffer) {
        LifePattern.resetMathContext()
        val bounds = getBounds(fieldX, fieldY)
        val level = bounds.getLevelFromBounds()

        /* nothing coming in will be so ginormous that it will exceed Integer.MAX_VALUE */
        val offset = FlexibleInteger.pow2(level - 1).toInt()
        val count = fieldX.capacity()
        moveField(fieldX, fieldY, offset, offset)
        runBlocking { root = setupLifeRecurse(0, count - 1, fieldX, fieldY, level) }
    }

    private fun setupLifeRecurse(
        start: Int,
        end: Int,
        fieldX: IntBuffer,
        fieldY: IntBuffer,
        recurseLevel: Int
    ): TreeNode {
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

    private fun level2Setup(start: Int, end: Int, fieldX: IntBuffer, fieldY: IntBuffer): TreeNode? {
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

    private fun createNode(nw: Node, ne: Node, sw: Node, se: Node): TreeNode {
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
    }

    private fun updatePatternInfo() {

        lifeInfo.addOrUpdate("level", FlexibleInteger.create(root.level))
        lifeInfo.addOrUpdate("step", FlexibleInteger.pow2(step))
        lifeInfo.addOrUpdate("generation", generation)
        lifeInfo.addOrUpdate("population", root.population)
        lifeInfo.addOrUpdate("lastId", FlexibleInteger.create(lastId))
        /*        patternInfo.addOrUpdate("hits", hashmap.hits)
                patternInfo.addOrUpdate("misses", hashmap.misses)
                patternInfo.addOrUpdate("%", hashmap.hitRate * 100)
                patternInfo.addOrUpdate("puts", hashmap.puts)*/
        lifeInfo.addOrUpdate("recurse", recurse)
        lifeInfo.addOrUpdate("quick", quick)
        val bounds = rootBounds
        lifeInfo.addOrUpdate("width", bounds.width)
        lifeInfo.addOrUpdate("height", bounds.height)
    }

    private var recurse = 0
    private var quick = 0

    suspend fun nextGeneration() {
        var currentRoot = this.root
        // val biggestUsage = hashMap.getValueWithHighestUsageCount()?.get(0)

        /*        println(
                    "size: ${hashMap.size} unused: ${currentRoot.countUnusedInMap(hashMap)} biggest:${biggestUsage!!.id} usage ${
                        hashMap.getUsageCountForKey(
                            biggestUsage.hashCode()
                        )
                    }"
                )*/

        /*      if (hashMap.size > 2_000_000) {
                  val myScope = CoroutineScope((Dispatchers.Default))
                  myScope.launch {
                      withContext(Dispatchers.IO) {
                          removeOldest()
                      }
                  }
              }*/
        recurse = 0
        quick = 0

        // when you're super stepping you need the first argument re:step to grow it immediately large enough!
        while (currentRoot.level <= step + 2 ||
            currentRoot.nw.population != ((currentRoot.nw as TreeNode).se as TreeNode).se.population ||
            currentRoot.ne.population != ((currentRoot.ne as TreeNode).sw as TreeNode).sw.population ||
            currentRoot.sw.population != ((currentRoot.sw as TreeNode).ne as TreeNode).ne.population ||
            currentRoot.se.population != ((currentRoot.se as TreeNode).nw as TreeNode).nw.population
        ) {
            currentRoot = expandUniverse(currentRoot)
        }

        val populationDivisor =
            FlexibleInteger.create(
                currentRoot.population.bigDecimal.divide(BigDecimal(4), RoundingMode.HALF_UP).toBigInteger()
            )
        val nextRoot = nextGenerationRecurse(node = currentRoot, maxLevel = currentRoot.level - 2)
        //val nextRoot = nextGenerationRecurse(node = currentRoot, populationDivisor = populationDivisor)

        // using the intermediate variable to allow AtomicReference to be used
        this.root = nextRoot

        generation += FlexibleInteger.pow2(step)
        birthFrame += 1
    }

    private fun expandUniverse(node: TreeNode): TreeNode {
        val t = emptyTree(node.level - 1)
        return createNode(
            createNode(t, t, t, node.nw),
            createNode(t, t, node.ne, t),
            createNode(t, node.sw, t, t),
            createNode(node.se, t, t, t)
        )
    }

    private suspend fun nextGenerationRecurse(node: TreeNode, maxLevel: Int): TreeNode {
        node.nextGenerationCache?.let { return it }
        recurse++

        if (step == node.level - 2) {
            return nodeQuickNextGeneration(node/*, node.level -1*/)
        }

        if (node.level == 2) {
            // level two's are straightforward to create and not that many of them
            // so we cache them on the node itself
            node.level2NextCache = node.level2NextCache ?: nodeLevel2Next(node)
            return node.level2NextCache as TreeNode
        }

        val nw = node.nw as TreeNode
        val ne = node.ne as TreeNode
        val sw = node.sw as TreeNode
        val se = node.se as TreeNode

        val n00 = createNode(
            (nw.nw as TreeNode).se,
            (nw.ne as TreeNode).sw,
            (nw.sw as TreeNode).ne,
            (nw.se as TreeNode).nw
        )
        val n01 = createNode(nw.ne.se, (ne.nw as TreeNode).sw, nw.se.ne, (ne.sw as TreeNode).nw)
        val n02 = createNode(ne.nw.se, (ne.ne as TreeNode).sw, ne.sw.ne, (ne.se as TreeNode).nw)
        val n10 = createNode(nw.sw.se, nw.se.sw, (sw.nw as TreeNode).ne, (sw.ne as TreeNode).nw)
        val n11 = createNode(nw.se.se, ne.sw.sw, sw.ne.ne, (se.nw as TreeNode).nw)
        val n12 = createNode(ne.sw.se, ne.se.sw, se.nw.ne, (se.ne as TreeNode).nw)
        val n20 = createNode(sw.nw.se, sw.ne.sw, (sw.sw as TreeNode).ne, (sw.se as TreeNode).nw)
        val n21 = createNode(sw.ne.se, se.nw.sw, sw.se.ne, (se.sw as TreeNode).nw)
        val n22 = createNode(se.nw.se, se.ne.sw, se.sw.ne, (se.se as TreeNode).nw)

        return (if (node.level > maxLevel) {
            coroutineScope {
                val asyncNW = async { nextGenerationRecurse(createNode(n00, n01, n10, n11), maxLevel) }
                val asyncNE = async { nextGenerationRecurse(createNode(n01, n02, n11, n12), maxLevel) }
                val asyncSW = async { nextGenerationRecurse(createNode(n10, n11, n20, n21), maxLevel) }
                val asyncSE = async { nextGenerationRecurse(createNode(n11, n12, n21, n22), maxLevel) }
                createNode(asyncNW.await(), asyncNE.await(), asyncSW.await(), asyncSE.await())
            }
        } else {
            val newNW = nextGenerationRecurse(createNode(n00, n01, n10, n11), maxLevel)
            val newNE = nextGenerationRecurse(createNode(n01, n02, n11, n12), maxLevel)
            val newSW = nextGenerationRecurse(createNode(n10, n11, n20, n21), maxLevel)
            val newSE = nextGenerationRecurse(createNode(n11, n12, n21, n22), maxLevel)
            createNode(newNW, newNE, newSW, newSE)
        }).also { node.nextGenerationCache = it }
    }

    private fun nodeQuickNextGeneration(node: TreeNode): TreeNode {

        if (node.level2NextCache != null) {
            return node.level2NextCache as TreeNode
        }
        quick++

        if (node.level == 2) {
            return nodeLevel2Next(node).also { node.level2NextCache = it }
        }

        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se
        val n00 = nodeQuickNextGeneration(nw as TreeNode)
        val n01 = nodeQuickNextGeneration(
            createNode(nw.ne, (ne as TreeNode).nw, nw.se, ne.sw)
        )
        val n02 = nodeQuickNextGeneration(ne)
        val n10 = nodeQuickNextGeneration(
            createNode(nw.sw, nw.se, (sw as TreeNode).nw, sw.ne),
        )
        val n11 = nodeQuickNextGeneration(
            createNode(nw.se, ne.sw, sw.ne, (se as TreeNode).nw),
        )
        val n12 = nodeQuickNextGeneration(
            createNode(ne.sw, ne.se, se.nw, se.ne),
        )
        val n20 = nodeQuickNextGeneration(sw)
        val n21 = nodeQuickNextGeneration(
            createNode(sw.ne, se.nw, sw.se, se.sw),
        )
        val n22 = nodeQuickNextGeneration(se)
        return createNode(
            nodeQuickNextGeneration(createNode(n00, n01, n10, n11)),
            nodeQuickNextGeneration(createNode(n01, n02, n11, n12)),
            nodeQuickNextGeneration(createNode(n10, n11, n20, n21)),
            nodeQuickNextGeneration(createNode(n11, n12, n21, n22))
        ).also { node.level2NextCache = it }
    }

    companion object {
        private val HASHMAP_INITIAL_CAPACITY = FlexibleInteger.pow2(24).toInt()
        private val LEVEL_2_CACHE_INITIAL_CAPACITY = 0x10000
    }
}