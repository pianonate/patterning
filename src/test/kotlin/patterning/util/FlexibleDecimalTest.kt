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
    fun `test creation with int - expect BigDecimal`() {
        val flexibleDecimal = FlexibleDecimal.create(5)
        assertEquals(5.toBigDecimal(), flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with long - expect BigDecimal`() {
        val flexibleDecimal = FlexibleDecimal.create(Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE.toBigDecimal(), flexibleDecimal.get())
    }
    
    @Test
    fun `test creation with small BigInteger - expect BigDecimal`() {
        val flexibleDecimal = FlexibleDecimal.create(BigInteger.valueOf(5))
        assertEquals(5.toBigDecimal(), flexibleDecimal.get())
    }
    
    
    @Test
    fun `test creation with large BigInteger - expect BigDecimal`() {
        val bigInt = BigInteger.valueOf(Long.MAX_VALUE)
        val biggerInt = bigInt.add(bigInt)
        
        val flexibleDecimal = FlexibleDecimal.create(biggerInt)
        assertEquals(biggerInt.toBigDecimal(), flexibleDecimal.get())
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
    fun `test creation with small BigDecimal - expect float`() {
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