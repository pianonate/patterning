import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundsTest {
    private Bounds bounds;

    @BeforeEach
    void setUp() {
        bounds = new Bounds(1, 2, 3, 4);
    }

    @Test
    void constructorTest() {
        assertEquals(1, bounds.top, "Top should be initialized to 1");
        assertEquals(2, bounds.left, "Left should be initialized to 2");
        assertEquals(3, bounds.bottom, "Bottom should be initialized to 3");
        assertEquals(4, bounds.right, "Right should be initialized to 4");
    }
}
