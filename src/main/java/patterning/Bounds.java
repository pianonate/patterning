package patterning;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Bounds {
    private final static Map<String, Bounds> cache = new HashMap<>();
    private static long cacheHits = 0;
    private static long cacheMisses = 0;
    private final float maxFloat = Float.MAX_VALUE;
    private final BigDecimal maxFloatAsDecimal = BigDecimal.valueOf(maxFloat);
    public BigInteger top, left, bottom, right;
    private BigDecimal topDecimal, leftDecimal, bottomDecimal, rightDecimal;

    public Bounds(BigInteger top, BigInteger left, BigInteger bottom, BigInteger right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public Bounds(BigDecimal top, BigDecimal left, BigDecimal bottom, BigDecimal right) {
        this(top.toBigInteger(), left.toBigInteger(), bottom.toBigInteger(), right.toBigInteger());
        this.topDecimal = top;
        this.leftDecimal = left;
        this.bottomDecimal = bottom;
        this.rightDecimal = right;
    }

    public Bounds(BigInteger top, BigInteger left) {
        this(top, left, top, left);
    }


    public Bounds getScreenBounds(float cellWidth, BigDecimal canvasOffsetX, BigDecimal canvasOffsetY) {

        String cacheKey = generateCacheKey(this, cellWidth, canvasOffsetX, canvasOffsetY);

        if (cache.containsKey(cacheKey)) {
            cacheHits++;

        } else {
            cacheMisses++;

            BigDecimal cellWidthDecimal = BigDecimal.valueOf(cellWidth);

            BigDecimal leftDecimal = this.leftToBigDecimal().multiply(cellWidthDecimal).add(canvasOffsetX);
            BigDecimal topDecimal = this.topToBigDecimal().multiply(cellWidthDecimal).add(canvasOffsetY);

            BigDecimal rightDecimal = this.rightToBigDecimal()
                    .subtract(this.leftToBigDecimal())
                    .multiply(cellWidthDecimal)
                    .add(cellWidthDecimal);
            BigDecimal bottomDecimal = this.bottomToBigDecimal()
                    .subtract(this.topToBigDecimal())
                    .multiply(cellWidthDecimal)
                    .add(cellWidthDecimal);

            Bounds newBounds = new Bounds(topDecimal, leftDecimal, bottomDecimal, rightDecimal);

            cache.put(cacheKey, newBounds);
        }

        return cache.get(cacheKey);
    }

    private String generateCacheKey(Bounds bounds, float cellWidth, BigDecimal offsetX, BigDecimal offsetY) {
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

        if (this.leftDecimal == null)
            this.leftDecimal = new BigDecimal(this.left);

        return leftDecimal;
    }

    public float leftToFloat() {
        return (leftToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : left.floatValue();
    }


    public BigDecimal rightToBigDecimal() {

        if (this.rightDecimal == null) {
            this.rightDecimal = new BigDecimal(this.right);
        }

        return this.rightDecimal;
    }

    public float rightToFloat() {
        return (rightToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : right.floatValue();
    }

    public BigDecimal topToBigDecimal() {

        if (this.topDecimal == null)
            this.topDecimal = new BigDecimal(this.top);

        return topDecimal;
    }

    public float topToFloat() {
        return (topToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : top.floatValue();
    }

    public BigDecimal bottomToBigDecimal() {

        if (this.bottomDecimal == null)
            this.bottomDecimal = new BigDecimal(this.bottom);

        return bottomDecimal;
    }

    public float bottomToFloat() {
        return (bottomToBigDecimal().compareTo(maxFloatAsDecimal) > 0) ? maxFloat : bottom.floatValue();
    }

    @Override
    public String toString() {
        return "patterning.Bounds{" +
                "top=" + top.toString() +
                ", left=" + left.toString() +
                ", bottom=" + bottom.toString() +
                ", right=" + right.toString() +
                '}';
    }
}