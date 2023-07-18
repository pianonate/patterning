package patterning

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

fun Number.formatWithCommas(): String = NumberFormat.getInstance().format(this)

class FlexibleInteger(initialValue: Number) : Comparable<FlexibleInteger> {

    private val value: Number = when (initialValue) {
        is BigInteger -> {
            when (initialValue) {
                in INT_MIN_BIG_INTEGER..INT_MAX_BIG_INTEGER -> initialValue.toInt()
                in LONG_MIN_BIG_INTEGER..LONG_MAX_BIG_INTEGER -> initialValue.toLong()
                else -> initialValue
            }
        }

        is Long -> {
            when (initialValue) {
                in INT_MIN..INT_MAX -> initialValue.toInt()
                else -> initialValue
            }
        }

        is Int -> initialValue

        else -> throw IllegalArgumentException("Unsupported number type")
    }


    operator fun plus(other: FlexibleInteger): FlexibleInteger {
        return when {
            value is Int && other.value is Int -> handleIntAddition(value, other.value)
            value is Int && other.value is Long -> handleLongAddition(value.toLong(), other.value)
            value is Int && other.value is BigInteger -> handleBigIntegerAddition(value.toBigIntegerSafe(), other.value)

            value is Long && other.value is Int -> handleLongAddition(value, other.value.toLong())
            value is Long && other.value is Long -> handleLongAddition(value, other.value)
            value is Long && other.value is BigInteger -> handleBigIntegerAddition(
                value.toBigIntegerSafe(),
                other.value
            )

            value is BigInteger && other.value is BigInteger -> handleBigIntegerAddition(value, other.value)

            // the possible choice for else is going to be either an int or a long
            else -> handleBigIntegerAddition(value.toBigIntegerSafe(), other.value.toBigIntegerSafe())
        }
    }

    private fun handleIntAddition(a: Int, b: Int): FlexibleInteger {
        return try {
            FlexibleInteger(Math.addExact(a, b))
        } catch (ex: ArithmeticException) {
            FlexibleInteger(a.toLong() + b.toLong())
        }
    }

    private fun handleLongAddition(a: Long, b: Long): FlexibleInteger {
        return try {
            FlexibleInteger(Math.addExact(a, b))
        } catch (ex: ArithmeticException) {
            FlexibleInteger(BigInteger.valueOf(a) + BigInteger.valueOf(b))
        }
    }

    private fun handleBigIntegerAddition(a: BigInteger, b: BigInteger): FlexibleInteger {
        return FlexibleInteger(a + b)
    }

    fun get(): Number = value

    fun isOne(): Boolean = when (value) {
        is Int -> value == 1
        is Long -> value == 1L
        is BigInteger -> value == BigInteger.ONE
        else -> throw IllegalArgumentException("Unexpected type")
    }

    fun isNotZero(): Boolean = when (value) {
        is Int -> value != 0
        is Long -> value != 0L
        is BigInteger -> value != BigInteger.ZERO
        else -> throw IllegalArgumentException("Unexpected type")
    }

    fun isZero(): Boolean = when (value) {
        is Int -> value == 0
        is Long -> value == 0L
        is BigInteger -> value == BigInteger.ZERO
        else -> throw IllegalArgumentException("Unexpected type")
    }

    fun shiftLeft(n: Int): Int {
        if (value is Int) {
            return (value) shl n
        } else {
            throw IllegalArgumentException("Operation only supported for Int values.")
        }
    }

    fun or(other: FlexibleInteger): Int {
        if (value is Int && other.value is Int) {
            return (value) or (other.value)
        } else {
            throw IllegalArgumentException("Operation only supported for Int values.")
        }
    }

    fun toInt(): Int {
        if (value is Int) {
            return value
        } else {
            throw IllegalArgumentException("Operation only supported for Int values.")
        }
    }

    fun negate(): FlexibleInteger {
        return when (value) {
            is Int -> FlexibleInteger(-value)
            is Long -> FlexibleInteger(-value)
            is BigInteger -> FlexibleInteger(value.negate())
            else -> throw IllegalArgumentException("Unsupported number type")
        }
    }

    fun addOne(): FlexibleInteger {
        return this + ONE
    }

    operator fun minus(other: FlexibleInteger): FlexibleInteger {
        return when {
            value is Int && other.value is Int -> handleIntSubtraction(value, other.value)
            value is Int && other.value is Long -> handleLongSubtraction(value.toLong(), other.value)
            value is Int && other.value is BigInteger -> handleBigIntegerSubtraction(value.toBigIntegerSafe(), other.value)

            value is Long && other.value is Int -> handleLongSubtraction(value, other.value.toLong())
            value is Long && other.value is Long -> handleLongSubtraction(value, other.value)
            value is Long && other.value is BigInteger -> handleBigIntegerSubtraction(value.toBigIntegerSafe(), other.value)

            value is BigInteger && other.value is BigInteger -> handleBigIntegerSubtraction(value, other.value)

            // the possible choice for else is going to be either an int or a long
            else -> handleBigIntegerSubtraction(value.toBigIntegerSafe(), other.value.toBigIntegerSafe())
        }
    }

    private fun handleIntSubtraction(a: Int, b: Int): FlexibleInteger {
        return try {
            FlexibleInteger(Math.subtractExact(a, b))
        } catch (ex: ArithmeticException) {
            FlexibleInteger(a.toLong() - b.toLong())
        }
    }

    private fun handleLongSubtraction(a: Long, b: Long): FlexibleInteger {
        return try {
            FlexibleInteger(Math.subtractExact(a, b))
        } catch (ex: ArithmeticException) {
            FlexibleInteger(BigInteger.valueOf(a) - BigInteger.valueOf(b))
        }
    }

    private fun handleBigIntegerSubtraction(a: BigInteger, b: BigInteger): FlexibleInteger {
        return FlexibleInteger(a - b)
    }


    fun min(other: FlexibleInteger): FlexibleInteger {
        return when {
            value is Int && other.value is Int -> if (value <= other.value) this else other
            value is Int && other.value is Long -> if (value.toLong() <= other.value) this else other
            value is Long && other.value is Long -> if (value <= other.value) this else other
            value is Long && other.value is Int -> if (value <= other.value.toLong()) this else other
            value is BigInteger && other.value is BigInteger -> if (value <= other.value) this else other
            else -> if (value.toBigIntegerSafe() <= other.value.toBigIntegerSafe()) this else other
        }
    }

    fun max(other: FlexibleInteger): FlexibleInteger {
        return when {
            value is Int && other.value is Int -> if (value >= other.value) this else other
            value is Int && other.value is Long -> if (value.toLong() >= other.value) this else other
            value is Long && other.value is Long -> if (value >= other.value) this else other
            value is Long && other.value is Int -> if (value >= other.value.toLong()) this else other
            value is BigInteger && other.value is BigInteger -> if (value >= other.value) this else other
            else -> if (value.toBigIntegerSafe() >= other.value.toBigIntegerSafe()) this else other
        }
    }

    override fun compareTo(other: FlexibleInteger): Int {
        return when {
            value is Int && other.value is Int -> value.compareTo(other.value)
            value is Int && other.value is Long -> (value.toLong()).compareTo(other.value)
            value is Long && other.value is Long -> value.compareTo(other.value)
            value is Long && other.value is Int -> value.compareTo(other.value.toLong())
            value is BigInteger && other.value is BigInteger -> (value).compareTo(other.value)
            else -> value.toBigIntegerSafe().compareTo(other.value.toBigIntegerSafe())
        }
    }

    fun toBigDecimal(): BigDecimal {
        return when (value) {
            is Int -> BigDecimal(value)
            is Long -> BigDecimal(value)
            is BigInteger -> BigDecimal(value)
            else -> BigDecimal(value.toBigIntegerSafe())
        }
    }

    private fun Number.toBigIntegerSafe(): BigInteger {
        return when (this) {
            is BigInteger -> this
            is Int -> BigInteger.valueOf(toLong())
            is Long -> BigInteger.valueOf(this)
            else -> throw IllegalArgumentException("Unsupported number type")
        }
    }

    fun minPrecisionForDrawing(): Int {
        return when (value) {
            is Int, is Long -> calculateMinPrecision()
            is BigInteger -> calculateMinPrecisionBigInteger()
            else -> throw IllegalArgumentException("Unsupported number type")
        }
    }

    private fun calculateMinPrecision(): Int {
        if (value == 0) return 0
        // We take log to base 10 of the number. We add 1 to round up to the nearest greater integer.
        return ceil(ln(value.toDouble()) / ln(10.0)).toInt()
    }

    private fun calculateMinPrecisionBigInteger(): Int {
        // We take log to base 10 of the BigInteger. BigInteger does not have log, so we convert it to BigDecimal
        if (value is BigInteger) {
            val logValue = BigDecimal(value).precision() - 1
            // Adding 1 to round up to the nearest greater integer.
            return logValue + 1
        } else {
            throw IllegalArgumentException("Unsupported number type")
        }

    }

    fun getLevel(): Int {
        return when (value) {
            is Int, is Long -> calculateLevel()
            is BigInteger -> calculateLevelBigInteger()
            else -> throw IllegalArgumentException("Unsupported number type")
        }
    }

    private fun calculateLevel(): Int {
        if (value == 0) return 1 // if value is 0, log2(0) is undefined but for the purpose of this class return 1
        return ceil(ln(value.toDouble()) / ln(2.0)).toInt() + 1
    }

    private fun calculateLevelBigInteger(): Int {
        if (value is BigInteger) {
            // bitLength() returns the number of bits in the minimal two's-complement
            // representation of this BigInteger, excluding a sign bit, which is effectively floor(log2(number)).
            // We check if the BigInteger is a power of 2, and if it is, we return the bit length directly.
            // Otherwise, we return bitLength + 1 to effectively calculate the ceiling of log2(number).
            val bitLength = value.bitLength()
            return if (value.shiftRight(bitLength).bitCount() == 1) bitLength else bitLength + 1
        } else {
            throw IllegalArgumentException("Unsupported number type")
        }
    }




    fun hudFormatted(): String {
        if (value == 0) return "0"
        return if (value is Int) {
            if (value < 1_000_000_000)
                value.formatWithCommas()
            else formatLargeNumber()
        } else {
            formatLargeNumber()
        }
    }

    private fun formatLargeNumber(): String {

        val bigIntegerValue = if (value is BigInteger) value else BigInteger.valueOf(value.toLong())
        val exponent = bigIntegerValue.toString().length - 1
        val index = (exponent - 3) / 3
        return when {
            index < largeNumberNames.size -> {
                val divisor = BigDecimal.valueOf(10.0.pow((index * 3 + 3).toDouble()))
                val shortNumber = bigIntegerValue.toBigDecimal().divide(divisor, 1, RoundingMode.HALF_UP).toDouble()
                "${shortNumber.toInt()}.${(shortNumber % 1 * 10).roundToInt()} ${largeNumberNames[index]}"
            }

            else -> String.format("%.1e", bigIntegerValue.toBigDecimal())
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlexibleInteger) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        val NEGATIVE_ONE = FlexibleInteger(-1)
        val ONE = FlexibleInteger(1)
        val ZERO = FlexibleInteger(0)
        private const val INT_MIN = Int.MIN_VALUE
        private const val INT_MAX = Int.MAX_VALUE
        private val INT_MIN_BIG_INTEGER = Int.MIN_VALUE.toBigInteger()
        private val INT_MAX_BIG_INTEGER = Int.MAX_VALUE.toBigInteger()
        private val LONG_MIN_BIG_INTEGER = Long.MIN_VALUE.toBigInteger()
        private val LONG_MAX_BIG_INTEGER = Long.MAX_VALUE.toBigInteger()

        private const val UNIVERSE_LEVEL_LIMIT = 2048

        private val _powers: HashMap<Int, FlexibleInteger> = hashMapOf(0 to FlexibleInteger(BigInteger.ONE))

        fun pow2(x: Int): FlexibleInteger {
            return _powers.getOrPut(x) { FlexibleInteger(BigInteger.valueOf(2).pow(x)) }
        }

        val MAX_VALUE by lazy { pow2(UNIVERSE_LEVEL_LIMIT) }
        val MIN_VALUE by lazy { MAX_VALUE.negate() }

        private val largeNumberNames = arrayOf(
            "thousand", "million", "billion", "trillion", "quadrillion", "quintillion", "sextillion",
            "septillion", "octillion", "nonillion", "decillion", "undecillion", "duodecillion",
            "tredecillion", "quattuordecillion"
        )
    }
}