import org.junit.jupiter.api.Test;
import ux.Panel;
import ux.TextPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TextDisplayTest {

    @Test
    public void testConstruction() {
        TextPanel display = new TextPanel.Builder("Test", Panel.HAlign.CENTER, Panel.VAlign.CENTER)
                .textSize(12)
                .fadeInDuration(1000)
                .build();
        assertNotNull(display);
    }

    @Test
    public void testDisplayMessage() {
        TextPanel display = new TextPanel.Builder("Test", Panel.HAlign.CENTER, Panel.VAlign.CENTER)
                .textSize(12)
                .fadeInDuration(1000)
                .build();

        assertEquals("Test", display.message);
        display.setMessage("New Message");
        assertEquals("New Message", display.message);
    }


}
