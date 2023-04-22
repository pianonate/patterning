import java.math.BigInteger;
import java.nio.IntBuffer;
import java.util.HashMap;

// todo - dynamically allocate the empty tree cache size just like the hashmap
//        if it reaches a size then increase it by a load factor and copy it to the new array

public class LifeUniverse {
    private static final double LOAD_FACTOR = 0.9;
    private static final int INITIAL_SIZE = 16;

    private static final int EMPTY_TREE_CACHE_SIZE = 1024;
    private static final int HASHMAP_LIMIT = 24;
    private static final int MASK_LEFT = 1;
    private static final int MASK_TOP = 2;
    private static final int MASK_RIGHT = 4;
    private static final int MASK_BOTTOM = 8;

    public int lastId;
    private int hashmapSize;
    public int maxLoad;
    private HashMap<Integer, Node> hashmap;
    private Node[] emptyTreeCache;
    private HashMap<Integer, Node> level2Cache;
    private final double[] _powers;
    @SuppressWarnings("FieldMayBeFinal")
    private byte[] _bitcounts;
    private int rule_b;
    private int rule_s;
    public Node root;
    private Node rewind_state;
    public int step;
    public BigInteger generation;

    private final Node falseLeaf;
    private final Node trueLeaf;

    LifeUniverse() {
        // last id for nodes
        this.lastId = 0;

        // Size of the hashmap.
        // Always a power of 2 minus 1
        this.hashmapSize = 0;

        // Size when the next GC will happen
        this.maxLoad = 0;

        // the hashmap
        this.hashmap = new HashMap<>();
        this.emptyTreeCache = new Node[EMPTY_TREE_CACHE_SIZE];
        this.level2Cache = new HashMap<>();

        this._powers = new double[1024];
        this._powers[0] = 1;

        for (int i = 1; i < 1024; i++) {
            this._powers[i] = this._powers[i - 1] * 2;
        }

        this._bitcounts = new byte[0x758];
        this._bitcounts[0] = 0;
        this._bitcounts[1] = 1;
        this._bitcounts[2] = 1;
        this._bitcounts[3] = 2;
        this._bitcounts[4] = 1;
        this._bitcounts[5] = 2;
        this._bitcounts[6] = 2;
        this._bitcounts[7] = 3;
        this._bitcounts[8] = 1;
        this._bitcounts[9] = 2;
        this._bitcounts[10] = 2;
        this._bitcounts[11] = 3;
        this._bitcounts[12] = 2;
        this._bitcounts[13] = 3;
        this._bitcounts[14] = 3;
        this._bitcounts[15] = 4;

        for (int i = 0x10; i < 0x758; i++) {
            this._bitcounts[i] = (byte) (this._bitcounts[i & 0xF] + this._bitcounts[i >> 4 & 0xF] + this._bitcounts[i >> 8]);
        }

        // current rule setting
        this.rule_b = 1 << 3;
        this.rule_s = 1 << 2 | 1 << 3;

        this.root = null;

        this.rewind_state = null;

        // number of generations to calculate at one time, written as 2^n
        this.step = 0;

        // in which generation are we
        this.generation = BigInteger.ZERO;

        this.falseLeaf = new Node(3, BigInteger.ZERO, 0);
        this.trueLeaf = new Node(2, BigInteger.ONE, 0);

        // the final necessary setup bits
        clearPattern();
    }

    // return the cached power of 2 - for performance reasons
    private double pow2(int x) {
        if (x >= 1024) {
            return Double.POSITIVE_INFINITY;
        }
        return this._powers[x];
    }


    // only called from main game
    public void saveRewindState() {
        rewind_state = root;
    }

    // only called from main game
    public void restoreRewindState() {
        generation = BigInteger.ZERO;
        root = rewind_state;

        // make sure to rebuild the hashmap, in case its size changed
        garbageCollect();
    }

    /* Note that Java doesn't have a bitwise logical shift operator
     that preserves the sign bit like JavaScript (>>),
     so we can use the unsigned right shift operator (>>>) instead.
     However, since the _bitcounts array contains only positive values,
     we can use the regular right shift operator (>>) instead without any issues.*/
    public int evalMask(int bitmask) {
        int rule = ((bitmask & 32) != 0) ? this.rule_s : this.rule_b;
        return (rule >> this._bitcounts[bitmask & 0x757]) & 1;
    }

    private Node level1Create(int bitmask) {
        return createTree(
                (bitmask & 1) != 0 ? trueLeaf : falseLeaf,
                (bitmask & 2) != 0 ? trueLeaf : falseLeaf,
                (bitmask & 4) != 0 ? trueLeaf : falseLeaf,
                (bitmask & 8) != 0 ? trueLeaf : falseLeaf
        );
    }

    private void setBit(int x, int y, boolean living) {
        int level = getLevelFromBounds(new Bounds(x, y, x, y));

        if (living) {
            while (level > root.level) {
                root = expandUniverse(root);
            }
        } else {
            if (level > root.level) {
                // no need to delete pixels outside the universe
                return;
            }
        }

        root = nodeSetBit(root, x, y, living);
    }

    public boolean getBit(int x, int y) {
        //noinspection SuspiciousNameCombination
        int level = getLevelFromBounds(new Bounds(x, y, x, y));

        if (level > root.level) {
            return false;
        } else {
            return nodeGetBit(root, x, y);
        }
    }

    public Bounds getRootBounds() {
        if (root.population.equals(BigInteger.ZERO)) {
            return new Bounds(0, 0, 0, 0);
        }

        Bounds bounds = new Bounds(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        double offset = pow2(root.level - 1);

        nodeGetBoundary(root, (int) -offset, (int) -offset, MASK_TOP | MASK_LEFT | MASK_BOTTOM | MASK_RIGHT, bounds);

        return bounds;
    }

    // will always be called with integer boundaries
    public void makeCenter(IntBuffer fieldX, IntBuffer fieldY, Bounds bounds) {
        int offsetX = Math.round((float) (bounds.left - bounds.right) / 2) - bounds.left;
        int offsetY = Math.round((float) (bounds.top - bounds.bottom) / 2) - bounds.top;

        moveField(fieldX, fieldY, offsetX, offsetY);

        bounds.left += offsetX;
        bounds.right += offsetX;
        bounds.top += offsetY;
        bounds.bottom += offsetY;
    }


    public void moveField(IntBuffer fieldX, IntBuffer fieldY, int offsetX, int offsetY) {
        for (int i = 0; i < fieldX.capacity(); i++) {
            int x = fieldX.get(i);
            int y = fieldY.get(i);

            fieldX.put(i, x + offsetX);
            fieldY.put(i, y + offsetY);
        }
    }


    private Node emptyTree(int level) {
        if (emptyTreeCache[level] != null) {
            return emptyTreeCache[level];
        }

        Node t;

        if (level == 1) {
            t = falseLeaf;
        } else {
            t = emptyTree(level - 1);
        }

        emptyTreeCache[level] = createTree(t, t, t, t);

        return emptyTreeCache[level];
    }


    private Node expandUniverse(Node node) {
        // System.out.println("expanding universe");

        Node t = emptyTree(node.level - 1);

        return createTree(
                createTree(t, t, t, node.nw),
                createTree(t, t, node.ne, t),
                createTree(t, node.sw, t, t),
                createTree(node.se, t, t, t)
        );
    }

    // Preserve the tree, but remove all cached
    // generations forward
    private void uncache(boolean alsoQuick) {
        for (Node node : hashmap.values()) {
            if (node != null) {
                node.cache = null;
                node.hashmapNext = null;
                if (alsoQuick) {
                    node.quick_cache = null;
                }
            }
        }
    }

    // return false if not in the hash map
    public boolean inHashmap(Node n) {
        int hash = calcHash(n.nw.id, n.ne.id, n.sw.id, n.se.id) & hashmapSize;
        Node node = hashmap.get(hash);

        while (node != null) {
            if (node.equals(n)) {
                return true;
            }
            node = node.hashmapNext;
        }

        return false;
    }

    // insert a node into the hashmap
    public void hashmapInsert(Node n) {
        int hash = calcHash(n.nw.id, n.ne.id, n.sw.id, n.se.id) & hashmapSize;
        Node node = hashmap.get(hash);
        Node prev = null;

        while (node != null) {
            prev = node;
            node = node.hashmapNext;
        }

        if (prev != null) {
            prev.hashmapNext = n;
        } else {
            hashmap.put(hash, n);
        }
    }


    // create or search for a tree node given its children
    private Node createTree(Node nw, Node ne, Node sw, Node se) {

        int hash = calcHash(nw.id, ne.id, sw.id, se.id) & hashmapSize;
        Node node = hashmap.get(hash);
        Node prev = null;

        while (node != null) {
            if (node.nw == nw && node.ne == ne && node.sw == sw && node.se == se) {
                return node;
            }
            prev = node;
            node = node.hashmapNext;
        }

        if (lastId > maxLoad) {
            garbageCollect();
            return createTree(nw, ne, sw, se);
        }

        Node newNode = new Node(nw, ne, sw, se, lastId++);
        if (prev != null) {
            prev.hashmapNext = newNode;
        } else {
            hashmap.put(hash, newNode);
        }

        return newNode;
    }

    public void nextGeneration(boolean isSingle) {
        Node root = this.root;

        while ((isSingle && root.level <= this.step + 2) ||
                !root.nw.population.equals(root.nw.se.se.population) ||
                !root.ne.population.equals(root.ne.sw.sw.population) ||
                !root.sw.population.equals(root.sw.ne.ne.population) ||
                !root.se.population.equals(root.se.nw.nw.population)) {
            root = this.expandUniverse(root);
        }

        if (isSingle) {
            this.generation = this.generation.add(BigInteger.valueOf((long) pow2(this.step)));
            root = nodeNextGeneration(root);
        } else {
            this.generation = this.generation.add(BigInteger.valueOf((long) pow2(this.root.level - 2)));
            root = nodeQuickNextGeneration(root);
        }

        this.root = root;
    }

    public void garbageCollect() {
        long start = System.currentTimeMillis();

        if (hashmapSize < (1 << HASHMAP_LIMIT) - 1) {
            hashmapSize = (hashmapSize << 1) | 1;
            hashmap = new HashMap<>(hashmapSize + 1);
        }

        maxLoad = (int) (hashmapSize * LOAD_FACTOR);

        for (int i = 0; i <= hashmapSize; i++) {
            this.hashmap.put(i, null);
        }

        lastId = 4;
        nodeHash(root);

        long end = System.currentTimeMillis();
        System.out.println("gc millis: " + (end - start));
    }

    // the hash function used for the hashmap
    int calcHash(int nw_id, int ne_id, int sw_id, int se_id) {
        int hash = ((nw_id * 23 ^ ne_id) * 23 ^ sw_id) * 23 ^ se_id;
        return hash;
    }

    public void clearPattern() {
        this.lastId = 4;
        this.hashmapSize = (1 << INITIAL_SIZE) - 1;
        this.maxLoad = (int) (this.hashmapSize * LOAD_FACTOR);
        this.hashmap = new HashMap<>();
        this.emptyTreeCache = new Node[EMPTY_TREE_CACHE_SIZE];
        this.level2Cache = new HashMap<>(0x10000);

        for (int i = 0; i <= this.hashmapSize; i++)
            this.hashmap.put(i, null);

        this.root = this.emptyTree(3);
        this.generation = BigInteger.ZERO;
    }

    public Bounds getBounds(IntBuffer fieldX, IntBuffer fieldY) {
        if (fieldX.capacity() == 0) {
            return new Bounds(0, 0, 0, 0);
        }

        Bounds bounds = new Bounds(fieldY.get(0), fieldX.get(0), fieldY.get(0), fieldX.get(0));
        int len = fieldX.capacity();

        for (int i = 1; i < len; i++) {
            int x = fieldX.get(i);
            int y = fieldY.get(i);

            if (x < bounds.left) {
                bounds.left = x;
            } else if (x > bounds.right) {
                bounds.right = x;
            }

            if (y < bounds.top) {
                bounds.top = y;
            } else if (y > bounds.bottom) {
                bounds.bottom = y;
            }
        }

        return bounds;
    }

    public int getLevelFromBounds(Bounds bounds) {
        int max = 4;
        String[] keys = {"top", "left", "bottom", "right"};

        for (String key : keys) {
            int coordinate = 0;

            switch (key) {
                case "top" -> coordinate = bounds.top;
                case "left" -> coordinate = bounds.left;
                case "bottom" -> coordinate = bounds.bottom;
                case "right" -> coordinate = bounds.right;
                default -> {
                }
            }

            if (coordinate + 1 > max) {
                max = coordinate + 1;
            } else if (-coordinate > max) {
                max = -coordinate;
            }
        }

        return (int) Math.ceil(Math.log(max) / Math.log(2)) + 1;
    }

    public void setupField(IntBuffer fieldX, IntBuffer fieldY, Bounds bounds) {
        if (bounds == null) {
            bounds = getBounds(fieldX, fieldY);
        }
        int level = getLevelFromBounds(bounds);
        double offset = pow2(level - 1);
        int count = fieldX.capacity();
        moveField(fieldX, fieldY, (int) offset, (int) offset);
        root = setupFieldRecurse(0, count - 1, fieldX, fieldY, level);
    }

    public int partition(int start, int end, IntBuffer testField, IntBuffer otherField, int offset) {
        int i = start, j = end, swap;

        while (i <= j) {
            while (i <= end && (testField.get(i) & offset) == 0) {
                i++;
            }
            while (j > start && (testField.get(j) & offset) != 0) {
                j--;
            }
            if (i >= j) {
                break;
            }
            swap = testField.get(i);
            testField.put(i, testField.get(j));
            testField.put(j, swap);

            swap = otherField.get(i);
            otherField.put(i, otherField.get(j));
            otherField.put(j, swap);

            i++;
            j--;
        }

        return i;
    }

    public Node setupFieldRecurse(int start, int end, IntBuffer fieldX, IntBuffer fieldY, int level) {
        if (start > end) {
            return emptyTree(level);
        }
        if (level == 2) {
            return level2Setup(start, end, fieldX, fieldY);
        }
        level--;
        int offset = 1 << level;
        int part3 = partition(start, end, fieldY, fieldX, offset);
        int part2 = partition(start, part3 - 1, fieldX, fieldY, offset);
        int part4 = partition(part3, end, fieldX, fieldY, offset);

        return createTree(
                setupFieldRecurse(start, part2 - 1, fieldX, fieldY, level),
                setupFieldRecurse(part2, part3 - 1, fieldX, fieldY, level),
                setupFieldRecurse(part3, part4 - 1, fieldX, fieldY, level),
                setupFieldRecurse(part4, end, fieldX, fieldY, level)
        );
    }

    public Node level2Setup(int start, int end, IntBuffer fieldX, IntBuffer fieldY) {
        int set = 0, x, y;

        for (int i = start; i <= end; i++) {
            x = fieldX.get(i);
            y = fieldY.get(i);

            // interleave 2-bit x and y values
            set |= 1 << (x & 1 | (y & 1 | x & 2) << 1 | (y & 2) << 2);
        }

        if (level2Cache.containsKey(set)) {
            return level2Cache.get(set);
        }

        Node tree = createTree(
                level1Create(set),
                level1Create(set >> 4),
                level1Create(set >> 8),
                level1Create(set >> 12)
        );

        level2Cache.put(set, tree);
        return tree;
    }


    @SuppressWarnings("unused")
    public void setStep(int step) {
        if (step != this.step) {
            this.step = step;

            uncache(false);
            emptyTreeCache = new Node[EMPTY_TREE_CACHE_SIZE];
            level2Cache = new HashMap<>(0x10000);
        }
    }

    @SuppressWarnings("unused")
    public void setRules(int s, int b) {
        if (this.rule_s != s || this.rule_b != b) {
            this.rule_s = s;
            this.rule_b = b;

            this.uncache(true);
            emptyTreeCache = new Node[EMPTY_TREE_CACHE_SIZE];
            level2Cache = new HashMap<>(0x10000);
        }
    }

    private Node nodeSetBit(Node node, int x, int y, boolean living) {
        if (node.level == 0) {
            return living ? trueLeaf : falseLeaf;
        }

        double offset = node.level == 1 ? 0 : pow2(node.level - 2);
        Node nw = node.nw, ne = node.ne, sw = node.sw, se = node.se;

        if (x < 0) {
            if (y < 0) {
                nw = nodeSetBit(nw, x + (int) offset, y + (int) offset, living);
            } else {
                sw = nodeSetBit(sw, x + (int) offset, y - (int) offset, living);
            }
        } else {
            if (y < 0) {
                ne = nodeSetBit(ne, x - (int) offset, y + (int) offset, living);
            } else {
                se = nodeSetBit(se, x - (int) offset, y - (int) offset, living);
            }
        }

        return createTree(nw, ne, sw, se);
    }

    private boolean nodeGetBit(Node node, int x, int y) {
        if (node.population.equals(BigInteger.ZERO)) {
            return false;
        }

        if (node.level == 0) {
            // other level 0 case is handled above
            return true;
        }

        double offset = node.level == 1 ? 0 : pow2(node.level - 2);

        if (x < 0) {
            if (y < 0) {
                return nodeGetBit(node.nw, x + (int) offset, y + (int) offset);
            } else {
                return nodeGetBit(node.sw, x + (int) offset, y - (int) offset);
            }
        } else {
            if (y < 0) {
                return nodeGetBit(node.ne, x - (int) offset, y + (int) offset);
            } else {
                return nodeGetBit(node.se, x - (int) offset, y - (int) offset);
            }
        }
    }


    public Node node_level2_next(Node node) {
        Node nw = node.nw;
        Node ne = node.ne;
        Node sw = node.sw;
        Node se = node.se;

        BigInteger bitmask =
                nw.nw.population.shiftLeft(15).or(nw.ne.population.shiftLeft(14)).or(ne.nw.population.shiftLeft(13)).or(ne.ne.population.shiftLeft(12))
                        .or(nw.sw.population.shiftLeft(11)).or(nw.se.population.shiftLeft(10)).or(ne.sw.population.shiftLeft(9)).or(ne.se.population.shiftLeft(8))
                        .or(sw.nw.population.shiftLeft(7)).or(sw.ne.population.shiftLeft(6)).or(se.nw.population.shiftLeft(5)).or(se.ne.population.shiftLeft(4))
                        .or(sw.sw.population.shiftLeft(3)).or(sw.se.population.shiftLeft(2)).or(se.sw.population.shiftLeft(1)).or(se.se.population);

        int result = evalMask(bitmask.shiftRight(5).intValue()) |
                evalMask(bitmask.shiftRight(4).intValue()) << 1 |
                evalMask(bitmask.shiftRight(1).intValue()) << 2 |
                evalMask(bitmask.intValue()) << 3;

        return level1Create(result);
    }

    private Node nodeNextGeneration(Node node) {
        if (node.cache != null) {
            return node.cache;
        }

        if (this.step == node.level - 2) {
            return nodeQuickNextGeneration(node);
        }

        // right now i have seen a nodeNextGeneration where
        // node.level == 2 so...
        // maybe this doesn't need to exist?
        // todo: see what happens when you have a blank canvas and use setbit...
        if (node.level == 2) {
            if (node.quick_cache == null) {
                node.quick_cache = node_level2_next(node);
            }
            return node.quick_cache;
        }

        Node nw = node.nw;
        Node ne = node.ne;
        Node sw = node.sw;
        Node se = node.se;

        @SuppressWarnings("DuplicatedCode")
        Node n00 = createTree(nw.nw.se, nw.ne.sw, nw.sw.ne, nw.se.nw);
        Node n01 = createTree(nw.ne.se, ne.nw.sw, nw.se.ne, ne.sw.nw);
        Node n02 = createTree(ne.nw.se, ne.ne.sw, ne.sw.ne, ne.se.nw);
        Node n10 = createTree(nw.sw.se, nw.se.sw, sw.nw.ne, sw.ne.nw);
        Node n11 = createTree(nw.se.se, ne.sw.sw, sw.ne.ne, se.nw.nw);
        Node n12 = createTree(ne.sw.se, ne.se.sw, se.nw.ne, se.ne.nw);
        Node n20 = createTree(sw.nw.se, sw.ne.sw, sw.sw.ne, sw.se.nw);
        Node n21 = createTree(sw.ne.se, se.nw.sw, sw.se.ne, se.sw.nw);
        Node n22 = createTree(se.nw.se, se.ne.sw, se.sw.ne, se.se.nw);

        Node result = createTree(
                nodeNextGeneration(createTree(n00, n01, n10, n11)),
                nodeNextGeneration(createTree(n01, n02, n11, n12)),
                nodeNextGeneration(createTree(n10, n11, n20, n21)),
                nodeNextGeneration(createTree(n11, n12, n21, n22))
        );

        node.cache = result;

        return result;
    }

    public Node nodeQuickNextGeneration(Node node) {
        if (node.quick_cache != null) {
            return node.quick_cache;
        }

        if (node.level == 2) {
            return node.quick_cache = this.node_level2_next(node);
        }

        Node nw = node.nw;
        Node ne = node.ne;
        Node sw = node.sw;
        Node se = node.se;

        Node n00 = this.nodeQuickNextGeneration(nw);
        Node n01 = this.nodeQuickNextGeneration(createTree(nw.ne, ne.nw, nw.se, ne.sw));
        Node n02 = this.nodeQuickNextGeneration(ne);
        Node n10 = this.nodeQuickNextGeneration(createTree(nw.sw, nw.se, sw.nw, sw.ne));
        Node n11 = this.nodeQuickNextGeneration(createTree(nw.se, ne.sw, sw.ne, se.nw));
        Node n12 = this.nodeQuickNextGeneration(createTree(ne.sw, ne.se, se.nw, se.ne));
        Node n20 = this.nodeQuickNextGeneration(sw);
        Node n21 = this.nodeQuickNextGeneration(createTree(sw.ne, se.nw, sw.se, se.sw));
        Node n22 = this.nodeQuickNextGeneration(se);

        return node.quick_cache = this.createTree(
                this.nodeQuickNextGeneration(createTree(n00, n01, n10, n11)),
                this.nodeQuickNextGeneration(createTree(n01, n02, n11, n12)),
                this.nodeQuickNextGeneration(createTree(n10, n11, n20, n21)),
                this.nodeQuickNextGeneration(createTree(n11, n12, n21, n22))
        );
    }

    public void nodeHash(Node node) {
        if (!this.inHashmap(node)) {
            // Update the id. We have looked for an old id, as
            // the hashmap has been cleared and ids have been
            // reset, but this cannot be avoided without iterating
            // the tree twice.
            node.id = lastId++;
            node.hashmapNext = null;

            if (node.level > 1) {
                nodeHash(node.nw);
                nodeHash(node.ne);
                nodeHash(node.sw);
                nodeHash(node.se);

                if (node.cache != null) {
                    nodeHash(node.cache);
                }
                if (node.quick_cache != null) {
                    nodeHash(node.quick_cache);
                }
            }

            hashmapInsert(node);
        }
    }

    private void nodeGetBoundary(Node node, int left, int top, int findMask, Bounds boundary) {
        if (node.population.equals(BigInteger.ZERO) || findMask == 0) {
            return;
        }

        if (node.level == 0) {
            if (left < boundary.left) {
                boundary.left = left;
            }
            if (left > boundary.right) {
                boundary.right = left;
            }
            if (top < boundary.top) {
                boundary.top = top;
            }
            if (top > boundary.bottom) {
                boundary.bottom = top;
            }
        } else {
            double offset = pow2(node.level - 1);

            if (left >= boundary.left && left + offset * 2 <= boundary.right &&
                    top >= boundary.top && top + offset * 2 <= boundary.bottom) {
                // this square is already inside the found boundary
                return;
            }

            int findNW = findMask;
            int findSW = findMask;
            int findNE = findMask;
            int findSE = findMask;

            if (!node.nw.population.equals(BigInteger.ZERO)) {
                findSW &= ~MASK_TOP;
                findNE &= ~MASK_LEFT;
                findSE &= ~(MASK_TOP | MASK_LEFT);
            }
            if (!node.sw.population.equals(BigInteger.ZERO)) {
                findSE &= ~MASK_LEFT;
                findNW &= ~MASK_BOTTOM;
                findNE &= ~(MASK_BOTTOM | MASK_LEFT);
            }
            if (!node.ne.population.equals(BigInteger.ZERO)) {
                findNW &= ~MASK_RIGHT;
                findSE &= ~MASK_TOP;
                findSW &= ~(MASK_TOP | MASK_RIGHT);
            }
            if (!node.se.population.equals(BigInteger.ZERO)) {
                findSW &= ~MASK_RIGHT;
                findNE &= ~MASK_BOTTOM;
                findNW &= ~(MASK_BOTTOM | MASK_RIGHT);
            }

            nodeGetBoundary(node.nw, left, top, findNW, boundary);
            nodeGetBoundary(node.sw, left, top + (int) offset, findSW, boundary);
            nodeGetBoundary(node.ne, left + (int) offset, top, findNE, boundary);
            nodeGetBoundary(node.se, left + (int) offset, top + (int) offset, findSE, boundary);
        }
    }
}



