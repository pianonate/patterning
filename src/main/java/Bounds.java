import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;

public class Bounds {
    public BigInteger top, left, bottom, right;
    private BigDecimal topDecimal, leftDecimal, bottomDecimal, rightDecimal;

    private final static Map<String, Bounds> cache = new HashMap<>();

    private final float maxFloat = Float.MAX_VALUE;
    private final BigDecimal maxFloatAsDecimal = BigDecimal.valueOf(maxFloat);
    private static long cacheHits = 0;
    private static long cacheMisses = 0;


    public Bounds(BigInteger top, BigInteger left, BigInteger bottom, BigInteger right) {
        this(top, left);
        this.bottom = bottom;
        this.right = right;
    }

    public Bounds(BigDecimal top, BigDecimal left, BigDecimal bottom, BigDecimal right) {
        this.topDecimal = top;
        this.leftDecimal = left;
        this.bottomDecimal = bottom;
        this.rightDecimal = right;
        this.top = top.toBigInteger();
        this.left = left.toBigInteger();
        this.bottom = bottom.toBigInteger();
        this.right = right.toBigInteger();
    }

    public Bounds(BigInteger top, BigInteger left) {
        this.top = top;
        this.left = left;
        this.bottom = top;
        this.right = left;
    }



    public Bounds getScreenBounds( float cellWidth, BigInteger canvasOffsetX, BigInteger canvasOffsetY) {

        String cacheKey = generateCacheKey(this, cellWidth, canvasOffsetX, canvasOffsetY);

        if (cache.containsKey(cacheKey)) {
            cacheHits++;

        } else {
            cacheMisses++;

            BigDecimal cellWidthDecimal = BigDecimal.valueOf(cellWidth);

            BigDecimal leftDecimal = this.leftToBigDecimal().multiply(cellWidthDecimal).add(new BigDecimal(canvasOffsetX));
            BigDecimal topDecimal = this.topToBigDecimal().multiply(cellWidthDecimal).add(new BigDecimal(canvasOffsetY));

            BigDecimal rightDecimal = this.rightToBigDecimal()
                    .subtract(this.leftToBigDecimal())
                    .multiply(cellWidthDecimal)
                    .add(cellWidthDecimal);
            BigDecimal bottomDecimal = this.bottomToBigDecimal()
                    .subtract(this.topToBigDecimal())
                    .multiply(cellWidthDecimal)
                    .add(cellWidthDecimal);

            Bounds newBounds = new Bounds(topDecimal.toBigInteger(),
                    leftDecimal.toBigInteger(),
                    bottomDecimal.toBigInteger(),
                    rightDecimal.toBigInteger());

            newBounds.topDecimal = topDecimal;
            newBounds.leftDecimal = leftDecimal;
            newBounds.bottomDecimal = bottomDecimal;
            newBounds.rightDecimal = rightDecimal;

            cache.put(cacheKey, newBounds);
        }

        return cache.get(cacheKey);
    }

    private String generateCacheKey(Bounds bounds, float cellWidth, BigInteger offsetX, BigInteger offsetY) {
        return cellWidth + "_" + offsetX + "_" + offsetY + "_" + bounds.top + "_" + bounds.left + "_" + bounds.bottom + "_" + bounds.right;
    }

    public BigDecimal getSize(float cellWidth) {
        BigDecimal cellWidthDecimal = BigDecimal.valueOf(cellWidth);
        return rightToBigDecimal().subtract(leftToBigDecimal()).multiply(cellWidthDecimal);
    }

    public double getCacheHitPercentage() {

        long totalRequests = cacheHits + cacheMisses;
        if (totalRequests == 0) {
            return 0;
        }
        return ((double) cacheHits) / totalRequests * 100;
    }

    public BigDecimal leftToBigDecimal() {
        return new BigDecimal(this.left);
    }

    public float leftToFloat() {
        return (leftToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : left.floatValue();
    }


    public BigDecimal rightToBigDecimal() {
        return new BigDecimal(this.right);
    }

    public float rightToFloat() {
        return (rightToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : right.floatValue();
    }

    public BigDecimal topToBigDecimal() {
        return new BigDecimal(this.top);
    }

    public float topToFloat() {
        return (topToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : top.floatValue();
    }

    public BigDecimal bottomToBigDecimal() {
        return new BigDecimal(this.bottom);
    }

    public float bottomToFloat() {
        return (bottomToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : bottom.floatValue();
    }

    @Override
    public String toString() {
        return "Bounds{" +
                "top=" + top.toString() +
                ", left=" + left.toString() +
                ", bottom=" + bottom.toString() +
                ", right=" + right.toString() +
                '}';
    }
}