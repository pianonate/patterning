import processing.core.PApplet;
import processing.data.JSONObject;
import processing.event.KeyEvent;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Patterning extends PApplet {
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
    private static final String PROPERTY_FILE_NAME = "patterning_autosave.json";
    private static final char SHORTCUT_DRAW_FASTER = 's';

    private static boolean running;
    private static LifeUniverse life;

    private DrawRateController drawRateController;

    private final List<MouseEventReceiver> mouseEventReceivers = new ArrayList<>();
    private final KeyCallback callbackDrawSlower = new KeyCallback(
            new KeyCombo(SHORTCUT_DRAW_FASTER, KeyEvent.SHIFT)
    ) {
        @Override
        public void invokeFeature() {
            float current = drawRateController.getCurrentDrawRate();

            float slowdownBy;
            if (current > 10) slowdownBy = 5;
            else if (current > 5) slowdownBy = 2;
            else if (current > 1) slowdownBy = 1;
            else slowdownBy = .1F;

            drawRateController.updateTargetDrawRate(current - slowdownBy);
        }

        @Override
        public String getUsageText() {
            return "slow the animation  down";
        }
    };
    private final KeyCallback callbackDrawFaster = new KeyCallback(SHORTCUT_DRAW_FASTER) {
        @Override
        public void invokeFeature() {

            float current = drawRateController.getCurrentDrawRate();
            drawRateController.updateTargetDrawRate((int) current + 5);

        }

        @Override
        public String getUsageText() {
            return "speed the animation up";
        }
    };
    private ComplexCalculationHandler<Integer> complexCalculationHandlerSetStep;
    private ComplexCalculationHandler<Void> complexCalculationHandlerNextGeneration;
    private PatternDrawer drawer;
    private final KeyCallback callbackMovement = new KeyCallback(Stream.of(PApplet.LEFT, PApplet.RIGHT, PApplet.UP, PApplet.DOWN)
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
    private final KeyCallback callbackZoomIn = new KeyCallback(
            new KeyCombo(SHORTCUT_ZOOM_IN),
            new KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)
    ) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(true, mouseX, mouseY);
        }

        @Override
        public String getUsageText() {
            return "zoom in centered on the mouse";
        }
    };
    private final KeyCallback callbackZoomInCenter = new KeyCallback(SHORTCUT_ZOOM_CENTERED) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(true, width / 2, height / 2);
        }

        @Override
        public String getUsageText() {
            return "zoom in centered on the middle of the screen";
        }
    };
    private final KeyCallback callbackZoomOutCenter = new KeyCallback(
            new KeyCombo(SHORTCUT_ZOOM_CENTERED, KeyEvent.SHIFT)
    ) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(false, width / 2, height / 2);
        }

        @Override
        public String getUsageText() {
            return "zoom out centered on the middle of the screen";
        }
    };
    private final KeyCallback callbackZoomOut = new KeyCallback(SHORTCUT_ZOOM_OUT) {
        @Override
        public void invokeFeature() {
            drawer.zoomXY(false, mouseX, mouseY);
        }

        @Override
        public String getUsageText() {
            return "zoom out centered on the mouse";
        }
    };
    private final KeyCallback callbackDisplayBounds = new KeyCallback(SHORTCUT_DISPLAY_BOUNDS) {
        @Override
        public void invokeFeature() {
            drawer.toggleDrawBounds();
        }

        @Override
        public String getUsageText() {
            return "draw a rectangle around the part of the universe that is 'alive'";
        }
    };
    private final KeyCallback callbackCenterView = new KeyCallback(SHORTCUT_CENTER) {
        @Override
        public void invokeFeature() {
            drawer.center(life.getRootBounds(), false, true);
        }

        @SuppressWarnings("SameReturnValue")
        @Override
        public String getUsageText() {
            return "center the view on the universe - regardless of its size";
        }
    };
    private final KeyCallback callbackUndoCenter = new KeyCallback(
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
            return "undo the last center or fit bounds to return to the last view";
        }
    };
    private final KeyCallback callbackFitUniverseOnScreen = new KeyCallback(SHORTCUT_FIT_UNIVERSE) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            fitUniverseOnScreen();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "fit the visible universe on screen";
        }
    };
    private final KeyCallback callbackPause = new KeyCallback(SHORTCUT_PAUSE) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            // the encapsulation is messy to ask the drawer to stop displaying countdown text
            // and just continue running, or toggle the running state...
            // but CountdownText already reaches back to Patterning.run()
            // so there aren't that many complex paths to deal with here...
            drawer.handlePause();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "pause and play";
        }
    };
    // used to control dragging the image around the screen with the mouse
    private float last_mouse_x;
    private float last_mouse_y;
    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private boolean mousePressedOverReceiver = false;
    // new instances only created in instantiateLife to keep things simple
    // lifeForm not made local as it is intended to be used with display functions in the future
    @SuppressWarnings("unused")
    private LifeForm lifeForm;
    private String storedLife;

    private int targetStep;
    private final KeyCallback callbackStepFaster = new KeyCallback(SHORTCUT_STEP_FASTER) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            handleStep(true);
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "double the generations per frame";
        }

    };
    private final KeyCallback callbackStepSlower = new KeyCallback(SHORTCUT_STEP_SLOWER) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            handleStep(false);
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "cut in half the generations per frame";
        }

    };
    private final KeyCallback callbackRewind = new KeyCallback(
            new KeyCombo(SHORTCUT_REWIND, KeyEvent.META, ValidOS.MAC),
            new KeyCombo(SHORTCUT_REWIND, KeyEvent.CTRL, ValidOS.NON_MAC)
    ) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            destroyAndCreate(false);
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "rewind the current life form back to generation 0";
        }
    };
    private final KeyCallback callbackRandomLife = new KeyCallback(SHORTCUT_RANDOM_FILE) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {

            destroyAndCreate(true);

        }

        @Override
        public String getUsageText() {
            return "get a random life form from the built-in library";
        }
    };
    private final KeyCallback callbackPaste = new KeyCallback(
            new KeyCombo(SHORTCUT_PASTE, KeyEvent.META, ValidOS.MAC),
            new KeyCombo(SHORTCUT_PASTE, KeyEvent.CTRL, ValidOS.NON_MAC)

    ) {
        @SuppressWarnings("unused")
        @Override
        public void invokeFeature() {
            pasteLifeForm();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    };

    public static boolean isRunning() {
        return running;
    }

    public static void toggleRun() {
        running = !running;
    }

    public static void run() {
        running = true;
    }

    public static LifeUniverse getLifeUniverse() {
        return life;
    }

    public static void main(String[] args) {
        PApplet.main("Patterning");
    }

    public void settings() {
        // on startup read the size from the json file
        // eventually find a better place for it - by default it is putting it in
        // documents/data - maybe you hide it somewhere useful
        JSONObject properties;
        String propertiesFileName = PROPERTY_FILE_NAME;
        // this dataPath() was apparently the required way to save and find it
        File propertiesFile = new File(dataPath(propertiesFileName));

        int width = 800, height = 800;

        if (propertiesFile.exists() && propertiesFile.length() > 0) {
            // Load window size from JSON file
            properties = loadJSONObject(dataPath(propertiesFileName));
            width = properties.getInt("width", width);
            height = properties.getInt("height", height);
            this.storedLife = properties.getString("lifeForm", "");
        }

        if (null == this.storedLife || this.storedLife.isEmpty()) {
            getRandomLifeform();
        }

        // Set the window size
        size(width, height);

    }

    private void getRandomLifeform() {
        // todo: do you need to instantiate every time?  maybe that's fine...
        try {
            this.storedLife = ResourceLoader.getRandomResourceAsString("rle");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void setup() {


        surface.setResizable(true);

        setupKeyHandler();
        List<ControlPanel> panels = getControlPanels();

        frameRate(DrawRateController.MAX_FRAME_RATE);

        this.drawRateController = new DrawRateController();
        this.drawer = new PatternDrawer(this, panels, drawRateController);

        this.targetStep = 0;

        complexCalculationHandlerSetStep = new ComplexCalculationHandler<>((p, v) -> {
            performComplexCalculationSetStep(p);
            return null;
        });

        complexCalculationHandlerNextGeneration = new ComplexCalculationHandler<>((v1, v2) -> {
            performComplexCalculationNextGeneration();
            return null;
        });


        loadSavedWindowPositions();

        // life will have been loaded in prior - either from saved life
        // or from the packaged resources so this doesn't need extra protection
        instantiateLifeform();

    }

    private void loadSavedWindowPositions() {

        JSONObject properties;
        String propertiesFileName = PROPERTY_FILE_NAME;
        File propertiesFile = new File(dataPath(propertiesFileName));

        int x = 100, y = 100, screenIndex = 0;

        if (propertiesFile.exists() && propertiesFile.length() > 0) {
            // Load window position, and screen from JSON file
            properties = loadJSONObject(dataPath(propertiesFileName));
            x = properties.getInt("x", x);
            y = properties.getInt("y", y);
            screenIndex = properties.getInt("screen", screenIndex);
        }

        // Set the window location based on the screen index
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        screenIndex = Math.min(screenIndex, screens.length - 1);
        GraphicsDevice screen = screens[screenIndex];
        Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();
        x = screenBounds.x + x;
        y = screenBounds.y + y;

        // use the chatGPT way to get a thing that can do you what you want
        Frame frame = getFrame();

        frame.setLocation(x, y);

    }

    private Frame getFrame() {

        // chatGPT thinks that this will only work with processing 4 so...
        // probably it would be helpful to have a non-processing mechanism
        // you're also a little bit reliant on the processing environment so...
        Component comp = (Component) getSurface().getNative();
        while (!(comp instanceof Frame)) {
            comp = comp.getParent();
        }
        return (Frame) comp;
    }

    // Override the exit() method to save window properties before closing
    @Override
    public void exit() {
        saveWindowProperties();
        super.exit();
    }

    private void saveWindowProperties() {
        JSONObject properties = new JSONObject();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();

        // this took a lot of chatting with GPT 4.0 to finally land on something that
        // would work
        Frame frame = getFrame();

        // Find the screen where the window is located
        int screenIndex = 0;
        for (int i = 0; i < screens.length; i++) {
            GraphicsDevice screen = screens[i];
            if (screen.getDefaultConfiguration().getBounds().contains(frame.getLocation())) {
                screenIndex = i;
                break;
            }
        }

        GraphicsDevice screen = screens[screenIndex];
        Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();

        properties.setInt("x", frame.getX() - screenBounds.x);
        properties.setInt("y", frame.getY() - screenBounds.y);
        properties.setInt("width", width);
        properties.setInt("height", height);
        properties.setInt("screen", screenIndex);
        properties.setString("lifeForm", storedLife);

        saveJSONObject(properties, dataPath(PROPERTY_FILE_NAME));
    }

    public void draw() {

        // we want independent control over how often we update and display
        // the next generation of drawing
        // the frameRate can and should run faster so the user experience is responsive
        drawRateController.adjustDrawRate();

        boolean shouldDrawLifeForm = drawRateController.shouldDraw();

        // goForwardIntime (below) requests a nextGeneartion and or a step change
        // both of which can take a while
        // given they operate on a separate thread for performance reasons
        // we don't want to put them before the draw or it will almost always be showing
        // that it is updatingLife().  so we need to put goForwardInTime _after_ the request to draw
        // and we tell the drawer whether it is still updatinglife since the last frame
        // we also tell the drawer whether the drawRateController thinks that it's time to draw the life form
        // in case the user has slowed it down a lot to see what's going on, it's okay for it to be going slow
        drawer.draw(!updatingLife() && shouldDrawLifeForm);

        // as mentioned above - this runs on a separate thread
        // and we don't want it to go any faster than the draw rate throttling mechanism
        if (shouldDrawLifeForm) {
            goForwardInTime();
        }

    }

    // possibly this will help when returning from screensaver
    // which had a problem that one time
    public void focusGained() {
        redraw();
    }

    private void goForwardInTime() {

        // don't start anything complex if we're not running
        if (!running)
            return;

        if (shouldStartComplexCalculationSetStep()) {
            int step = life.step;
            step += (step < targetStep) ? 1 : -1;
            complexCalculationHandlerSetStep.startCalculation(step);
            return;
        }

        if (shouldStartComplexCalculationNextGeneration()) {
            complexCalculationHandlerNextGeneration.startCalculation(null);
        }
    }

    private void performComplexCalculationSetStep(Integer step) {
        life.setStep(step);
        // todo for some reason this needs to exist or maximum volatility gun goes nuts if you step too quickly
        drawer.clearCache();
    }

    private void performComplexCalculationNextGeneration() {
        life.nextGeneration();
    }

    // only start these if you're not running either one
    private boolean shouldStartComplexCalculationNextGeneration() {

        return !updatingLife();
    }

    private boolean updatingLife() {

        // don't start if either of these calculations are currently running
        boolean setStepRunning = complexCalculationHandlerSetStep.isCalculationInProgress();
        boolean nextGenerationRunning = complexCalculationHandlerNextGeneration.isCalculationInProgress();

        return (nextGenerationRunning || setStepRunning);

    }

    private boolean shouldStartComplexCalculationSetStep() {

        // if we're not running a complex task, and we're expecting to change the step
        return (!updatingLife() && (life.step != targetStep));

    }

    public void mousePressed() {
        last_mouse_x += mouseX;
        last_mouse_y += mouseY;

        boolean mousePressedOverAny = false;

        for (MouseEventReceiver receiver : mouseEventReceivers) {
            if (!mousePressedOverAny) {
                if (receiver.mousePressedOverMe()) {
                    mousePressedOverAny = true;
                    mousePressedOverReceiver = true;
                }
            }

            receiver.onMousePressed();
        }
    }

    public void mouseReleased() {

        mousePressedOverReceiver = false;

        last_mouse_x = 0;
        last_mouse_y = 0;

        for (MouseEventReceiver receiver : mouseEventReceivers) {
            receiver.onMouseReleased();
        }
    }

    public void mouseDragged() {
        if (mousePressedOverReceiver)
            return;

        float dx = Math.round(mouseX - last_mouse_x);
        float dy = Math.round(mouseY - last_mouse_y);

        drawer.move(dx, dy);

        last_mouse_x += dx;
        last_mouse_y += dy;
    }

    public void mouseOver() {

    }

    public void pasteLifeForm() {

        try {
            // Get the system clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Check if the clipboard contains text data and then get it
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                storedLife = (String) clipboard.getData(DataFlavor.stringFlavor);
                instantiateLifeform();
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
    }

    private void instantiateLifeform() {

        try {
            // static on Patterning
            running = false;
            life = new LifeUniverse();

            // instance variables - do they need to be?
            LifeFormats parser = new LifeFormats();
            LifeForm newLife = parser.parseRLE(storedLife);

            targetStep = 0;
            life.setStep(0);

            life.setupField(newLife.field_x, newLife.field_y);
            lifeForm = newLife;

            drawer.setupNewLife(life.getRootBounds());


        } catch (NotLifeException e) {
            // todo: on failure you need to
            println("get a life - here's what failed:\n\n" + e.getMessage());
        }
    }

    private void handleStep(boolean faster) {

        int increment = (faster) ? 1 : -1;

        if (this.targetStep + increment < 0)
            increment = 0;
        this.targetStep += increment;

        String fasterOrSlower = (faster) ? "faster requested" : "slower requested";

    }

    private void fitUniverseOnScreen() {
        drawer.center(life.getRootBounds(), true, true);
    }

    // either bring us back to the start on the current life form
    // or get a random one from the well...
    private void destroyAndCreate(boolean random) {

        ComplexCalculationHandler.lock();
        try {

            if (random) {
                getRandomLifeform();
            }

            instantiateLifeform();

        } finally {
            ComplexCalculationHandler.unlock();
        }
    }

    private void setupKeyHandler() {

        KeyHandler keyHandler = new KeyHandler(this);

        // order matters - put them in the order you want them to show in getUsageText()
        keyHandler.addKeyCallback(callbackPause);
        keyHandler.addKeyCallback(callbackZoomIn);
        keyHandler.addKeyCallback(callbackZoomInCenter);
        keyHandler.addKeyCallback(callbackZoomOut);
        keyHandler.addKeyCallback(callbackZoomOutCenter);
        keyHandler.addKeyCallback(callbackStepFaster);
        keyHandler.addKeyCallback(callbackStepSlower);
        keyHandler.addKeyCallback(callbackDrawFaster);
        keyHandler.addKeyCallback(callbackDrawSlower);
        keyHandler.addKeyCallback(callbackDisplayBounds);
        keyHandler.addKeyCallback(callbackCenterView);
        keyHandler.addKeyCallback(callbackFitUniverseOnScreen);
        keyHandler.addKeyCallback(callbackRandomLife);
        keyHandler.addKeyCallback(callbackRewind);
        keyHandler.addKeyCallback(callbackPaste);
        keyHandler.addKeyCallback(callbackUndoCenter);
        keyHandler.addKeyCallback(callbackMovement);

        System.out.println(keyHandler.getUsageText());

    }

    // now set up control panels
    public List<ControlPanel> getControlPanels() {

        ControlPanel panelLeft, panelTop;

        try {
            // loading icons can generate an IOException if the file isn't there - which is problematic
            panelLeft = new ControlPanel.Builder(ControlPanel.PanelPosition.LEFT, this)
                    .addControl("fitToScreen.png", callbackFitUniverseOnScreen)
                    .addControl("zoomIn.png", callbackZoomInCenter)
                    .addControl("zoomOut.png", callbackZoomOutCenter)
                    .alignment(ControlPanel.PanelAlignment.CENTER)
                    .sizeToFit(true)
                    .build();

            // loading icons can generate an IOException if the file isn't there - which is problematic
            panelTop = new ControlPanel.Builder(ControlPanel.PanelPosition.TOP, this)
                    .addControl("stepSlower.png", callbackStepSlower)
                    .addControl("drawSlower.png", callbackDrawSlower)
                    .addControl("pause.png", "play.png", callbackPause)
                    .addControl("drawFaster.png", callbackDrawFaster)
                    .addControl("stepFaster.png", callbackStepFaster)
                    .alignment(ControlPanel.PanelAlignment.CENTER)
                    .sizeToFit(true)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<ControlPanel> panels = Arrays.asList(panelLeft, panelTop);

        mouseEventReceivers.addAll(panels);

        return panels;
    }

}
