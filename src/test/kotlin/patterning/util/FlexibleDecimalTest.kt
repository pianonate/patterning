package patterning.util

import java.math.MathContext
import kotlin.test.Test
import kotlin.test.assertEquals

class FlexibleDecimalTest {
    
    @Test
    fun `test multiply`() {
        assertEquals(
            FlexibleDecimal.create(0.5),
            FlexibleDecimal.create(0.25).multiply(FlexibleDecimal.TWO, MathContext(2))
        )
        assertEquals(
            FlexibleDecimal.create(0.5),
            FlexibleDecimal.create(0.25).multiply(FlexibleDecimal.TWO, MathContext(2))
        )
    }
    
}