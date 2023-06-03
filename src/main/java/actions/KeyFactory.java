package actions;

import patterning.Patterning;
import processing.core.PApplet;
import processing.event.KeyEvent;
import ux.DrawRateManager;
import ux.PatternDrawer;
import ux.UXThemeManager;
import ux.UXThemeType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

public class KeyFactory {
    private static final char SHORTCUT_CENTER = 'c';
    private static final char SHORTCUT_DISPLAY_BOUNDS = 'b';
    private static final char SHORTCUT_FIT_UNIVERSE = 'f';
    private static final char SHORTCUT_PASTE = 'v';
    private static final char SHORTCUT_PAUSE = ' ';
    private static final char SHORTCUT_RANDOM_FILE = 'r';
    private static final char SHORTCUT_REWIND = 'r'; // because this one is paired with Command
    private static final char SHORTCUT_STEP_FASTER = ']';
    private static final char SHORTCUT_STEP_SLOWER = '[';
    private static final char SHORTCUT_ZOOM_IN = '=';
    private static final char SHORTCUT_ZOOM_OUT = '-';
    private static final char SHORTCUT_UNDO = 'z';
    private static final char SHORTCUT_ZOOM_CENTERED = 'z';
    private static final char SHORTCUT_DRAW_SPEED = 's';
    private final Patterning patterning;
    private final PApplet processing;

    private final PatternDrawer drawer;

    public KeyFactory(Patterning patterning, PatternDrawer drawer) {
        this.patterning = patterning;
        this.processing = patterning;
        this.drawer = drawer;
    }

    public void setupKeyHandler() {
        KeyHandler keyHandler = new KeyHandler.Builder(processing)
            .addKeyCallback(callbackPause)
            .addKeyCallback(callbackZoomIn)
            .addKeyCallback(callbackZoomInCenter)
            .addKeyCallback(callbackZoomOut)
            .addKeyCallback(callbackZoomOutCenter)
            .addKeyCallback(callbackStepFaster)
            .addKeyCallback(callbackStepSlower)
            .addKeyCallback(callbackDrawFaster)
            .addKeyCallback(callbackDrawSlower)
            .addKeyCallback(callbackDisplayBounds)
            .addKeyCallback(callbackCenterView)
            .addKeyCallback(callbackFitUniverseOnScreen)
            .addKeyCallback(callbackThemeToggle)
            .addKeyCallback(callbackRandomLife)
            .addKeyCallback(callbackRewind)
            .addKeyCallback(callbackPaste)
            .addKeyCallback(callbackUndoMovement)
            .addKeyCallback(callbackMovement)
            .addKeyCallback(callbackLoadLifeForm)
            .build();

        System.out.println(keyHandler.getUsageText());

    }

    public final KeyCallback callbackPause = new KeyCallback(SHORTCUT_PAUSE) {
        @Override
        public void invokeFeature() {
            // the encapsulation is messy to ask the drawer to stop displaying countdown text
            // and just continue running, or toggle the running state...
            // but CountdownText already reaches back to patterning.Patterning.run()
            // so there aren't that many complex paths to deal with here...
            drawer.handlePause();
        }

        @Override
        public String getUsageText() {
            return "pause and play";
        }
    };

    public final KeyCallback callbackLoadLifeForm = new KeyCallback(
            new LinkedHashSet<>(Arrays.asList('1', '2', '3', '4', '5', '6', '7', '8', '9'))
    ) {

        @Override
        public void invokeFeature() {
            patterning.getNumberedLifeForm();
        }

        @Override
        public String getUsageText() {
            return "press a # key to load one of the first 9 embedded RLE resource files";
        }
    };

    public final KeyCallback callbackDrawSlower = new KeyCallback(
            new KeyCombo(SHORTCUT_DRAW_SPEED, KeyEvent.SHIFT)
    ) {
        @Override
        public void invokeFeature() {
            DrawRateManager drawRateManager = DrawRateManager.getInstance();
            float current = drawRateManager.getCurrentDrawRate();

            float slowdownBy;
            if (current > 10) slowdownBy = 5;
            else if (current > 5) slowdownBy = 2;
            else if (current > 1) slowdownBy = 1;
            else slowdownBy = .1F;

            drawRateManager.updateTargetDrawRate(current - slowdownBy);
        }

        @Override
        public String getUsageText() {
            return "slow the animation down";
        }
    };
    public final KeyCallback callbackDrawFaster = new KeyCallback(SHORTCUT_DRAW_SPEED) {
        @Override
        public void invokeFeature() {
            DrawRateManager drawRateManager = DrawRateManager.getInstance();
            float current = drawRateManager.getCurrentDrawRate();
            drawRateManager.updateTargetDrawRate((int) current + 5);
        }

        @Override
        public String getUsageText() {
            return "speed the animation up";
        }
    };

    public final KeyCallback callbackStepFaster = new KeyCallback(SHORTCUT_STEP_FASTER) {
        @Override
        public void invokeFeature() {
            patterning.handleStep(true);
        }


        @Override
        public String getUsageText() {
            return "double the generations per draw";
        }

    };
    public final KeyCallback callbackStepSlower = new KeyCallback(SHORTCUT_STEP_SLOWER) {

        @Override
        public void invokeFeature() {
            patterning.handleStep(false);
        }

        @Override
        public String getUsageText() {
            return "cut in half the generations per draw";
        }
    };

    public final KeyCallback callbackRewind = new KeyCallback(
            new KeyCombo(SHORTCUT_REWIND, KeyEvent.META, ValidOS.MAC),
            new KeyCombo(SHORTCUT_REWIND, KeyEvent.CTRL, ValidOS.NON_MAC)
    ) {
        @Override
        public void invokeFeature() {
            patterning.destroyAndCreate();
        }

        @Override
        public String getUsageText() {
            return "rewind the current life form back to generation 0";
        }
    };
    public final KeyCallback callbackRandomLife = new KeyCallback(SHORTCUT_RANDOM_FILE) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {

            patterning.getRandomLifeform(true);
            patterning.destroyAndCreate();

        }

        @Override
        public String getUsageText() {
            return "get a random life form from the built-in library";
        }
    };

    public final KeyCallback callbackZoomIn = new KeyCallback(
            new KeyCombo(SHORTCUT_ZOOM_IN),
            new KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)
    ) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(true, patterning.getMouseX(), patterning.getMouseY());
        }

        @Override
        public String getUsageText() {
            return "zoom in centered on the mouse";
        }
    };
    public final KeyCallback callbackZoomInCenter = new KeyCallback(SHORTCUT_ZOOM_CENTERED) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(true, (float) patterning.getWidth() / 2, (float) patterning.getHeight() / 2);
        }

        @Override
        public String getUsageText() {
            return "zoom in centered on the middle of the screen";
        }
    };

    public final KeyCallback callbackZoomOutCenter = new KeyCallback(
            new KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)
    ) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(false, (float) patterning.getWidth() / 2, (float) patterning.getHeight() / 2);
        }

        @Override
        public String getUsageText() {
            return "zoom out centered on the middle of the screen";
        }
    };
    public final KeyCallback callbackZoomOut = new KeyCallback(SHORTCUT_ZOOM_OUT) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(false, patterning.getMouseX(), patterning.getMouseY());
        }

        @Override
        public String getUsageText() {
            return "zoom out centered on the mouse";
        }
    };

    public final KeyCallback callbackDisplayBounds = new KeyCallback(SHORTCUT_DISPLAY_BOUNDS) {
        @Override
        public void invokeFeature() {
            drawer.toggleDrawBounds();
        }

        @Override
        public String getUsageText() {
            return "draw a border around the part of the universe containing living cells";
        }
    };
    public final KeyCallback callbackCenterView = new KeyCallback(SHORTCUT_CENTER) {
        @Override
        public void invokeFeature() {
            patterning.centerView();
        }

        @SuppressWarnings("SameReturnValue")
        @Override
        public String getUsageText() {
            return "center the view on the universe - regardless of its size";
        }
    };
    public final KeyCallback callbackUndoMovement = new KeyCallback(
            new KeyCombo(SHORTCUT_UNDO, KeyEvent.META, ValidOS.MAC),
            new KeyCombo(SHORTCUT_UNDO, KeyEvent.CTRL, ValidOS.NON_MAC)
    ) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            drawer.undoMovement();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "undo various movement actions such as centering or fitting to screen";
        }
    };
    public final KeyCallback callbackFitUniverseOnScreen = new KeyCallback(SHORTCUT_FIT_UNIVERSE) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            patterning.fitUniverseOnScreen();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "fit the visible universe on screen";
        }
    };

    public final KeyCallback callbackThemeToggle = new KeyCallback('d') {
        private boolean toggled = true;
        @Override
        public void invokeFeature() {
            if (toggled)
                UXThemeManager.getInstance().setTheme(UXThemeType.DEFAULT);
            else
                UXThemeManager.getInstance().setTheme(UXThemeType.DARK);

            toggled = !toggled;
        }

        @Override
        public String getUsageText() {
            return "toggle between dark and light themes";
        }
    };

    public final KeyCallback callbackPaste = new KeyCallback(
            new KeyCombo(SHORTCUT_PASTE, KeyEvent.META, ValidOS.MAC),
            new KeyCombo(SHORTCUT_PASTE, KeyEvent.CTRL, ValidOS.NON_MAC)

    ) {
        @Override
        public void invokeFeature() {
            patterning.pasteLifeForm();
        }

        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    };

    public final KeyCallback callbackMovement = new KeyCallback(Stream.of(PApplet.LEFT, PApplet.RIGHT, PApplet.UP, PApplet.DOWN)
            .map(KeyCombo::new)
            .toArray(KeyCombo[]::new)) {
        private boolean pressed = false;

        @Override
        public void invokeFeature() {
            if (!pressed) {
                pressed = true;
                // we only want to save the undo state for key presses when we start them
                // no need to save again until they're all released
                drawer.saveUndoState();
            }
        }

        @Override
        public void cleanupFeature() {
            if (KeyHandler.getPressedKeys().size() == 0) {
                pressed = false;
            }
        }

        @Override
        public String getUsageText() {
            return "use arrow keys to move the image around. hold down two keys to move diagonally";
        }
    };
}
