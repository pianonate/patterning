package patterning.util

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class FlexibleIntegerTest {

    @Test
    fun testIntAddition() {
        val a = FlexibleInteger.create(Int.MAX_VALUE)
        val b = FlexibleInteger.create(1)
        val result = a + b
        assertEquals(FlexibleInteger.create(Int.MAX_VALUE.toLong() + 1), result)
    }

    @Test
    fun testLongAddition() {
        val a = FlexibleInteger.create(Long.MAX_VALUE)
        val b = FlexibleInteger.create(1)
        val result = a + b
        assertEquals(FlexibleInteger.create(BigInteger.valueOf(Long.MAX_VALUE) + BigInteger.ONE), result)
    }

    @Test
    fun testIntSubtraction() {
        val a = FlexibleInteger.create(Int.MIN_VALUE)
        val b = FlexibleInteger.create(-1)
        val result = a - b
        assertEquals(FlexibleInteger.create(Int.MIN_VALUE.toLong() + 1), result)
    }

    @Test
    fun testLongSubtraction() {
        val a = FlexibleInteger.create(Long.MIN_VALUE)
        val b = FlexibleInteger.create(-1)
        val result = a - b
        assertEquals(FlexibleInteger.create(BigInteger.valueOf(Long.MIN_VALUE) + BigInteger.ONE), result)
    }

}