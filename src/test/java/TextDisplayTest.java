import org.junit.jupiter.api.Test;
import processing.core.PGraphics;

import static org.junit.jupiter.api.Assertions.*;


public class TextDisplayTest {

    @Test
    public void testConstruction() {
        TextDisplay display = new TextDisplay.Builder("Test", TextDisplay.Position.CENTER)
                .textSize(12)
                .textColor(0x00FFFFFF)
                .fadeInDuration(1000)
                .build();
        assertNotNull(display);
    }




    @Test
    public void testDisplayMessage() {
        PGraphics buffer = new PGraphics();
        TextDisplay display = new TextDisplay.Builder("Test", TextDisplay.Position.CENTER)
                .textSize(12)
                .textColor(0x00FFFFFF)
                .fadeInDuration(1000)
                .build();

        assertEquals("Test", display.message);
        display.setMessage("New Message");
        assertEquals("New Message", display.message);
    }

    @Test
    public void testStartDisplay() {
        TextDisplay display = new TextDisplay.Builder("Test", TextDisplay.Position.CENTER)
                .textSize(12)
                .textColor(0x00FFFFFF)
                .fadeInDuration(1000)
                .build();

        assertFalse(display.isDisplaying);
        display.startDisplay();
        assertTrue(display.isDisplaying);
    }

    @Test
    public void testStopDisplay() {
        TextDisplay display = new TextDisplay.Builder("Test", TextDisplay.Position.CENTER)
                .textSize(12)
                .textColor(0x00FFFFFF)
                .fadeInDuration(1000)
                .build();

        display.startDisplay();
        assertTrue(display.isDisplaying);
        display.stopDisplay();
        assertFalse(display.isDisplaying);
    }

}
