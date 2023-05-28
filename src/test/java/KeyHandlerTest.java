import actions.KeyCallback;
import actions.KeyCombo;
import actions.KeyHandler;
import actions.ValidOS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import processing.core.PApplet;
import processing.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

public class KeyHandlerTest {
    // Mock Processing applet for testing
     final PApplet mockPApplet = new PApplet();

    @Test
    public void testAddKeyCallback() {

        KeyHandler handler = new KeyHandler(mockPApplet);
        MockKeyCallback callback = new MockKeyCallback('A');
        handler.addKeyCallback(callback);
        KeyEvent testEvent = new KeyEvent(mockPApplet,
                System.currentTimeMillis(),
                KeyEvent.PRESS,
                0,
                'A',
                'A');

        handler.keyEvent(testEvent);
        assertTrue(callback.wasCalled());
    }

    @Test
    public void testAddKeyCallbackDuplicate() {
        KeyHandler handler = new KeyHandler(mockPApplet);

        // Duplicate actions.KeyCallback with single char
        MockKeyCallback callback1 = new MockKeyCallback('A');
        MockKeyCallback callback2 = new MockKeyCallback('A');
        handler.addKeyCallback(callback1);

        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> handler.addKeyCallback(callback2));
        String expectedMessage1 = "The following key combos are already associated with another callback: A";
        assertTrue(exception1.getMessage().contains(expectedMessage1));

        // Duplicate actions.KeyCallback with char and modifier
        KeyCombo comboWithModifier = new KeyCombo('B', KeyEvent.CTRL);
        MockKeyCallback callback3 = new MockKeyCallback(comboWithModifier);
        MockKeyCallback callback4 = new MockKeyCallback(comboWithModifier);
        handler.addKeyCallback(callback3);

        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> handler.addKeyCallback(callback4));
        String expectedMessage2 = "The following key combos are already associated with another callback: ^B";
        assertTrue(exception2.getMessage().contains(expectedMessage2));

        // Duplicate actions.KeyCallback with char, modifier and validOS
        KeyCombo comboWithModifierAndOS = new KeyCombo('C', KeyEvent.CTRL, ValidOS.NON_MAC);
        MockKeyCallback callback5 = new MockKeyCallback(comboWithModifierAndOS);
        MockKeyCallback callback6 = new MockKeyCallback(comboWithModifierAndOS);
        handler.addKeyCallback(callback5);

        Exception exception3 = assertThrows(IllegalArgumentException.class, () -> handler.addKeyCallback(callback6));
        String expectedMessage3 = "The following key combos are already associated with another callback: ^C";
        assertTrue(exception3.getMessage().contains(expectedMessage3));
    }

    @Test
    public void testAddKeyCallbackWithDifferentModifiers() {
        KeyHandler handler = new KeyHandler(mockPApplet);

        // Adding actions.KeyCallback with 'A' and CTRL modifier
        KeyCombo combo1 = new KeyCombo('A', KeyEvent.CTRL);
        MockKeyCallback callback1 = new MockKeyCallback(combo1);
        handler.addKeyCallback(callback1);

        // Adding actions.KeyCallback with 'A' and SHIFT modifier
        KeyCombo combo2 = new KeyCombo('A', KeyEvent.SHIFT);
        MockKeyCallback callback2 = new MockKeyCallback(combo2);
        handler.addKeyCallback(callback2); // This should not throw an exception
    }

    @Test
    public void testAddKeyCallbackWithSameCharModifierDifferentOS() {
        KeyHandler handler = new KeyHandler(mockPApplet);

        // Adding actions.KeyCallback with 'A', CTRL modifier and Windows OS
        KeyCombo combo1 = new KeyCombo('A', KeyEvent.CTRL, ValidOS.NON_MAC);
        MockKeyCallback callback1 = new MockKeyCallback(combo1);
        handler.addKeyCallback(callback1);

        // Adding actions.KeyCallback with 'A', CTRL modifier and macOS
        KeyCombo combo2 = new KeyCombo('A', KeyEvent.CTRL, ValidOS.MAC);
        MockKeyCallback callback2 = new MockKeyCallback(combo2);
        handler.addKeyCallback(callback2); // This should not throw an exception
    }

    @Test
    public void testAddKeyCallbackWithSameCharDifferentModifierSameOS() {
        KeyHandler handler = new KeyHandler(mockPApplet);

        // Adding actions.KeyCallback with 'A', CTRL modifier and Windows OS
        KeyCombo combo1 = new KeyCombo('A', KeyEvent.CTRL, ValidOS.NON_MAC);
        MockKeyCallback callback1 = new MockKeyCallback(combo1);
        handler.addKeyCallback(callback1);

        // Adding actions.KeyCallback with 'A', SHIFT modifier and Windows OS
        KeyCombo combo2 = new KeyCombo('A', KeyEvent.SHIFT, ValidOS.NON_MAC);
        MockKeyCallback callback2 = new MockKeyCallback(combo2);
        handler.addKeyCallback(callback2); // This should not throw an exception
    }

    @ParameterizedTest
    @EnumSource(ValidOS.class)
    public void testGetUsageTextReturnsOSSpecificUsage(ValidOS os) {

        KeyCombo.setCurrentOS(os);

        KeyHandler handler = new KeyHandler(mockPApplet);

        KeyCombo keyCombo1 = new KeyCombo('a', KeyEvent.META, ValidOS.MAC);
        KeyCombo keyCombo2 = new KeyCombo('b', KeyEvent.CTRL, ValidOS.NON_MAC);
        KeyCombo keyCombo3 = new KeyCombo('c', KeyEvent.ALT, ValidOS.ANY);

        KeyCallback keyCallback1 = new KeyCallback(keyCombo1) {
            @Override
            public void invokeFeature() {
                // handle key event
            }

            @Override
            public String getUsageText() {
                return "Sample usage text for MAC";
            }

        };

        KeyCallback keyCallback2 = new KeyCallback(keyCombo2) {
            @Override
            public void invokeFeature() {
                // handle key event
            }

            @Override
            public String getUsageText() {
                return "Sample usage text for NON_MAC";
            }

        };

        KeyCallback keyCallback3 = new KeyCallback(keyCombo3) {
            @Override
            public void invokeFeature() {
                // handle key event
            }

            @Override
            public String getUsageText() {
                return "Sample usage text for ANY";
            }

        };

        handler.addKeyCallback(keyCallback1);
        handler.addKeyCallback(keyCallback2);
        handler.addKeyCallback(keyCallback3);

        // Usage text should only contain key combos that are valid for current OS
        String usageText = handler.getUsageText();

        switch (os) {

            case ANY -> {
                assertTrue(usageText.contains("Sample usage text for ANY"));
                assertFalse(usageText.contains("Sample usage text for MAC"));
                assertFalse(usageText.contains("Sample usage text for NON_MAC"));
            }
            case MAC -> {
                assertTrue(usageText.contains("Sample usage text for MAC"));
                assertTrue(usageText.contains("Sample usage text for ANY"));
                assertFalse(usageText.contains("Sample usage text for NON_MAC"));
            }
            case NON_MAC -> {
                assertTrue(usageText.contains("Sample usage text for NON_MAC"));
                assertTrue(usageText.contains("Sample usage text for ANY"));
                assertFalse(usageText.contains("Sample usage text for MAC"));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ValidOS.class)
    public void testGetUsageTextWithOSDependentKeyCombos(ValidOS os) {

        KeyCombo.setCurrentOS(os);

        KeyHandler handler = new KeyHandler(mockPApplet);

        // Create actions.KeyCallback with two KeyCombos, one valid for Mac, another valid for Non-Mac.
        KeyCallback callback = new KeyCallback(
                new KeyCombo('C', KeyEvent.META, ValidOS.MAC),
                new KeyCombo('C', KeyEvent.CTRL, ValidOS.NON_MAC),
                new KeyCombo('C', KeyEvent.ALT, ValidOS.ANY)
        ) {
            @Override
            public void invokeFeature() {

            }

            @Override
            public String getUsageText() {
                return "copy behavior";
            }
        };

        handler.addKeyCallback(callback);

        // Now get the usage text
        String usageText = handler.getUsageText();

        switch (os) {

            case ANY -> {
                assertTrue(usageText.contains("⌥C"));
                assertFalse(usageText.contains("⌘C"));
                assertFalse(usageText.contains("^C"));
            }
            case MAC -> {
                assertTrue(usageText.contains("⌥C"));
                assertTrue(usageText.contains("⌘C"));
                assertFalse(usageText.contains("^C"));
            }
            case NON_MAC -> {
                assertTrue(usageText.contains("⌥C"));
                assertFalse(usageText.contains("⌘C"));
                assertTrue(usageText.contains("^C"));
            }
        }
    }
}
