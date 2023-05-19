import org.junit.jupiter.api.Test;
import processing.core.PGraphics;

import static org.junit.jupiter.api.Assertions.*;


public class TextDisplayTest {

    @Test
    public void testConstruction() {
        PGraphics buffer = new PGraphics();
        TextDisplay display = new TextDisplay(buffer, "Test", TextDisplay.Position.CENTER, 12, 0x00FFFFFF, 1000);

        assertNotNull(display);
    }

    @Test
    public void testDisplayPosition() {
        PGraphics buffer = new PGraphics();
        buffer.setSize(100, 100);

        TextDisplay displayCenter = new TextDisplay(buffer, "Test", TextDisplay.Position.CENTER, 12, 0x00FFFFFF, 1000);
        assertEquals(50, displayCenter.x);
        assertEquals(50, displayCenter.y);

        TextDisplay displayTopLeft = new TextDisplay(buffer, "Test", TextDisplay.Position.TOP_LEFT, 12, 0x00FFFFFF, 1000);
        assertEquals(10, displayTopLeft.x);
        assertEquals(10, displayTopLeft.y);

        TextDisplay displayTopCenter = new TextDisplay(buffer, "Test", TextDisplay.Position.TOP_CENTER, 12, 0x00FFFFFF, 1000);
        assertEquals(50, displayTopCenter.x);
        assertEquals(10, displayTopCenter.y);

        TextDisplay displayTopRight = new TextDisplay(buffer, "Test", TextDisplay.Position.TOP_RIGHT, 12, 0x00FFFFFF, 1000);
        assertEquals(90, displayTopRight.x); // buffer width (100) - margin (10)
        assertEquals(10, displayTopRight.y);
    }


    @Test
    public void testDisplayMessage() {
        PGraphics buffer = new PGraphics();
        TextDisplay display = new TextDisplay(buffer, "Test", TextDisplay.Position.CENTER, 12, 0x00FFFFFF, 1000);

        assertEquals("Test", display.message);
        display.setMessage("New Message");
        assertEquals("New Message", display.message);
    }

    @Test
    public void testStartDisplay() {
        PGraphics buffer = new PGraphics();
        TextDisplay display = new TextDisplay(buffer, "Test", TextDisplay.Position.CENTER, 12, 0x00FFFFFF, 1000);

        assertFalse(display.isDisplaying);
        display.startDisplay();
        assertTrue(display.isDisplaying);
    }

    @Test
    public void testStopDisplay() {
        PGraphics buffer = new PGraphics();
        TextDisplay display = new TextDisplay(buffer, "Test", TextDisplay.Position.CENTER, 12, 0x00FFFFFF, 1000);

        display.startDisplay();
        assertTrue(display.isDisplaying);
        display.stopDisplay();
        assertFalse(display.isDisplaying);
    }

}
