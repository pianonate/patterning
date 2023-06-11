package ux

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class HUDStringBuilder {
    private val data // Changed from Number to Object
            : MutableMap<String, Any>
    private var cachedFormattedString = ""
    private val numberFormat: NumberFormat
    private val delimiter = " | "
    private var lastUpdateFrame = 0

    init {
        data = LinkedHashMap() // Use LinkedHashMap to maintain the insertion order
        numberFormat = NumberFormat.getInstance()
    }

    fun addOrUpdate(key: String, value: Any?) {
        if (value is Number) {
            data[key] = value
        } else {
            throw IllegalArgumentException("Value must be a Number or BigInteger.")
        }
    }

    fun addOrUpdate(key: String, value: String) {
        data[key] = value
    }

    private fun formatLargeNumber(value: Any): String {
        return if (value is BigInteger) {
            val exponent = value.toString().length - 1
            formatLargeNumberUsingExponent(BigDecimal(value), exponent)
        } else {
            val numValue = value as Number
            val doubleValue = numValue.toDouble()
            val exponent = floor(log10(doubleValue)).toInt()
            formatLargeNumberUsingExponent(BigDecimal.valueOf(doubleValue), exponent)
        }
    }

    private fun formatLargeNumberUsingExponent(value: BigDecimal, exponent: Int): String {
        val largeNumberNames = arrayOf(
            "thousand", "million", "billion", "trillion", "quadrillion", "quintillion", "sextillion",
            "septillion", "octillion", "nonillion", "decillion", "undecillion", "duodecillion",
            "tredecillion", "quattuordecillion"
        )
        val index = (exponent - 3) / 3
        return if (index < 0) {
            numberFormat.format(value)
        } else if (index < largeNumberNames.size) {
            val divisor = BigDecimal.valueOf(10.0.pow((index * 3 + 3).toDouble()))
            val shortNumber = value.divide(divisor, 1, RoundingMode.HALF_UP)
            String.format("%.1f %s", shortNumber, largeNumberNames[index])
        } else {
            String.format("%.1e", value)
        }
    }

    private fun getFormattedString(frameCount: Int, updateFrequency: Int, delimiter: String): String {
        if (frameCount - lastUpdateFrame >= updateFrequency || cachedFormattedString.isEmpty()) {
            val formattedString = StringBuilder()
            for ((key, value) in data) {
                val formattedValue = if (value is Number && value.toDouble() >= 10.0.pow(9.0)) {
                    key + " " + formatLargeNumber(value)
                } else if (value is BigInteger && value >= BigInteger.valueOf(1000000000)) {
                    key + " " + formatLargeNumber(value)
                } else if (value is String) {
                    value
                } else {
                    key + " " + numberFormat.format(value)
                }
                formattedString.append(formattedValue).append(delimiter)
            }
            // Remove the last delimiter
            if (formattedString.isNotEmpty()) {
                formattedString.setLength(formattedString.length - delimiter.length)
            }
            cachedFormattedString = formattedString.toString()
            lastUpdateFrame = frameCount
        }
        return cachedFormattedString
    }

    fun getFormattedString(frameCount: Int, updateFrequency: Int): String {
        return getFormattedString(frameCount, updateFrequency, delimiter)
    }
}