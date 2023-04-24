import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class BoundsTest {
    private Bounds bounds;

    @BeforeEach
    void setUp() {
        bounds = new Bounds(BigInteger.valueOf(1), BigInteger.valueOf(2),
                BigInteger.valueOf(3), BigInteger.valueOf(4));
    }

    @Test
    void constructorTest() {
        assertEquals(BigInteger.valueOf(1), bounds.top, "Top should be initialized to 1");
        assertEquals(BigInteger.valueOf(2), bounds.left, "Left should be initialized to 2");
        assertEquals(BigInteger.valueOf(3), bounds.bottom, "Bottom should be initialized to 3");
        assertEquals(BigInteger.valueOf(4), bounds.right, "Right should be initialized to 4");
    }
}