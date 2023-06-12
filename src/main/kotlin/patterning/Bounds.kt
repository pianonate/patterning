package patterning

import java.math.BigDecimal
import java.math.BigInteger

class Bounds @JvmOverloads constructor(
    @JvmField var top: BigInteger,
    @JvmField var left: BigInteger,
    @JvmField var bottom: BigInteger = top,
    @JvmField var right: BigInteger = left
) {
    private val maxFloat = Float.MAX_VALUE
    private val maxFloatAsDecimal = BigDecimal.valueOf(maxFloat.toDouble())
    private var topDecimal: BigDecimal? = null
    private var leftDecimal: BigDecimal? = null
    private var bottomDecimal: BigDecimal? = null
    private var rightDecimal: BigDecimal? = null

    constructor(top: BigDecimal, left: BigDecimal, bottom: BigDecimal, right: BigDecimal) : this(
        top.toBigInteger(),
        left.toBigInteger(),
        bottom.toBigInteger(),
        right.toBigInteger()
    ) {
        topDecimal = top
        leftDecimal = left
        bottomDecimal = bottom
        rightDecimal = right
    }

    fun getScreenBounds(cellSize: BigDecimal, canvasOffsetX: BigDecimal, canvasOffsetY: BigDecimal): Bounds? {
        val cacheKey = generateCacheKey(this, canvasOffsetX, canvasOffsetY)
        //val cacheKey = generateCacheKey(this)
        if (cache.containsKey(cacheKey)) {
            cacheHits++
        } else {
            cacheMisses++
            val leftDecimal = leftToBigDecimal().multiply(cellSize).add(canvasOffsetX)
            val topDecimal = topToBigDecimal().multiply(cellSize).add(canvasOffsetY)
            val rightDecimal = rightToBigDecimal()
                .subtract(leftToBigDecimal())
                .multiply(cellSize)
                .add(cellSize)
            val bottomDecimal = bottomToBigDecimal()
                .subtract(topToBigDecimal())
                .multiply(cellSize)
                .add(cellSize)
            val newBounds = Bounds(topDecimal, leftDecimal, bottomDecimal, rightDecimal)
            cache[cacheKey] = newBounds
        }
        return cache[cacheKey]
    }

    private fun generateCacheKey(bounds: Bounds, offsetX: BigDecimal, offsetY: BigDecimal): String {
        return offsetX.toString() + "_" + offsetY + "_" + bounds.top + "_" + bounds.left + "_" + bounds.bottom + "_" + bounds.right
    }

/*    private fun generateCacheKey(bounds: Bounds): String {
        return  bounds.top.toString() + "_" + bounds.left + "_" + bounds.bottom + "_" + bounds.right
    }*/

    fun leftToBigDecimal(): BigDecimal {
        if (leftDecimal == null) leftDecimal = BigDecimal(left)
        return leftDecimal!!
    }

    fun leftToFloat(): Float {
        return if (leftToBigDecimal() > maxFloatAsDecimal) maxFloat else left.toFloat()
    }

    private fun rightToBigDecimal(): BigDecimal {
        if (rightDecimal == null) rightDecimal = BigDecimal(right)
        return rightDecimal!!
    }

    fun rightToFloat(): Float {
        return if (rightToBigDecimal() > maxFloatAsDecimal) maxFloat else right.toFloat()
    }

    fun topToBigDecimal(): BigDecimal {
        if (topDecimal == null) topDecimal = BigDecimal(top)
        return topDecimal!!
    }

    fun topToFloat(): Float {
        return if (topToBigDecimal() > maxFloatAsDecimal) maxFloat else top.toFloat()
    }

    private fun bottomToBigDecimal(): BigDecimal {
        if (bottomDecimal == null) bottomDecimal = BigDecimal(bottom)
        return bottomDecimal!!
    }

    fun bottomToFloat(): Float {
        return if (bottomToBigDecimal() > maxFloatAsDecimal) maxFloat else bottom.toFloat()
    }

    override fun toString(): String {
        return "patterning.Bounds{" +
                "top=" + top.toString() +
                ", left=" + left.toString() +
                ", bottom=" + bottom.toString() +
                ", right=" + right.toString() +
                '}'
    }

    companion object {
        private val cache: MutableMap<String, Bounds> = HashMap()
        private var cacheHits: Long = 0
        private var cacheMisses: Long = 0
    }
}