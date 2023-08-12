package patterning.util

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class FlexibleDecimal private constructor(initialValue: Number) : Comparable<FlexibleDecimal> {
    
    private val transformedValue: Number = when (initialValue) {
        is BigInteger -> BigDecimal(initialValue)
        is Long -> initialValue.toDouble()
        is Int -> initialValue.toFloat()
        else -> initialValue
    }
    
    val value: Number = when (transformedValue) {
        is BigDecimal -> {
            when (transformedValue) {
                in FLOAT_MIN_BIG_DECIMAL..FLOAT_MAX_BIG_DECIMAL -> initialValue.toFloat()
                in DOUBLE_MIN_BIG_DECIMAL..DOUBLE_MAX_BIG_DECIMAL -> initialValue.toDouble()
                else -> initialValue
            }
        }
        
        is Double -> {
            when (transformedValue) {
                in FLOAT_MIN..FLOAT_MAX -> initialValue.toFloat()
                else -> initialValue
            }
        }
        
        is Float -> transformedValue
        
        else -> throw IllegalArgumentException("Unsupported number type")
    }
    
    
    operator fun plus(other: FlexibleDecimal): FlexibleDecimal {
        return when {
            value is Float && other.value is Float -> handleFloatAddition(value, other.value)
            value is Float && other.value is Double -> handleDoubleAddition(value.toDouble(), other.value)
            value is Float && other.value is BigDecimal -> handleBigDecimalAddition(
                value.toBigDecimalSafe(),
                other.value
            )
            
            value is Double && other.value is Float -> handleDoubleAddition(value, other.value.toDouble())
            value is Double && other.value is Double -> handleDoubleAddition(value, other.value)
            value is Double && other.value is BigDecimal -> handleBigDecimalAddition(
                value.toBigDecimalSafe(),
                other.value
            )
            
            value is BigDecimal && other.value is BigDecimal -> handleBigDecimalAddition(value, other.value)
            
            // the possible choice for else is going to be either an Float or a Double
            else -> handleBigDecimalAddition(value.toBigDecimalSafe(), other.value.toBigDecimalSafe())
        }
    }
    
    private fun handleFloatAddition(a: Float, b: Float): FlexibleDecimal {
        val result = a + b
        return if (result.isInfinite() || result.isNaN()) {
            create(a.toDouble() + b.toDouble())
        } else {
            create(result)
        }
    }
    
    private fun handleDoubleAddition(a: Double, b: Double): FlexibleDecimal {
        val result = a + b
        return if (result.isInfinite() || result.isNaN()) {
            create(BigDecimal.valueOf(a) + BigDecimal.valueOf(b))
        } else {
            create(result)
        }
    }
    
    private fun handleBigDecimalAddition(a: BigDecimal, b: BigDecimal): FlexibleDecimal {
        return create(a + b)
    }
    
    fun get(): Number = value
    
    fun toFloat(): Float {
        if (value is Float) {
            return value
        } else {
            throw IllegalArgumentException("Operation only supported for Float values.")
        }
    }
    
    operator fun unaryMinus(): FlexibleDecimal {
        return when (value) {
            is Float -> create(-value)
            is Double -> create(-value)
            is BigDecimal -> create(value.negate())
            else -> throw IllegalArgumentException("Unsupported number type")
        }
    }
    
    private fun addOne(): FlexibleDecimal {
        return this + ONE
    }
    
    operator fun inc(): FlexibleDecimal {
        return addOne()
    }
    
    operator fun dec(): FlexibleDecimal {
        return this - ONE
    }
    
    operator fun minus(other: FlexibleDecimal): FlexibleDecimal {
        return when {
            value is Float && other.value is Float -> handleFloatSubtraction(value, other.value)
            value is Float && other.value is Double -> handleDoubleSubtraction(value.toDouble(), other.value)
            value is Float && other.value is BigDecimal -> handleBigDecimalSubtraction(
                value.toBigDecimalSafe(),
                other.value
            )
            
            value is Double && other.value is Float -> handleDoubleSubtraction(value, other.value.toDouble())
            value is Double && other.value is Double -> handleDoubleSubtraction(value, other.value)
            value is Double && other.value is BigDecimal -> handleBigDecimalSubtraction(
                value.toBigDecimalSafe(),
                other.value
            )
            
            value is BigDecimal && other.value is BigDecimal -> handleBigDecimalSubtraction(value, other.value)
            
            // the possible choice for else is going to be either a Float or a Double
            else -> handleBigDecimalSubtraction(value.toBigDecimalSafe(), other.value.toBigDecimalSafe())
        }
    }
    
    private fun handleFloatSubtraction(a: Float, b: Float): FlexibleDecimal {
        val result = a - b
        return if (result.isInfinite() || result.isNaN()) {
            create(a.toDouble() - b.toDouble())
        } else {
            create(result)
        }
    }
    
    
    private fun handleDoubleSubtraction(a: Double, b: Double): FlexibleDecimal {
        val result = a - b
        return if (result.isInfinite() || result.isNaN()) {
            create(BigDecimal.valueOf(a) - BigDecimal.valueOf(b))
        } else {
            create(result)
        }
    }
    
    
    private fun handleBigDecimalSubtraction(a: BigDecimal, b: BigDecimal): FlexibleDecimal {
        return create(a - b)
    }
    
    fun divide(other: FlexibleDecimal, context: MathContext): FlexibleDecimal {
        return when {
            value is Float && other.value is Float -> handleFloatDivision(value, other.value)
            value is Float && other.value is Double -> handleDoubleDivision(value.toDouble(), other.value, context)
            value is Float && other.value is BigDecimal -> handleBigDecimalDivision(
                value.toBigDecimalSafe(),
                other.value,
                context
            )
            
            value is Double && other.value is Float -> handleDoubleDivision(value, other.value.toDouble(), context)
            value is Double && other.value is Double -> handleDoubleDivision(value, other.value, context)
            value is Double && other.value is BigDecimal -> handleBigDecimalDivision(
                value.toBigDecimalSafe(),
                other.value,
                context
            )
            
            value is BigDecimal && other.value is BigDecimal -> handleBigDecimalDivision(value, other.value, context)
            
            else -> handleBigDecimalDivision(value.toBigDecimalSafe(), other.value.toBigDecimalSafe(), context)
        }
    }
    
    private fun handleFloatDivision(a: Float, b: Float): FlexibleDecimal {
        val result = a / b
        return if (result.isInfinite() || result.isNaN()) {
            create(a.toDouble() / b.toDouble())
        } else {
            create(result)
        }
    }
    
    private fun handleDoubleDivision(a: Double, b: Double, context: MathContext): FlexibleDecimal {
        val result = a / b
        return if (result.isInfinite() || result.isNaN()) {
            handleBigDecimalDivision(BigDecimal.valueOf(a), BigDecimal.valueOf(b), context)
        } else {
            create(result)
        }
    }
    
    private fun handleBigDecimalDivision(a: BigDecimal, b: BigDecimal, context: MathContext): FlexibleDecimal {
        try {
            return create(a.divide(b, context))
        } catch (e: ArithmeticException) {
            throw ArithmeticException("Division by zero or other arithmetic error.")
        }
    }
    
    fun multiply(other: FlexibleDecimal, context: MathContext): FlexibleDecimal {
        return when {
            value is Float && other.value is Float -> handleFloatMultiplication(value, other.value)
            value is Float && other.value is Double -> handleDoubleMultiplication(
                value.toDouble(),
                other.value,
                context
            )
            
            value is Float && other.value is BigDecimal -> handleBigDecimalMultiplication(
                value.toBigDecimalSafe(),
                other.value,
                context
            )
            
            value is Double && other.value is Float -> handleDoubleMultiplication(
                value,
                other.value.toDouble(),
                context
            )
            
            value is Double && other.value is Double -> handleDoubleMultiplication(value, other.value, context)
            value is Double && other.value is BigDecimal -> handleBigDecimalMultiplication(
                value.toBigDecimalSafe(),
                other.value,
                context
            )
            
            value is BigDecimal && other.value is BigDecimal -> handleBigDecimalMultiplication(
                value,
                other.value,
                context
            )
            
            else -> handleBigDecimalMultiplication(value.toBigDecimalSafe(), other.value.toBigDecimalSafe(), context)
        }
    }
    
    private fun handleFloatMultiplication(a: Float, b: Float): FlexibleDecimal {
        val result = a * b
        return if (result.isInfinite() || result.isNaN()) {
            create(a.toDouble() * b.toDouble())
        } else {
            create(result)
        }
    }
    
    private fun handleDoubleMultiplication(a: Double, b: Double, context: MathContext): FlexibleDecimal {
        val result = a * b
        return if (result.isInfinite() || result.isNaN()) {
            handleBigDecimalMultiplication(BigDecimal.valueOf(a), BigDecimal.valueOf(b), context)
        } else {
            create(result)
        }
    }
    
    private fun handleBigDecimalMultiplication(a: BigDecimal, b: BigDecimal, context: MathContext): FlexibleDecimal {
        return create(a.multiply(b, context))
    }
    
    fun pow(exponent: Int): FlexibleDecimal {
        return when (value) {
            is Float -> handleFloatPow(value, exponent)
            is Double -> handleDoublePow(value, exponent)
            is BigDecimal -> handleBigDecimalPow(value, exponent)
            else -> throw UnsupportedOperationException("Unsupported type for pow operation")
        }
    }
    
    private fun handleFloatPow(base: Float, exponent: Int): FlexibleDecimal {
        val result = base.toDouble().pow(exponent.toDouble())
        return if (result.isInfinite() || result.isNaN()) {
            handleBigDecimalPow(base.toBigDecimalSafe(), exponent)
        } else {
            create(result.toFloat())
        }
    }
    
    private fun handleDoublePow(base: Double, exponent: Int): FlexibleDecimal {
        val result = base.pow(exponent.toDouble())
        return if (result.isInfinite() || result.isNaN()) {
            handleBigDecimalPow(base.toBigDecimalSafe(), exponent)
        } else {
            create(result)
        }
    }
    
    private fun handleBigDecimalPow(base: BigDecimal, exponent: Int): FlexibleDecimal {
        return create(base.pow(exponent))
    }
    
    override fun compareTo(other: FlexibleDecimal): Int {
        return when {
            value is Float && other.value is Float -> value.compareTo(other.value)
            value is Float && other.value is Double -> (value.toDouble()).compareTo(other.value)
            value is Double && other.value is Double -> value.compareTo(other.value)
            value is Double && other.value is Float -> value.compareTo(other.value.toDouble())
            value is BigDecimal && other.value is BigDecimal -> (value).compareTo(other.value)
            else -> value.toBigDecimalSafe().compareTo(other.value.toBigDecimalSafe())
        }
    }
    
    fun toDouble(): Double {
        return when (value) {
            is Float -> value.toDouble()
            is Double -> value.toDouble()
            else -> throw NumberFormatException("Value cannot be safely converted to Double.")
        }
    }
    
    private fun Number.toBigDecimalSafe(): BigDecimal {
        return when (this) {
            is BigDecimal -> this
            is Float -> BigDecimal.valueOf(toDouble())
            is Double -> BigDecimal.valueOf(this)
            else -> throw IllegalArgumentException("Unsupported number type")
        }
    }
    
    fun toNumber(): Number {
        return when (value) {
            is Float -> value
            is Double -> value
            is BigDecimal -> value
            else -> throw IllegalArgumentException("Unsupported number type")
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlexibleDecimal) return false
        return value == other.value
    }
    
    override fun hashCode(): Int {
        return value.hashCode()
    }
    
    override fun toString(): String {
        return ("${value}:${value.toString().length}")
    }
    
    companion object {
        
        // New static map to hold previously created instances
        private val instances: ConcurrentHashMap<Number, FlexibleDecimal> = ConcurrentHashMap<Number, FlexibleDecimal>()
        
        val ZERO = create(0)
        val ONE = create(1)
        val TWO = create(2)
        
        private const val FLOAT_MIN = Float.MIN_VALUE
        private const val FLOAT_MAX = Float.MAX_VALUE
        private val FLOAT_MIN_BIG_DECIMAL = Float.MIN_VALUE.toBigDecimal()
        private val FLOAT_MAX_BIG_DECIMAL = Float.MAX_VALUE.toBigDecimal()
        private val DOUBLE_MIN_BIG_DECIMAL = Double.MIN_VALUE.toBigDecimal()
        private val DOUBLE_MAX_BIG_DECIMAL = Double.MAX_VALUE.toBigDecimal()
        
        private var hits = 0L
        
        // Static factory method to create new FlexibleDecimal instances
        fun create(initialValue: Number): FlexibleDecimal {
            hits++
            // Check if an instance with this value already exists
            return if (instances.containsKey(initialValue)) {
                // If yes, return that instance
                instances[initialValue]!!
            } else {
                // If not, create a new instance, store it in the map, and return it
                val newInstance = FlexibleDecimal(initialValue)
                instances[initialValue] = newInstance
                newInstance
            }
        }
    }
}