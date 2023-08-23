package patterning.util

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class FlexibleDecimal private constructor(initialValue: Number) :
    Comparable<FlexibleDecimal> {
    
    private fun canRepresentAsDouble(value: BigDecimal): Boolean {
        val doubleValue = value.toDouble()
        if (doubleValue.isInfinite() || doubleValue.isNaN()) {
            return false
        }
        
        return doubleValue.toBigDecimal() == value
    }
    
    
    private fun canRepresentAsFloat(value: BigDecimal): Boolean {
        val floatValue = value.toFloat()
        if (floatValue.isInfinite() || floatValue.isNaN()) {
            return false
        }
        
        return floatValue.toBigDecimal() == value
    }
    
    val value: Number = initialValue.toBigDecimalSafe().let { transformed ->
        when {
            initialValue is Int -> initialValue.toFloat()
            initialValue is Long -> initialValue.toDouble()
            canRepresentAsFloat(transformed) -> transformed.toFloat()
            canRepresentAsDouble(transformed) -> transformed.toDouble()
            else -> transformed
        }
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
        val floatResult = a + b
        
        return if (floatResult.isInfinite() || floatResult.isNaN()) {
            handleDoubleAddition(a.toDouble(), b.toDouble())// Use Double if precision is lost
        } else {
            val result = create(floatResult)
            result
        }
    }
    
    
    private fun handleDoubleAddition(a: Double, b: Double): FlexibleDecimal {
        val doubleResult = a + b
        
        return if (doubleResult.isInfinite() || doubleResult.isNaN()) {
            handleBigDecimalAddition(
                a.toBigDecimal(),
                b.toBigDecimal()
            )
        } else {
            val result = create(doubleResult)
            result
        }
    }
    
    
    private fun handleBigDecimalAddition(a: BigDecimal, b: BigDecimal): FlexibleDecimal {
        return create(a + b)
    }
    
    fun get(): Number = value
    
    fun toFloat(): Float {
        val doubleValue = value.toDouble()
        if (doubleValue < FLOAT_MIN || doubleValue > FLOAT_MAX) {
            throw IllegalArgumentException("Value out of range for Float.")
        } else {
            return value.toFloat()
        }
    }
    
    /**
     * scale is primarily used by BoundingBox and it helps to align the precision for all the calculations
     * for debugging it is easier to have everything line up so we take a minor performance hit - to just
     * simply scale all the 'major players' to the same precision
     */
    fun scale(mc: MathContext): FlexibleDecimal {
        
        val working = when (value) {
            is BigDecimal -> value
            is Double -> value.toBigDecimal()
            is Float -> BigDecimal(value.toDouble())
            else -> throw IllegalArgumentException("Unsupported number type")
        }
        
        val newPrecision = working.precision()
        val toTruncate = newPrecision - mc.precision
        val newScale = working.scale() - toTruncate
        return create(working.setScale(newScale, mc.roundingMode))
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
        
        val floatResult = a - b
        
        return if (floatResult.isInfinite() || floatResult.isNaN()) {
            handleDoubleSubtraction(a.toDouble(), b.toDouble())
        } else {
            
            val result = create(floatResult)
            result
            
        }
    }
    
    private fun handleDoubleSubtraction(a: Double, b: Double): FlexibleDecimal {
        // val bigDecimalResult = a.toBigDecimal() - b.toBigDecimal()
        val doubleResult = a - b
        
        return if (/*doubleResult.toBigDecimal() != bigDecimalResult ||*/ doubleResult.isInfinite() || doubleResult.isNaN()) {
            handleBigDecimalSubtraction(
                a.toBigDecimal(),
                b.toBigDecimal()
            ) // create(bigDecimalResult) // Use BigDecimal if precision is lost or result is outside of the range
        } else {
            val result = create(doubleResult)
            
            result
        }
    }
    
    
    private fun handleBigDecimalSubtraction(a: BigDecimal, b: BigDecimal): FlexibleDecimal {
        
        return create(a - b)
    }
    
    fun divide(other: FlexibleDecimal, context: MathContext): FlexibleDecimal {
        return when {
            value is Float && other.value is Float -> handleFloatDivision(value, other.value, context)
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
    
    private fun handleFloatDivision(a: Float, b: Float, context: MathContext): FlexibleDecimal {
        val floatResult = a / b
        
        return if (floatResult.isInfinite() || floatResult.isNaN()) {
            handleDoubleDivision(
                a.toDouble(),
                b.toDouble(),
                context
            ) // Use Double if precision is lost or result is outside of the range for Float
        } else {
            
            val result = create(floatResult)
            result
        }
    }
    
    private fun handleDoubleDivision(a: Double, b: Double, context: MathContext): FlexibleDecimal {
        
        val doubleResult = a / b
        
        return if (doubleResult.isInfinite() || doubleResult.isNaN()) {
            handleBigDecimalDivision(a.toBigDecimal(), b.toBigDecimal(), context)
        } else {
            create(doubleResult)
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
            value is Float && other.value is Float -> handleFloatMultiplication(value, other.value, context)
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
    
    private fun handleFloatMultiplication(a: Float, b: Float, context: MathContext): FlexibleDecimal {
        
        val floatResult = a * b
        
        return if (floatResult.isInfinite() || floatResult.isNaN()) {
            handleDoubleMultiplication(a.toDouble(), b.toDouble(), context)
        } else {
            create(floatResult)
        }
    }
    
    
    private fun handleDoubleMultiplication(a: Double, b: Double, context: MathContext): FlexibleDecimal {
        val doubleResult = a * b
        
        return if (doubleResult.isInfinite() || doubleResult.isNaN()) {
            handleBigDecimalMultiplication(a.toBigDecimal(), b.toBigDecimal(), context)
        } else {
            create(doubleResult)
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
        return value.toDouble()
    }
    
    private fun Number.toBigDecimalSafe(): BigDecimal {
        return when (this) {
            is BigDecimal -> this
            is BigInteger -> this.toBigDecimal()
            is Float -> this.toBigDecimal()
            is Long -> this.toBigDecimal()
            is Int -> this.toBigDecimal()
            is Double -> this.toBigDecimal()
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
        return when (value) {
            is BigDecimal -> "${value}:$precision:BD"
            is Double -> "${value}:$precision:D"
            is Float -> "${value}:$precision:F"
            else -> {
                throw IllegalArgumentException("Unsupported number type")
            }
        }
    }
    
    private val precision: Int
        get() {
            return when (value) {
                is Float -> value.toBigDecimal().toPlainString().filter { it.isDigit() }.length
                is Double -> value.toBigDecimal().toPlainString().filter { it.isDigit() }.length
                is BigDecimal -> value.toPlainString().filter { it.isDigit() }.length
                else -> throw IllegalArgumentException("Unsupported number type")
            }
        }
    
    companion object {
        
        // New static map to hold previously created instances
        private val instances: ConcurrentHashMap<Number, FlexibleDecimal> = ConcurrentHashMap<Number, FlexibleDecimal>()
        
        val ZERO = create(0)
        val ONE = create(1)
        val TWO = create(2)
        
        // we don't use Float.MIN_VALUE as this is actually the smallest non-zero value of a Float - we need to check
        // positive and negative range
        private const val FLOAT_MIN = -Float.MAX_VALUE
        private const val FLOAT_MAX = Float.MAX_VALUE
        
        private var hits = 0L
        
        // Static factory method to create new FlexibleDecimal instances
        fun create(initialValue: Number): FlexibleDecimal {
            hits++
            // Check if an instance with this value already exists
            return if (instances.containsKey(initialValue)) {
                val instance = instances[initialValue]!!
                // If yes, return that instance
                instance
            } else {
                // If not, create a new instance, store it in the map, and return it
                val newInstance = FlexibleDecimal(initialValue)
                
                instances[initialValue] = newInstance
                newInstance
            }
        }
    }
}