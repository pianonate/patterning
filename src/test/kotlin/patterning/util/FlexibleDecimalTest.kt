package patterning.util

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class FlexibleDecimalTest {
    
    /**
     * Creation tests
     */
    
    @Test
    fun `test creation with int - expect float`() {
        val flexibleDecimal = FlexibleDecimal.create(5)
        assertEquals(5f, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with long - expect float`() {
        val flexibleDecimal = FlexibleDecimal.create(5L)
        assertEquals(5F, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with BigInteger - expect float`() {
        val flexibleDecimal = FlexibleDecimal.create(BigInteger.valueOf(5))
        assertEquals(5f, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with float`() {
        val flexibleDecimal = FlexibleDecimal.create(5.0f)
        assertEquals(5f, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with double - expect float`() {
        val flexibleDecimal = FlexibleDecimal.create(5.0)
        assertEquals(5f, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with BigDecimal - expect float`() {
        val flexibleDecimal = FlexibleDecimal.create(BigDecimal.valueOf(5.0))
        assertEquals(5f, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with float max value`() {
        val flexibleDecimal = FlexibleDecimal.create(Float.MAX_VALUE)
        assertEquals(Float.MAX_VALUE, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with float min value`() {
        val flexibleDecimal = FlexibleDecimal.create(Float.MIN_VALUE)
        assertEquals(Float.MIN_VALUE, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with double max value`() {
        val flexibleDecimal = FlexibleDecimal.create(Double.MAX_VALUE)
        assertEquals(Double.MAX_VALUE, flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with double min value`() {
        val flexibleDecimal = FlexibleDecimal.create(Double.MIN_VALUE)
        assertEquals(Double.MIN_VALUE, flexibleDecimal.get())
    }
}