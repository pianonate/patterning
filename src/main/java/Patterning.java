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
import java.util.stream.Stream;

/*
 * todo: noise (and maybe others) showing running:  https://gist.github.com/Bleuje/ae3662d67bea2e24092d64efe022ed4c
 *       https://necessarydisorder.wordpress.com/2017/11/15/drawing-from-noise-and-then-making-animated-loopy-gifs-from-there/
 *
 * todo: autofit to screenf
 * todo: show lifeForm stats in its slide out box
 * todo: notification that you have pasted with the name of the life form - above the countdown text and larger - with the name
 * todo: splash message "John Conway's Game Of Life" and if nothing loaded, tell'em what's happening
 * todo: magnifier over mouseX
 * todo: smooth combination of zoom/center
 * todo: decouple step management from speed management. use fast forward and rewind buttons to speed up slow down
 * todo: move all drawing into PatternDrawer
 * todo: somewhere on the screen show fade in the target step and the current
 *      step until they're one and the same and then fade out
 * todo: move imagery around cached images into the ImageCacheEntry routine
 * todo: binary bit array - clearing - too complicated - needs to be on automatic or you'll screw up
 * todo: grid out the screen based on the pressed number key so you can see what level of the tree is that grid
 * todo: add RLE parser tests that can double as tests for the app
 * todo: reorganize the code for cleanliness and testing with 4.0's help
 * todo: click on node and it will tell you the information about it at the last selected grid level (or something)
 *          - maybe it recurses up to show info about all levels nearby
 * todo: show what level you have zoomed to using fade in face out text on screen
 * todo: indicate you have just done a rewind
 * todo: label bounding box with actual universe size in pixels and meters based on current cellSize - compare to what % of the known universe this would be in size
 * todo: create the mc in PatternDrawer suitable to the 2^1024 possible width (maybe you don't need that) make it a constant so that you
 * todo: cache of Boolean array of any unchanged node that has visibility on screen of what is visible and what are its bounds and blast that out to the screen
 * todo: single step mode
 * todo: out of memory error
 * todo: use touch interface as it looks as if TOUCH is an enum in the KeyEvent class - maybe maybe... provide squeeze to zoom
 * todo: is it possible to bind keyboard shortcuts to methods?
 * todo: display keyboard shortcuts in a panel and allow for it to be moved around the screen
 * todo: move HUD to upper right with a panel with an expand/collapse
 * todo: display pasted in metadata in a HUD section
 * todo: smooth zoom - is that possible? seems to me it would have to be possible.
 * todo: detect periodic stability - it seems that the lastID stops growing in the model - is that the detector?
 * todo: Add mc parser support
 * todo: do you need to manage the size of the hashmap?
 * todo: possibly simplification create an alternate implementation - extend the hashmap class and override resize method to capture the timing of the resize
 * todo: here's what would be cool - zoom over a section - if your mouse is over a section of what's going on, you can see the details at a much higher zoom level
 * todo: save all pasted in valid RLEs in a folder. check if it's already there and if it's different.
 * todo: allow for creation and then saving as an RLE with associated metadata - from the same place where you allow editing
 * todo: allow for rotating the images for visual appeal
 * todo: copy / paste selections
 * todo: create a test for PatternDrawer that allows you to know if it actually is improved in performance
 * todo: double click to zoom
 * todo: smooth zoom
 * todo: click for info
 * todo: directional big jump
 * todo: file manager of RLEs
 * todo: undo mouse moves? would it make sense to people
 * todo: paste logic - move to separate class to handle - just for the sake of keeping Patterning clean
 */

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
    private static final String PROPERTY_FILE_NAME = "patterning_autosave.json";
    private static boolean running;

    public static boolean isRunning() {
        return running;
    }

    public static void toggleRun() {
        running = !running;
    }

    public static void run() {
        running = true;
    }

    public static void main(String[] args) {
        PApplet.main("Patterning");
    }
    private ComplexCalculationHandler<Integer> complexCalculationHandlerSetStep;
    private ComplexCalculationHandler<Void> complexCalculationHandlerNextGeneration;
    private LifeUniverse life;
    private PatternDrawer drawer;
    private float last_mouse_x;
    private float last_mouse_y;

    // new instances only created in instantiateLife to keep things simple
    // lifeForm not made local as it is intended to be used with display functions in the future
    @SuppressWarnings("unused")
    private LifeForm lifeForm;
    private String storedLife;
    // used for resize detection
    private int prevWidth, prevHeight;
    // right now this is the only way that life gets into the game
    private int targetStep;


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
        ResourceReader r = new ResourceReader();
        try {
            this.storedLife = r.getRandomResourceAsString("rle");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void setup() {

        setupKeyHandler();

        surface.setResizable(true);

        frameRate(30f);

        this.drawer = new PatternDrawer(this);

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

        // initial height in case we resize
        prevWidth = width;
        prevHeight = height;

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


        if (prevWidth != width || prevHeight != height) {
            // moral equivalent of a resize
            drawer.surfaceResized(width, height);
        }
        prevWidth = width;
        prevHeight = height;

        if (updatingLife()) {

            drawer.displayBufferedImage();

        } else {

            drawer.draw(life);

        }

        goForwardInTime();

    }

    // possibly this will help when returning from screensaver
    // which had a problem that one time
    public void focusGained() {
        redraw();
    }


    /*
     * everything related to keyboard shortcuts
     */

    private void goForwardInTime() {

        // don't start anything complex if we're not running
        if (!running)
            return;

        if (shouldStartComplexCalculationSetStep()) {
            int step = life.step;
            step += (step < targetStep) ? 1 : -1;
            complexCalculationHandlerSetStep.startCalculation(step, (p, v) -> onCalculationSetStepComplete(p));
            return;
        }

        if (shouldStartComplexCalculationNextGeneration()) {
            complexCalculationHandlerNextGeneration.startCalculation(null, (v, result) -> onCalculationNextGenerationComplete());
        }
    }

    private void onCalculationNextGenerationComplete() {
        // currently nothing on calculation complete - so maybe the complex calculation handlers don't need to do anything
    }

    @SuppressWarnings("unused")
    private void onCalculationSetStepComplete(Integer step) {

        // this one used to have an ending function but i got rid of it
        // todo: determine whether we have any returns from complexCalculationHandlers...
        // between this one and onCalculationNextGenerationComplete it demonstrates two ways
        // that a result can be returned (or not) from a complex calculation
        // but maybe we don't need them at all...
    }

    private void performComplexCalculationSetStep(Integer step) {
        life.setStep(step);
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
        // if (stepSlider.hasFocus()) return;
        last_mouse_x += mouseX;
        last_mouse_y += mouseY;
    }

    public void mouseReleased() {
        // if (stepSlider.hasFocus()) return;
        last_mouse_x = 0;
        last_mouse_y = 0;
    }

    public void mouseDragged() {

        float dx = Math.round(mouseX - last_mouse_x);
        float dy = Math.round(mouseY - last_mouse_y);

        drawer.move(dx, dy);

        last_mouse_x += dx;
        last_mouse_y += dy;
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

            running = false;

            life = new LifeUniverse();

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

    private void setupKeyHandler() {

        KeyHandler keyHandler = new KeyHandler(this);
        // order matters - put them in the order you want them to show in getUsageText()
        keyHandler.addKeyCallback(callbackPause);
        keyHandler.addKeyCallback(callbackZoomIn);
        keyHandler.addKeyCallback(callbackZoomOut);
        keyHandler.addKeyCallback(callbackStepFaster);
        keyHandler.addKeyCallback(callbackStepSlower);
        keyHandler.addKeyCallback(callbackDisplayBounds);
        keyHandler.addKeyCallback(callbackCenterView);
        keyHandler.addKeyCallback(callbackFitUniverseOnScreen);
        keyHandler.addKeyCallback(callbackUndoCenter);
        keyHandler.addKeyCallback(callbackRandomLife);
        keyHandler.addKeyCallback(callbackRewind);
        keyHandler.addKeyCallback(callbackPaste);
        keyHandler.addKeyCallback(callbackMovement);

        System.out.println(keyHandler.getUsageText());

    }

    private final KeyCallback callbackMovement = new KeyCallback(Stream.of(PApplet.LEFT, PApplet.RIGHT, PApplet.UP, PApplet.DOWN)
            .map(KeyCombo::new)
            .toArray(KeyCombo[]::new)) {
        private boolean pressed = false;

        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
            if (!pressed) {
                pressed = true;
                // we only want to save the undo state for key presses when we start them
                // no need to save again until they're all released
                drawer.saveUndoState();
            }
        }

        @SuppressWarnings("unused")
        @Override
        public void onKeyRelease(KeyEvent event) {
            if (KeyHandler.getPressedKeys().size() == 0) {
                pressed = false;
            }
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "use arrow keys to move the image around. hold down two keys to move diagonally";
        }
    };
    private final KeyCallback callbackZoomIn = new KeyCallback(
            new KeyCombo(SHORTCUT_ZOOM_IN),
            new KeyCombo(SHORTCUT_ZOOM_IN, KeyEvent.SHIFT)
    ) {
        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
            drawer.zoomXY(true, mouseX, mouseY);
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "zoom in";
        }
    };
    private final KeyCallback callbackZoomOut = new KeyCallback(SHORTCUT_ZOOM_OUT) {
        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
            drawer.zoomXY(false, mouseX, mouseY);
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "zoom out";
        }
    };
    private final KeyCallback callbackDisplayBounds = new KeyCallback(SHORTCUT_DISPLAY_BOUNDS) {
        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
            drawer.toggleDrawBounds();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "draw a rectangle around the part of the universe that is 'alive'";
        }
    };
    private final KeyCallback callbackCenterView = new KeyCallback(SHORTCUT_CENTER) {
        @Override
        public void onKeyPress(KeyEvent event) {
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
        public void onKeyPress(KeyEvent event) {
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
        public void onKeyPress(KeyEvent event) {
            fitUniverseOnScreen();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "fit the visible universe on screen and center it";
        }
    };
    private final KeyCallback callbackPause = new KeyCallback(SHORTCUT_PAUSE) {
        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
            // the encapsulation is messy to ask the drawer to stop displaying countdown text
            // and just continue running, or toggle the running state...
            // but CountdownText already reaches back to Patterning.run()
            // so there aren't that many complex paths to deal with here...
            drawer.handlePause();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "press space to pause and unpause";
        }
    };

    private void handleStep(boolean faster) {

        int increment = (faster) ? 1 : -1;

        if (this.targetStep + increment < 0)
            increment = 0;
        this.targetStep += increment;

        String fasterOrSlower = (faster) ? "faster requested" : "slower requested";
        System.out.println(fasterOrSlower + ", targetStep: " + this.targetStep);

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

    private final KeyCallback callbackStepFaster = new KeyCallback(SHORTCUT_STEP_FASTER) {
        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
            handleStep(true);
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "go faster";
        }

    };
    private final KeyCallback callbackStepSlower = new KeyCallback(SHORTCUT_STEP_SLOWER) {
        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
            handleStep(false);
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "go slower";
        }

    };
    private final KeyCallback callbackRewind = new KeyCallback(
            new KeyCombo(SHORTCUT_REWIND, KeyEvent.META, ValidOS.MAC),
            new KeyCombo(SHORTCUT_REWIND, KeyEvent.CTRL, ValidOS.NON_MAC)
    ) {
        @SuppressWarnings("unused")
        @Override
        public void onKeyPress(KeyEvent event) {
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
        public void onKeyPress(KeyEvent event) {

            destroyAndCreate(true);

        }

        @SuppressWarnings("unused")
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
        public void onKeyPress(KeyEvent event) {
            pasteLifeForm();
        }

        @SuppressWarnings("unused")
        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    };
}
