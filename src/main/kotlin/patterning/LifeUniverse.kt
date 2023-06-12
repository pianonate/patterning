package patterning

import patterning.Node.Companion.addChanged
import patterning.Node.Companion.clearChanged
import java.math.BigInteger
import java.nio.IntBuffer
import kotlin.math.ceil
import kotlin.math.ln

/*
 great article:  https://www.dev-mind.blog/hashlife/ and code: https://github.com/ngmsoftware/hashlife

* this wikipedia article talks about hash caching, superspeed memoization and representation of the tree as well as manual garbage collection
- better than chatgpt - refer to it when you get stuck: https://en.wikipedia.org/wiki/Hashlife
- also: https://web.archive.org/web/20220131050938/https://jennyhasahat.github.io/hashlife.html
 */

class LifeUniverse internal constructor() {
    private var lastId: Int
    private var hashmapSize: Int
    private var maxLoad: Int
    private var hashmap: HashMap<Int, Node?>
    private var emptyTreeCache: Array<Node?>
    private val level2Cache: HashMap<Int, Node>
    private val _bitcounts: ByteArray = ByteArray(0x758)
    private val ruleB: Int
    private val rulesS: Int
    private var generation: BigInteger
    private val falseLeaf: Node
    private val trueLeaf: Node

    var root: Node?
    val patternInfo: PatternInfo = PatternInfo()
    var step: Int = 0
        set(value) {
            if (value != field) {
                field = value
                uncache(/*false*/)

                // todo: why did this originally exist - it seems empty trees are all the same
                emptyTreeCache = arrayOfNulls(UNIVERSE_LEVEL_LIMIT)
                // level2Cache = new HashMap<>(0x10000); // it seems level2cache is only used on setting up the life form so maybe they were just taking the opportunity to clear it here?
            }
        }

    /*
     * Note that Java doesn't have a bitwise logical shift operator
     * that preserves the sign bit like JavaScript (>>),
     * so we can use the unsigned right shift operator (>>>) instead.
     * However, since the _bitcounts array contains only positive values,
     * we can use the regular right shift operator (>>) instead without any issues.
     */
    private fun evalMask(bitmask: Int): Int {
        val rule = if (bitmask and 32 != 0) rulesS else ruleB
        return rule shr _bitcounts[bitmask and 0x757].toInt() and 1
    }

    private fun level1Create(bitmask: Int): Node {
        return createTree(
            if (bitmask and 1 != 0) trueLeaf else falseLeaf,
            if (bitmask and 2 != 0) trueLeaf else falseLeaf,
            if (bitmask and 4 != 0) trueLeaf else falseLeaf,
            if (bitmask and 8 != 0) trueLeaf else falseLeaf
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

    private fun emptyTree(level: Int): Node? {
        if (emptyTreeCache[level] != null) {
            return emptyTreeCache[level]
        }
        val t: Node? = if (level == 1) {
            falseLeaf
        } else {
            emptyTree(level - 1)
        }
        emptyTreeCache[level] = createTree(t, t, t, t)
        return emptyTreeCache[level]
    }

    /*
     * In the expandUniverse method, a new root node will be created at one level
     * higher than the current root node
     * surrounded by empty space, essentially making the universe double sized (on
     * either dimension)
     *
     * a new subtree is created for each quadrant. Quadrants have a level one below
     * the passed in node but createTree will combine that with the empty trees and
     * create nodes at the same
     * level as the input node.
     *
     * then all 4 will be combined into a new, larger universe by the outside
     * createTree which will return a new
     * node with the prior quadrants moved towards the center of the new tree -
     * creating space at the edges for
     * additional growth
     *
     * Here's the process of creating the new root node with increased level:
     * Create an empty tree t with level node.level - 1
     * Create new trees for each quadrant, with the original node's
     * quadrants shifted to the corner and empty space added to the other corners.
     * Combine these new trees using the createTree method.
     *
     * So, the new root node will have a level one greater than the original root node,
     * as it combines the new trees created with an extra level of hierarchy.
     */
    private fun expandUniverse(node: Node?): Node {
        // System.out.println("expanding universe");
        val t = emptyTree(node!!.level - 1)
        return createTree(
            createTree(t, t, t, node.nw),
            createTree(t, t, node.ne, t),
            createTree(t, node.sw, t, t),
            createTree(node.se, t, t, t)
        )
    }

    // Preserve the tree, but remove all cached
    // generations forward
    // alsoQuick came from the original algorithm and was used when
    // (I believe) that the rule set was changed to allow for different birth/survival rules
    // right now i don't know why it's otherwise okay to preserve
    // need to think more about this
    private fun uncache(/*alsoQuick: Boolean*/) {
        hashmap.values.parallelStream().forEach { node: Node? ->
            if (node != null) {
                node.cache = null
                node.hashmapNext = null
/*                if (alsoQuick) {
                    node.quickCache = null
                }*/
            }
        }
    }

    // return false if not in the hash map
    // which means it could be in the linked list associated with the hashmap
    private fun inHashmap(n: Node?): Boolean {
        val hash = calcHash(n!!.nw!!.id, n.ne!!.id, n.sw!!.id, n.se!!.id) and hashmapSize
        var node = hashmap[hash]
        while (node != null) {
            if (node == n) {
                return true
            }
            node = node.hashmapNext
        }
        return false
    }

    /*
     * The nodeHash method is called during garbage collection, and its main purpose
     * is to ensure
     * that all nodes in the given tree are present in the hashmap. It recursively
     * traverses the
     * tree and calls hashmapInsert to insert each node into the hashmap if it's not
     * already there.
     * 
     * The inHashmap method checks if the node is already in the hashmap (including
     * the linked
     * list of nodes in case of hash collisions). If a node is already in the
     * hashmap, the
     * nodeHash method won't insert it again.
     * 
     * The hashmapInsert method is responsible for inserting a given node n into the
     * hashmap.
     * It first calculates the hash for the node and finds the corresponding entry
     * in the hashmap.
     * If there are any hash collisions, it follows the linked list (using the
     * hashmapNext field)
     * to the last node in the list and inserts the new node there.
     * 
     * So, in summary, the nodeHash method is responsible for ensuring that all
     * nodes in a given
     * tree are present in the hashmap during garbage collection. It does not
     * repopulate a cleared
     * hashmap with a tree, but rather ensures that the existing nodes are correctly
     * inserted
     * into the hashmap. The linked list resulting from hash collisions is
     * maintained by the hashmapInsert method.
     */
    // called only on garbageCollect
    private fun nodeHash(node: Node?) {
        if (!inHashmap(node)) {
            // Update the id. We have looked for an old id, as
            // the hashmap has been cleared and ids have been
            // reset, but this cannot be avoided without iterating
            // the tree twice.
            node!!.id = lastId++
            node.hashmapNext = null
            if (node.level > 1) {
                nodeHash(node.nw)
                nodeHash(node.ne)
                nodeHash(node.sw)
                nodeHash(node.se)
                if (node.cache != null) {
                    nodeHash(node.cache)
                }
                if (node.quickCache != null) {
                    nodeHash(node.quickCache)
                }
            }
            hashmapInsert(node)
        }
    }

    // insert a node into the hashmap
    private fun hashmapInsert(n: Node?) {
        val hash = calcHash(n!!.nw!!.id, n.ne!!.id, n.sw!!.id, n.se!!.id) and hashmapSize
        var node = hashmap[hash]
        var prev: Node? = null
        while (node != null) {
            prev = node
            node = node.hashmapNext
        }
        if (prev != null) {
            prev.hashmapNext = n
        } else {
            hashmap[hash] = n
        }
    }

    private fun garbageCollect() {
        // if rewinding just keep things as they are
        if (hashmapSize < (1 shl HASHMAP_LIMIT) - 1) {
            hashmapSize = hashmapSize shl 1 or 1
            hashmap = HashMap(hashmapSize + 1)
        }
        maxLoad = (hashmapSize * LOAD_FACTOR).toInt()
        hashmap.clear()
        lastId = 4
        nodeHash(root)
    }

    /*
     * // the hash function used for the hashmap
     * int calcHash(int nw_id, int ne_id, int sw_id, int se_id) {
     * return ((nw_id * 23 ^ ne_id) * 23 ^ sw_id) * 23 ^ se_id;
     * }
     */
    private fun calcHash(nwId: Int, neId: Int, swId: Int, seId: Int): Int {
        var result = 17
        result = 31 * result xor nwId
        result = 31 * result xor neId
        result = 31 * result xor swId
        result = 31 * result xor seId
        return result
    }

    // this is just used when setting up the field initially unless I'm missing
    // something
    private fun getBounds(fieldX: IntBuffer, fieldY: IntBuffer): Bounds {
        if (fieldX.capacity() == 0) {
            return Bounds(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
        }

        // this sets up a bounds that just has an initial top and bottom that are the
        // same and left and right the same
        // the RLEBuffer creates x and y to be the same size regardless of source width
        // and height - it will make it
        // the size of the bounding box
        val bounds = Bounds(
            BigInteger.valueOf(fieldY[0].toLong()), BigInteger.valueOf(
                fieldX[0].toLong()
            )
        )
        val len = fieldX.capacity()

        // todo: pass in varied width and heights and look at the field size they
        // actually generate
        for (i in 1 until len) {
            val x = BigInteger.valueOf(fieldX[i].toLong())
            val y = BigInteger.valueOf(fieldY[i].toLong())
            if (x < bounds.left) {
                bounds.left = x
            } else if (x > bounds.right) {
                bounds.right = x
            }
            if (y < bounds.top) {
                bounds.top = y
            } else if (y > bounds.bottom) {
                bounds.bottom = y
            }
        }
        return bounds
    }

    private fun getLevelFromBounds(bounds: Bounds): Int {
        var max = 4
        val keys = arrayOf("top", "left", "bottom", "right")
        for (key in keys) {
            var coordinate = BigInteger.ZERO
            when (key) {
                "top" -> coordinate = bounds.top
                "left" -> coordinate = bounds.left
                "bottom" -> coordinate = bounds.bottom
                "right" -> coordinate = bounds.right
                else -> {}
            }
            if (coordinate.add(BigInteger.ONE) > BigInteger.valueOf(max.toLong())) {
                max = coordinate.add(BigInteger.ONE).toInt()
            } else if (coordinate.negate() > BigInteger.valueOf(max.toLong())) {
                max = coordinate.negate().toInt()
            }
        }
        return ceil(ln(max.toDouble()) / ln(2.0)).toInt() + 1
    }

    fun setupField(fieldX: IntBuffer, fieldY: IntBuffer) {
        val bounds = getBounds(fieldX, fieldY)
        val level = getLevelFromBounds(bounds)
        val offset = BigInteger.valueOf(2).pow(level - 1)
        val count = fieldX.capacity()
        moveField(fieldX, fieldY, offset.toInt(), offset.toInt())
        root = setupFieldRecurse(0, count - 1, fieldX, fieldY, level)
        updatePatternInfo()
    }

    private fun partition(start: Int, end: Int, testField: IntBuffer, otherField: IntBuffer, offset: Int): Int {
        var i = start
        var j = end
        var swap: Int
        while (i <= j) {
            while (i <= end && testField[i] and offset == 0) {
                i++
            }
            while (j > start && testField[j] and offset != 0) {
                j--
            }
            if (i >= j) {
                break
            }
            swap = testField[i]
            testField.put(i, testField[j])
            testField.put(j, swap)
            swap = otherField[i]
            otherField.put(i, otherField[j])
            otherField.put(j, swap)
            i++
            j--
        }
        return i
    }

    private fun setupFieldRecurse(start: Int, end: Int, fieldX: IntBuffer, fieldY: IntBuffer, recurseLevel: Int): Node? {
        var level = recurseLevel
        if (start > end) {
            return emptyTree(level)
        }
        if (level == 2) {
            return level2Setup(start, end, fieldX, fieldY)
        }
        level--
        val offset = 1 shl level
        val part3 = partition(start, end, fieldY, fieldX, offset)
        val part2 = partition(start, part3 - 1, fieldX, fieldY, offset)
        val part4 = partition(part3, end, fieldX, fieldY, offset)
        return createTree(
            setupFieldRecurse(start, part2 - 1, fieldX, fieldY, level),
            setupFieldRecurse(part2, part3 - 1, fieldX, fieldY, level),
            setupFieldRecurse(part3, part4 - 1, fieldX, fieldY, level),
            setupFieldRecurse(part4, end, fieldX, fieldY, level)
        )
    }

    private fun level2Setup(start: Int, end: Int, fieldX: IntBuffer, fieldY: IntBuffer): Node? {
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
        val tree = createTree(
            level1Create(set),
            level1Create(set shr 4),
            level1Create(set shr 8),
            level1Create(set shr 12)
        )
        level2Cache[set] = tree
        return tree
    }

    /*
     * public void setRules(int s, int b) {
     * if (this.rule_s != s || this.rule_b != b) {
     * this.rule_s = s;
     * this.rule_b = b;
     * 
     * this.uncache(true);
     * emptyTreeCache = new patterning.Node[UNIVERSE_SIZE_LIMIT];
     * level2Cache = new HashMap<>(0x10000);
     * }
     * }
     */
    /*
     * private patterning.Node nodeSetBit(patterning.Node node, BigInteger x, BigInteger y, boolean
     * living) {
     * 
     * if (node.level == 0) {
     * return living ? trueLeaf : falseLeaf;
     * }
     * 
     * BigInteger offset = node.level == 1 ? BigInteger.ZERO : pow2(node.level - 2);
     * 
     * patterning.Node nw = node.nw, ne = node.ne, sw = node.sw, se = node.se;
     * 
     * if (x.compareTo(BigInteger.ZERO) < 0) {
     * if (y.compareTo(BigInteger.ZERO) < 0) {
     * nw = nodeSetBit(nw, x.add(offset), y.add(offset), living);
     * } else {
     * sw = nodeSetBit(sw, x.add(offset), y.subtract(offset), living);
     * }
     * } else {
     * if (y.compareTo(BigInteger.ZERO) < 0) {
     * ne = nodeSetBit(ne, x.subtract(offset), y.add(offset), living);
     * } else {
     * se = nodeSetBit(se, x.subtract(offset), y.subtract(offset), living);
     * }
     * }
     * 
     * return createTree(nw, ne, sw, se);
     * }
     * 
     * private boolean nodeGetBit(patterning.Node node, BigInteger x, BigInteger y) {
     * if (node.population.equals(BigInteger.ZERO)) {
     * return false;
     * }
     * 
     * if (node.level == 0) {
     * // other level 0 case is handled above
     * return true;
     * }
     * 
     * BigInteger offset = node.level == 1 ? BigInteger.ZERO : pow2(node.level - 2);
     * 
     * if (x.compareTo(BigInteger.ZERO) < 0) {
     * if (y.compareTo(BigInteger.ZERO) < 0) {
     * return nodeGetBit(node.nw, x.add(offset), y.add(offset));
     * } else {
     * return nodeGetBit(node.sw, x.add(offset), y.subtract(offset));
     * }
     * } else {
     * if (y.compareTo(BigInteger.ZERO) < 0) {
     * return nodeGetBit(node.ne, x.subtract(offset), y.add(offset));
     * } else {
     * return nodeGetBit(node.se, x.subtract(offset), y.subtract(offset));
     * }
     * }
     * }
     */
    private fun nodeLevel2Next(node: Node?): Node {
        val nw = node!!.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se
        val bitmask = nw!!.nw!!.population.shiftLeft(15).or(nw.ne!!.population.shiftLeft(14))
            .or(ne!!.nw!!.population.shiftLeft(13)).or(ne.ne!!.population.shiftLeft(12))
            .or(nw.sw!!.population.shiftLeft(11)).or(nw.se!!.population.shiftLeft(10))
            .or(ne.sw!!.population.shiftLeft(9))
            .or(ne.se!!.population.shiftLeft(8))
            .or(sw!!.nw!!.population.shiftLeft(7)).or(sw.ne!!.population.shiftLeft(6))
            .or(se!!.nw!!.population.shiftLeft(5))
            .or(se.ne!!.population.shiftLeft(4))
            .or(sw.sw!!.population.shiftLeft(3)).or(sw.se!!.population.shiftLeft(2)).or(se.sw!!.population.shiftLeft(1))
            .or(se.se!!.population)
        val result = evalMask(bitmask.shiftRight(5).toInt()) or (
                evalMask(bitmask.shiftRight(4).toInt()) shl 1) or (
                evalMask(bitmask.shiftRight(1).toInt()) shl 2) or (
                evalMask(bitmask.toInt()) shl 3)
        return level1Create(result)
    }

    // create or search for a tree node given its children
    private fun createTree(nw: Node?, ne: Node?, sw: Node?, se: Node?): Node {
        val hash = calcHash(nw!!.id, ne!!.id, sw!!.id, se!!.id) and hashmapSize
        var node = hashmap[hash]
        var prev: Node? = null
        while (node != null) {
            if (node.nw == nw && node.ne == ne && node.sw == sw && node.se == se) {
                return node
            }
            prev = node
            node = node.hashmapNext
        }
        if (lastId > maxLoad) {
            garbageCollect()
            // garbageCollect new maxLoad:{String.format("%,d", maxLoad)} - next thing up is
            // returning a new tree because createTree was about to just make a new one, but
            // now it's calling itself recursively rather than just making a new node and
            // returning it
            return createTree(nw, ne, sw, se)
        }
        val newNode = Node(nw, ne, sw, se, lastId++, step)
        if (prev != null) {
            prev.hashmapNext = newNode
        } else {
            hashmap[hash] = newNode
        }
        return newNode
    }

    fun nextGeneration() {
        var root = this.root

        /* // the following is a new mechanism to only expandUniverse as far as you need to
        // and not just merely to match up to the current step size
        // this seems to work but if it ever doesn't this might be the culprit
        // you  have to go back to
        // this as the first line of the while below:
        // while ((root.level <= this.step + 2) ||

        // Get the current patterning.Bounds object for live cells in root
        patterning.Bounds bounds = getRootBounds();

        // Calculate the maximum dimension of the live cells' bounding box
        BigInteger width = bounds.right.subtract(bounds.left);
        BigInteger height = bounds.bottom.subtract(bounds.top);
        BigInteger maxDimension = width.max(height);

        // Calculate the required universe size based on the maximum dimension
        int requiredLevel = root.level;
        BigInteger size = pow2(root.level + 1);
        while (size.compareTo(maxDimension) < 0) {
            requiredLevel++;
            size = pow2(requiredLevel + 1);
        }*

        // when we're not large enough to fit the bounds, or the tree needs to be
        // repositioned towards the center, then expand universe until that's true
        while (root.level < requiredLevel ||/
        */


        /* 
         * so the above didn't work - chatGPT suggested the following:
         * Your commented-out proposed optimization seems to be trying to calculate
         *  the minimum required level of the root node based on the size of the bounding 
         * box of the live cells. This could be a good optimization if the size of the live
         *  area is typically much smaller than the size of the entire universe. However, 
         * it's possible that this broke your algorithm because the level of the root node 
         * ended up being too small for the number of generations you're trying to advance at once.

            To create a test for this issue, you could create a pattern that grows
             significantly over many generations, such as a glider gun in the Game of Life.
              Then, you could create a test that steps this pattern forward by a large number
               of generations and checks the resulting state against a known correct result.
                This could help you figure out if the issue is with the size of the root node or with recentering the universe.
         */while (root!!.level <= step + 2 ||
            root.nw!!.population != root.nw!!.se!!.se!!.population ||
            root.ne!!.population != root.ne!!.sw!!.sw!!.population ||
            root.sw!!.population != root.sw!!.ne!!.ne!!.population ||
            root.se!!.population != root.se!!.nw!!.nw!!.population
        ) {
            root = expandUniverse(root)
        }
        // patterning.Node maintains a per generation set of nodes that were newly created
        // clear them so the drawing routine can use unchanged nodes as a key to a cache
        // associated with their
        // drawing
        clearChanged()
        this.root = nodeNextGeneration(root)
        val generationIncrease = pow2(step) // BigInteger.valueOf(2).pow(this.step);
        generation = generation.add(generationIncrease)
        updatePatternInfo()
    }

    private fun updatePatternInfo() {
        patternInfo.addOrUpdate("level", root!!.level)
        patternInfo.addOrUpdate("step", pow2(step)!!)
        patternInfo.addOrUpdate("generation", generation)
        patternInfo.addOrUpdate("population", root!!.population)
        patternInfo.addOrUpdate("maxLoad", maxLoad)
        patternInfo.addOrUpdate("lastId", lastId)
        val bounds = rootBounds
        patternInfo.addOrUpdate("width", bounds.right.subtract(bounds.left).add(BigInteger.ONE))
        patternInfo.addOrUpdate("height", bounds.bottom.subtract(bounds.top).add(BigInteger.ONE))

    }

    private fun nodeNextGeneration(node: Node?): Node? {
        if (node!!.cache != null) {
            return node.cache
        }
        if (step == node.level - 2) {
            quickgen = 0
            // System.out.println("root.level: " + root.level + " step: " + this.step + "
            // node.level: " + node.level);
            return nodeQuickNextGeneration(node, 0)
        }

        // right now i have seen a nodeNextGeneration where
        // node.level == 2 so...
        // maybe this doesn't need to exist?
        // todo: see what happens when you have a blank canvas and use setbit...
        if (node.level == 2) {
            if (node.quickCache == null) {
                node.quickCache = nodeLevel2Next(node)
            }
            return node.quickCache
        }
        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se
        val n00 = createTree(nw!!.nw!!.se, nw.ne!!.sw, nw.sw!!.ne, nw.se!!.nw)
        val n01 = createTree(nw.ne!!.se, ne!!.nw!!.sw, nw.se!!.ne, ne.sw!!.nw)
        val n02 = createTree(ne.nw!!.se, ne.ne!!.sw, ne.sw!!.ne, ne.se!!.nw)
        val n10 = createTree(nw.sw!!.se, nw.se!!.sw, sw!!.nw!!.ne, sw.ne!!.nw)
        val n11 = createTree(nw.se!!.se, ne.sw!!.sw, sw.ne!!.ne, se!!.nw!!.nw)
        val n12 = createTree(ne.sw!!.se, ne.se!!.sw, se.nw!!.ne, se.ne!!.nw)
        val n20 = createTree(sw.nw!!.se, sw.ne!!.sw, sw.sw!!.ne, sw.se!!.nw)
        val n21 = createTree(sw.ne!!.se, se.nw!!.sw, sw.se!!.ne, se.sw!!.nw)
        val n22 = createTree(se.nw!!.se, se.ne!!.sw, se.sw!!.ne, se.se!!.nw)
        val newNW = nodeNextGeneration(createTree(n00, n01, n10, n11))
        val newNE = nodeNextGeneration(createTree(n01, n02, n11, n12))
        val newSW = nodeNextGeneration(createTree(n10, n11, n20, n21))
        val newSE = nodeNextGeneration(createTree(n11, n12, n21, n22))
        val result = createTree(newNW, newNE, newSW, newSE)

        // cascade it up the tree
        addChanged(node)
        // mark the new one changed
        addChanged(result)
        node.cache = result
        return result
    }

    private var quickgen: Long = 0

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
            _bitcounts[i] = (_bitcounts[i and 0xF] + _bitcounts[i shr 4 and 0xF]
                    + _bitcounts[i shr 8]).toByte()
        }

        // current rule setting
        ruleB = 1 shl 3
        rulesS = 1 shl 2 or (1 shl 3)
        this.root = null

        // number of generations to calculate at one time, written as 2^n
        step = 0

        // in which generation are we
        generation = BigInteger.ZERO
        falseLeaf = Node(3, BigInteger.ZERO, 0)
        trueLeaf = Node(2, BigInteger.ONE, 0)

        // the final necessary setup bits

        // last id for nodes
        lastId = 4

        // Size of the hashmap - Always a power of 2 minus 1
        hashmapSize = (1 shl INITIAL_SIZE) - 1

        // Size when the next GC will happen
        maxLoad = (hashmapSize * LOAD_FACTOR).toInt()
        hashmap = HashMap()
        emptyTreeCache = arrayOfNulls(UNIVERSE_LEVEL_LIMIT)
        level2Cache = HashMap(0x10000)
        this.root = emptyTree(3)
        generation = BigInteger.ZERO
        step = 0
    }

    @Throws(IllegalStateException::class)
    fun nodeQuickNextGeneration(node: Node?, recurseDepth: Int): Node? {
        var depth = recurseDepth
        quickgen += 1
        if (node!!.quickCache != null) {
            return node.quickCache
        }
        if (node.level == 2) {
            return nodeLevel2Next(node).also { node.quickCache = it }
        }
        val nw = node.nw
        val ne = node.ne
        val sw = node.sw
        val se = node.se
        depth += 1
        val n00 = nodeQuickNextGeneration(nw, depth)
        val n01 = nodeQuickNextGeneration(
            createTree(nw!!.ne, ne!!.nw, nw.se, ne.sw),
            depth
        )
        val n02 = nodeQuickNextGeneration(ne, depth)
        val n10 = nodeQuickNextGeneration(
            createTree(nw.sw, nw.se, sw!!.nw, sw.ne),
            depth
        )
        val n11 = nodeQuickNextGeneration(
            createTree(nw.se, ne.sw, sw.ne, se!!.nw),
            depth
        )
        val n12 = nodeQuickNextGeneration(
            createTree(ne.sw, ne.se, se.nw, se.ne),
            depth
        )
        val n20 = nodeQuickNextGeneration(sw, depth)
        val n21 = nodeQuickNextGeneration(
            createTree(sw.ne, se.nw, sw.se, se.sw),
            depth
        )
        val n22 = nodeQuickNextGeneration(se, depth)
        return createTree(
            nodeQuickNextGeneration(createTree(n00, n01, n10, n11), depth),
            nodeQuickNextGeneration(createTree(n01, n02, n11, n12), depth),
            nodeQuickNextGeneration(createTree(n10, n11, n20, n21), depth),
            nodeQuickNextGeneration(createTree(n11, n12, n21, n22), depth)
        ).also { node.quickCache = it }
    }

    val rootBounds: Bounds
        get() {
            if (root!!.population == BigInteger.ZERO) {
                return Bounds(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
            }

            // BigInteger offset = BigInteger.valueOf(2).pow(root.level - 1);
            val offset = pow2(root!!.level - 1)
            val bounds = Bounds(offset!!, offset, offset.negate(), offset.negate())
            nodeGetBoundary(
                root, offset.negate(), offset.negate(),
                MASK_TOP or MASK_LEFT or MASK_BOTTOM or MASK_RIGHT, bounds
            )
            return bounds
        }

    private fun nodeGetBoundary(node: Node?, left: BigInteger, top: BigInteger, findMask: Int, boundary: Bounds) {
        if (node!!.population == BigInteger.ZERO || findMask == 0) {
            return
        }
        if (node.level == 0) {
            boundary.left = boundary.left.min(left)
            boundary.right = boundary.right.max(left)
            boundary.top = boundary.top.min(top)
            boundary.bottom = boundary.bottom.max(top)
        } else {
            val offset = pow2(node.level - 1)
            val doubledOffset = pow2(node.level)
            if (left >= boundary.left &&
                left.add(doubledOffset) <= boundary.right &&
                top >= boundary.top &&
                top.add(doubledOffset) <= boundary.bottom
            ) {
                // This square is already inside the found boundary
                return
            }
            var findNW = findMask
            var findSW = findMask
            var findNE = findMask
            var findSE = findMask
            if (node.nw!!.population != BigInteger.ZERO) {
                findSW = findSW and MASK_TOP.inv()
                findNE = findNE and MASK_LEFT.inv()
                findSE = findSE and (MASK_TOP or MASK_LEFT).inv()
            }
            if (node.sw!!.population != BigInteger.ZERO) {
                findSE = findSE and MASK_LEFT.inv()
                findNW = findNW and MASK_BOTTOM.inv()
                findNE = findNE and (MASK_BOTTOM or MASK_LEFT).inv()
            }
            if (node.ne!!.population != BigInteger.ZERO) {
                findNW = findNW and MASK_RIGHT.inv()
                findSE = findSE and MASK_TOP.inv()
                findSW = findSW and (MASK_TOP or MASK_RIGHT).inv()
            }
            if (node.se!!.population != BigInteger.ZERO) {
                findSW = findSW and MASK_RIGHT.inv()
                findNE = findNE and MASK_BOTTOM.inv()
                findNW = findNW and (MASK_BOTTOM or MASK_RIGHT).inv()
            }
            nodeGetBoundary(node.nw, left, top, findNW, boundary)
            nodeGetBoundary(node.sw, left, top.add(offset), findSW, boundary)
            nodeGetBoundary(node.ne, left.add(offset), top, findNE, boundary)
            nodeGetBoundary(node.se, left.add(offset), top.add(offset), findSE, boundary)
        }
    }

    companion object {
        private const val LOAD_FACTOR = 0.95
        private const val INITIAL_SIZE = 26

        // this is extremely large but can be maybe reached if you fix problems with nodeQuickNextGeneration
        // going to try a maximum universe size of 1024 with a new drawing scheme that relies
        // on calculating relating a viewport to the level being drawn and relies on the max double value
        // the largest level this can support would be  (from Wolfram)  "input": "Floor[Log2[1.8*10^308]]" "output": "1024"
        private const val UNIVERSE_LEVEL_LIMIT = 1024
        private const val HASHMAP_LIMIT = 30
        private const val MASK_LEFT = 1
        private const val MASK_TOP = 2
        private const val MASK_RIGHT = 4
        private const val MASK_BOTTOM = 8
        private val _powers = arrayOfNulls<BigInteger>(UNIVERSE_LEVEL_LIMIT)

        init { // the size of the MathContext
            _powers[0] = BigInteger.ONE
            for (i in 1 until UNIVERSE_LEVEL_LIMIT) {
                _powers[i] = _powers[i - 1]!!.multiply(BigInteger.TWO)
            }
        }

        fun pow2(x: Int): BigInteger? {
            return if (x >= UNIVERSE_LEVEL_LIMIT) {
                BigInteger.valueOf(2).pow(UNIVERSE_LEVEL_LIMIT)
            } else _powers[x]
        }
    }
}