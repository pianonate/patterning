import java.math.BigInteger;
import java.util.HashSet;

public class Node {

    private final static HashSet<Node> changedNodes = new HashSet<>();

    public static void addChanged(Node node) {
        changedNodes.add(node);
    }

    public static void clearChanged() {
        changedNodes.clear();
    }

    private static boolean hasChanged(Node node) {
        return changedNodes.contains(node);
    }

    public Node nw;
    public Node ne;
    public Node sw;
    public Node se;

    public int id;
    public int level;
    public BigInteger population;

    public int step;

    public Node cache = null;
    public Node quick_cache = null;
    public Node hashmapNext = null;

    private boolean[][] binaryBitArray;


    // falseLeaf, trueLeaf constructors
    public Node(int id, BigInteger population, int level) {
        this.id = id;
        this.population = population;
        this.level = level;
        this.step = 0;
    }

    public Node(Node nw, Node ne, Node sw, Node se, int id, int step) {
        this.nw = nw;
        this.ne = ne;
        this.sw = sw;
        this.se = se;
        this.id = id;
        this.step = step;

        this.level = nw.level + 1;

        this.population = nw.population.add(ne.population).add(sw.population).add(se.population);
    }


    public boolean hasChanged() {
        return Node.hasChanged(this);
    }

    public int countNodes() {
        int count = 1;

        if (nw != null) {
            count += nw.countNodes();
            count += ne.countNodes();
            count += sw.countNodes();
            count += se.countNodes();
        }

        return count;
    }

    public int countChangedNodes() {
        int count = hasChanged() ? 1 : 0;

        if (nw != null) {
            count += nw.countChangedNodes();
            count += ne.countChangedNodes();
            count += sw.countChangedNodes();
            count += se.countChangedNodes();
        }

        return count;
    }

    // you have to make sure that you don't call this on a top level node or it will puke
    public boolean[][] getBinaryBitArray() {

        if (binaryBitArray==null) {

            int arraySize = (int) Math.pow(2, level);
            binaryBitArray = new boolean[arraySize][arraySize];

            fillBinaryBitArray(this, binaryBitArray, 0, 0, arraySize);
        }

        return binaryBitArray;
    }

    public void clearBinaryBitArray() {
        binaryBitArray = null;
    }

    private void fillBinaryBitArray(Node node, boolean[][] binaryBitArray, int x, int y, int size) {

        if (node.level == 0) {
            if (node.population.equals(BigInteger.ONE)) {
                binaryBitArray[y][x] = true;
            }
            return;
        }

        int halfSize = size / 2;
        fillBinaryBitArray(node.nw, binaryBitArray, x, y, halfSize);
        fillBinaryBitArray(node.ne, binaryBitArray, x + halfSize, y, halfSize);
        fillBinaryBitArray(node.sw, binaryBitArray, x, y + halfSize, halfSize);
        fillBinaryBitArray(node.se, binaryBitArray, x + halfSize, y + halfSize, halfSize);
    }


}