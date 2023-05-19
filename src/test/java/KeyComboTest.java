import org.junit.jupiter.api.Test;
import processing.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

public class KeyComboTest {

    @Test
    public void testKeyComboWithKeyCodeOnly() {
        KeyCombo keyCombo = new KeyCombo(65);
        assertEquals(65, keyCombo.getKeyCode());
        assertEquals(0, keyCombo.getModifiers());
        assertEquals(ValidOS.ANY, keyCombo.getValidOS());
    }

    @Test
    public void testKeyComboWithKeyCodeAndModifiers() {
        KeyCombo keyCombo = new KeyCombo(65, KeyEvent.META);
        assertEquals(65, keyCombo.getKeyCode());
        assertEquals(KeyEvent.META, keyCombo.getModifiers());
        assertEquals(ValidOS.ANY, keyCombo.getValidOS());
    }

    @Test
    public void testKeyComboWithKeyCodeModifiersAndValidOS() {
        KeyCombo keyComboMac = new KeyCombo(65, KeyEvent.CTRL, ValidOS.MAC);
        assertEquals(65, keyComboMac.getKeyCode());
        assertEquals(KeyEvent.CTRL, keyComboMac.getModifiers());
        assertEquals(ValidOS.MAC, keyComboMac.getValidOS());

        KeyCombo keyComboNonMac = new KeyCombo(65, KeyEvent.CTRL, ValidOS.NON_MAC);
        assertEquals(65, keyComboNonMac.getKeyCode());
        assertEquals(KeyEvent.CTRL, keyComboNonMac.getModifiers());
        assertEquals(ValidOS.NON_MAC, keyComboNonMac.getValidOS());

        KeyCombo keyComboAny = new KeyCombo(65, KeyEvent.CTRL, ValidOS.ANY);
        assertEquals(65, keyComboAny.getKeyCode());
        assertEquals(KeyEvent.CTRL, keyComboAny.getModifiers());
        assertEquals(ValidOS.ANY, keyComboAny.getValidOS());
    }

    @Test
    public void testIsValidForCurrentOS() {
        KeyCombo keyComboMac = new KeyCombo(65, KeyEvent.CTRL, ValidOS.MAC);
        KeyCombo keyComboNonMac = new KeyCombo(65, KeyEvent.CTRL, ValidOS.NON_MAC);
        KeyCombo keyComboAny = new KeyCombo(65, KeyEvent.CTRL, ValidOS.ANY);

        String currentOS = System.getProperty("os.name").toLowerCase();

        if (currentOS.contains("mac")) {
            assertTrue(keyComboMac.isValidForCurrentOS());
            assertFalse(keyComboNonMac.isValidForCurrentOS());
        } else {
            assertFalse(keyComboMac.isValidForCurrentOS());
            assertTrue(keyComboNonMac.isValidForCurrentOS());
        }
        assertTrue(keyComboAny.isValidForCurrentOS());
    }
}
