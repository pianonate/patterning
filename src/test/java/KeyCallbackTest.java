import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.event.KeyEvent;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class KeyCallbackTest {


    @Test
    public void testKeyCallbackWithSingleChar() {
        MockKeyCallback callback = new MockKeyCallback('B');
        KeyCombo expected = new KeyCombo('B');
        assertTrue(callback.getKeyCombos().contains(expected));
    }

    @Test
    public void testKeyCallbackWithCharacterSet() {
        Set<Character> charSet = Set.of('A', 'B', 'C');
        MockKeyCallback callback = new MockKeyCallback(charSet);
        charSet.forEach(ch -> {
            KeyCombo expected = new KeyCombo(ch);
            assertTrue(callback.getKeyCombos().contains(expected));
        });
    }

    @Test
    public void testKeyCallbackWithKeyCombos() {
        KeyCombo kc1 = new KeyCombo(65, KeyEvent.CTRL, ValidOS.MAC);
        KeyCombo kc2 = new KeyCombo(66, KeyEvent.ALT, ValidOS.NON_MAC);
        KeyCombo kc3 = new KeyCombo(67);
        MockKeyCallback callback = new MockKeyCallback(kc1, kc2, kc3);
        assertTrue(callback.getKeyCombos().containsAll(Set.of(kc1, kc2, kc3)));
    }

    // Test for KeyCallback matches() method
    @Test
    public void testKeyCallbackMatches() {
        MockKeyCallback callback = new MockKeyCallback('A');
        KeyEvent event = new KeyEvent(new PApplet(), System.currentTimeMillis(), KeyEvent.PRESS, 0, 'A', 'A');

        assertTrue(callback.matches(event));

        event = new KeyEvent(new PApplet(), System.currentTimeMillis(), KeyEvent.PRESS, 0, 'B', 'B');
        assertFalse(callback.matches(event));
    }

    // Test for KeyCallback isValidForCurrentOS() method
    @Test
    public void testKeyCallbackIsValidForCurrentOS() {
        MockKeyCallback callback = new MockKeyCallback(new KeyCombo('A', 0, ValidOS.MAC));
        assertEquals(System.getProperty("os.name").toLowerCase().contains("mac"), callback.isValidForCurrentOS());

        callback = new MockKeyCallback(new KeyCombo('A', 0, ValidOS.NON_MAC));
        assertEquals(!System.getProperty("os.name").toLowerCase().contains("mac"), callback.isValidForCurrentOS());

        callback = new MockKeyCallback(new KeyCombo('A'));
        assertTrue(callback.isValidForCurrentOS());
    }
}
