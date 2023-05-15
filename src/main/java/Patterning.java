import processing.core.PApplet;
import processing.core.PGraphics;
import processing.data.JSONObject;
import processing.event.KeyEvent;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.Set;

/*
 * Test approach - add unit tests if something fails
 *               - add unit tests for anything that doesn't have a visible UX - but just when you're working on it
 *               - i.e., if you're trying to change LifeUniverse, then add tests around what you are trying to change
 *
 * todo: rewind - put it on Command-R and make R for load random
 * todo: notification that you have pasted with the name of the life form - above the countdown text and larger - with the name
 * todo: splash message "John Conway's Game Of Life" and if nothing loaded, tell'em what's happening
 * todo: magnifier over mouseX
 * todo: smooth combination of zoom/center
 * todo: decouple step management from speed management. use fast forward and rewind buttons to speed up slow down
 * todo: move all drawing into LifeDrawer
 * todo: put undo under command-z...also follow up on making ctrl-z, command-z be part of a
 *       combination key to the same KeyHandler as right now it's not setup to work with different commands for the same thing
 *       on different OS's correctly
 * todo: somewhere on the screen show fade in the target step and the current
 *      step until they're one and the same and then fade out
 * todo: move imagery around cached images into the ImageCacheEntry routine
 * todo: binary bit array - clearing - too complicated - needs to be on automatic or you'll screw up
 * todo: grid out the screen based on the pressed number key so you can see what level of the tree is that grid
 * todo: add RLE parser tests that can double as tests for the app
 * todo: reorganize the code for cleanliness and testing with 4.0's help
 * todo: click on node and it will tell you the information about it at the last selected grid level (or something) - mnaybe it recurses up to show info about all levels nearby
 * todo: with help - change KeyCallback to work with KeyData key, modifiers,validOS - but make simple mechanisms to create one
 * todo: show what level you have zoomed to using fade in face out text on screen
 * todo: indicate you have just done a rewind
 * todo: label bounding box with actual universe size in pixels and meters based on current cellSize - compare to what % of the known universe this would be in size
 * todo: create the mc in LifeDrawer suitable to the 2^1024 possible width (maybe you don't need that) make it a constant so that you
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
 * todo: load RLEs from a file
 * todo: save all pasted in valid RLEs in a folder. check if it's already there and if it's different.
 * todo: allow for creation and then saving as an RLE with associated metadata - from the same place where you allow editing
 * todo: allow for rotating the images for visual appeal
 * todo: copy / paste selections
 * todo: create a test for LifeDrawer that allows you to know if it actually is improved in performance
 * todo: doubleclick to zoom
 * todo: smooth zoom
 * todo: click for info
 * todo: directional big jump
 * todo: file manager of RLEs
 */

public class Patterning extends PApplet {

    private static final char SHORTCUT_REWIND = 'r'; // because this one is paired with Command
    private static final char SHORTCUT_RANDOM_FILE = 'l';

    public static void main(String[] args) {
        PApplet.main("Patterning");
    }

    // new instances onlycreated in instantiateLife to keep things simple
    private LifeUniverse life;
    private LifeDrawer drawer;

    private Set<Integer> pressedKeys;
    private MovementHandler movementHandler;

    PGraphics buffer;

    ComplexCalculationHandler<Integer> complexCalculationHandlerSetStep;

    ComplexCalculationHandler<Void> complexCalculationHandlerNextGeneration;

    float targetFrameRate = 30f;

    float last_mouse_x;
    float last_mouse_y;

    private HUDStringBuilder hudInfo;
    private Countdown countdownText;
    private boolean running;

    private LifeForm lifeForm;

    // private GCustomSlider stepSlider;

    // used for resize detection
    private int prevWidth, prevHeight;
    // right now this is the only way that life gets into the game
    private String storedLife;
    private static final String PROPERTY_FILE_NAME = "patterning_autosave.json";

    private int targetStep;
    private boolean displayBounds;

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

        if (null==this.storedLife || this.storedLife.isEmpty()) {
            getRandomLifeform();
        }

        // Set the window size
        size(width, height);

        noSmooth();

    }

    private void getRandomLifeform() {
        // todo: do you need to instantiate every time?  maybe that's fine...
        ResourceReader r = new ResourceReader();
        try {
            this.storedLife  = r.getRandomResourceAsString("rle");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void setup() {

        setupKeyHandler();

        surface.setResizable(true);

        // "pre" will get invoked before draw - using this to manage window resizing as
        // that's a useful thing to do only at that time
        registerMethod("pre", this);

        background(0);

        frameRate(targetFrameRate);

        this.drawer = new LifeDrawer(this, 4);
        this.movementHandler = new MovementHandler(this.drawer);

        buffer = createGraphics(width, height);

        complexCalculationHandlerSetStep = new ComplexCalculationHandler<>((p, v) -> {
            performComplexCalculationSetStep(p);
            return null;
        });
        complexCalculationHandlerNextGeneration = new ComplexCalculationHandler<>((v1, v2) -> {
            performComplexCalculationNextGeneration();
            return null;
        });

        this.targetStep = 0;
        this.displayBounds = false;

        this.hudInfo = new HUDStringBuilder();

        loadSavedWindowPositions();
        prevWidth = width;
        prevHeight = height;

        // good life was saved prior
        if (!(this.storedLife == null || this.storedLife.isEmpty())) {
            // invoke the logic you already have to reify this
            instantiateLifeform();
        }

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
        // oh baby - reify
        properties.setString("lifeForm", storedLife);

        saveJSONObject(properties, dataPath(PROPERTY_FILE_NAME));
    }

    // guaranteed by processing to be called prior to the draw phase
    // IDE doesn't see this as having a usage, but we know better
    @SuppressWarnings("unused")
    public void pre() {

        if (prevWidth != width || prevHeight != height) {
            // moral equivalent of a resize
            drawer.surfaceResized(width, height);
            buffer = createGraphics(width, height);
        }
        prevWidth = width;
        prevHeight = height;
    }

    public void draw() {

        Bounds bounds = life.getRootBounds();

        // result is null until a value has been passed in from a copy/paste or load of
        // RLE (currently)
        if (lifeForm != null) {

            // notify all throttles (or any other code that needs the frameRate)
            // frameRateNotifier.notifyListeners(frameRate);

            if (updatingLife()) {

                image(buffer, 0, 0);

            } else {
                // todo: move these into lifeDrawer - why should they be here?
                buffer.beginDraw();
                buffer.background(0);

                // i don't like that movementHandler has to have a lifeDrawer instance but for
                // now it can't be avoided
                movementHandler.move(pressedKeys);

                // use this with LogPoints to tie how long redraw takes with and without bounds
                // or with and without image cache (which was probably an unnecessaary
                // optimization)
                // final long startTime = System.nanoTime();

                // todo: move the displaybounds into the life drawer and simply
                // notify it from the keycomand that it should do so rather than
                // maintaining this state here
                if (displayBounds) {
                    drawer.drawBounds(bounds, buffer);
                }

                // make it so
                drawer.redraw(life.root, buffer);

                // drawHUD is in this class - probably it would be better to put all drawing
                // related items in
                // the drawer, and then have it maintain its own buffer - esp. when reize events
                // happen
                drawHUD(bounds);

                // another thing that the drawer shoudld handle
                if (countdownText.isDisplaying) {
                    // countdownText.update();
                    countdownText.draw();
                }

                // and another thing the drawer shoud handle...
                buffer.endDraw();
                image(buffer, 0, 0);

            }

        }

        goForwardInTime();

    }

    // possibly this will help when returning from screensaver
    public void focusGained() {
        // println("Focus gained");
        redraw();
    }

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
        // currently nothing on calculation complete - so maybe the complexcalculation handlers don't need to do anything
    }

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

        // if we're not running a complex task and we're expecting to change the step
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
        // turn off fit to window mode as we're dragging it and if 'f' is on it
        // will keep trying to bounce back
        // if (stepSlider.hasFocus()) return;

        float dx = Math.round(mouseX - last_mouse_x);
        float dy = Math.round(mouseY - last_mouse_y);

        drawer.move(dx, dy);

        last_mouse_x += dx;
        last_mouse_y += dy;
    }

    /*
     * public void onSliderChange(int value) {
     * println("slider changed:" + value);
     *
     * }
     */

    private void drawHUD(Bounds bounds) {

        Node root = life.root;

        hudInfo.addOrUpdate("fps", Math.round(frameRate));
        hudInfo.addOrUpdate("cell", drawer.getCellWidth());
        hudInfo.addOrUpdate("running", (running) ? "running" : "stopped");

        hudInfo.addOrUpdate("level: ", root.level);
        BigInteger bigStep = new BigInteger("2").pow(life.step);
        hudInfo.addOrUpdate("step", bigStep);
        hudInfo.addOrUpdate("generation", life.generation);
        hudInfo.addOrUpdate("population", root.population);
        hudInfo.addOrUpdate("maxLoad", life.maxLoad);
        hudInfo.addOrUpdate("lastID", life.lastId);

        hudInfo.addOrUpdate("width", bounds.right.subtract(bounds.left));
        hudInfo.addOrUpdate("height", bounds.bottom.subtract(bounds.top));

        buffer.textAlign(RIGHT, BOTTOM);
        // use the default delimiter
        String hud = hudInfo.getFormattedString(frameCount, 12);

        buffer.fill(255);
        float hudTextSize = 24;
        buffer.textSize(hudTextSize);

        float hudMargin = 10;

        while ((buffer.textWidth(hud) + (2 * hudMargin) > buffer.width)
                || ((buffer.textAscent() + buffer.textDescent()) + (2 * hudMargin) > buffer.height)) {
            hudTextSize--;
            hudTextSize = max(hudTextSize, 1); // Prevent the textSize from going below 1
            buffer.textSize(hudTextSize);
        }

        buffer.text(hud, buffer.width - hudMargin, buffer.height - 5);

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

            stop();

            life = new LifeUniverse();

            LifeFormats parser = new LifeFormats();
            LifeForm newLife = parser.parseRLE(storedLife);

            // todo: you have to set the step size here and in life.clearPattern() to 0
            // so... that's not encapsulated..
            targetStep = 0;
            life.setStep(0);

            life.setupField(newLife.field_x, newLife.field_y);

            Bounds bounds = life.getRootBounds();

            drawer.center(bounds, true, false);

            // this is tough to have to know - somehow we need to have the drawer
            // know when newLife comes into existence so we don't have to
            // remember to clear it's cache...
            drawer.clearCache();

            life.saveRewindState();
            lifeForm = newLife;

            countdownText = new Countdown(buffer, this::run, this::run, "counting down - press space to begin immediately");
            countdownText.startCountdown();

        } catch (NotLifeException e) {
            // todo: on failure you need to
            println("get a life - here's what failed:\n\n" + e.getMessage());
        }
    }

    public void run() {
        running = true;
    }

    public void stop() {
        targetStep = 0;
        running = false;
    }

    /*
     * everything related to keyboard shortcuts
     */

    private void setupKeyHandler() {

        KeyHandler keyHandler = new KeyHandler(this);

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

        System.out.println(keyHandler.generateUsageText());
    }

    private final KeyCallback callbackMovement = new KeyCallbackMovement() {
        @Override
        public void onKeyEvent(KeyEvent event) {

        }

        @Override
        public void move(Set<Integer> pressedKeys) {
            drawer.saveUndoState(); // before we get a lot more events
            Patterning.this.pressedKeys = pressedKeys;
        }

        @Override
        public void stopMoving() {
            Patterning.this.pressedKeys = null;
            Patterning.this.movementHandler.stop();
        }

        @Override
        public String getUsageText() {
            return "use arrow keys to move the image around. hold down two keys to move diagonally";
        }
    };

    // Implement the getActionDescription() method for the zoom callback
    private final KeyCallback callbackZoomIn = new KeyCallback(Set.of('+', '=')) {
        @Override
        public void onKeyEvent(KeyEvent event) {
            drawer.zoomXY(true, mouseX, mouseY);
        }

        @Override
        public String getUsageText() {
            return "zoom in";
        }
    };

    private final KeyCallback callbackZoomOut = new KeyCallback('-') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            drawer.zoomXY(false, mouseX, mouseY);
        }

        @Override
        public String getUsageText() {
            return "zoom out";
        }
    };

    private final KeyCallback callbackStepFaster = new KeyCallback(']') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            handleStep(true);
        }

        @Override
        public String getUsageText() {
            return "go faster";
        }

    };

    private final KeyCallback callbackStepSlower = new KeyCallback('[') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            handleStep(false);
        }

        @Override
        public String getUsageText() {
            return "go slower";
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

    private final KeyCallback callbackDisplayBounds = new KeyCallback('b') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            displayBounds = !displayBounds;
        }

        @Override
        public String getUsageText() {
            return "draw a rectangle around the part of the universe that is 'alive'";
        }
    };

    private final KeyCallback callbackCenterView = new KeyCallback('c') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            drawer.center(life.getRootBounds(), false, true);
        }

        @Override
        public String getUsageText() {
            return "center the view on the universe - regardless of its size";
        }
    };

    private final KeyCallback callbackUndoCenter = new KeyCallback('u') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            drawer.undoMovement();
        }

        @Override
        public String getUsageText() {
            return "undo the last center or fit bounds to return to the last view";
        }
    };

    private final KeyCallback callbackFitUniverseOnScreen = new KeyCallback('f') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            fitUniverseOnScreen();
        }

        @Override
        public String getUsageText() {
            return "fit the visible universe on screen and center it";
        }
    };

    private void fitUniverseOnScreen() {
        drawer.center(life.getRootBounds(), true, true);
    }

    private final KeyCallback callbackRewind = new KeyCallback(SHORTCUT_REWIND, KeyEvent.META, KeyCallback.MAC) {
        @Override
        public void onKeyEvent(KeyEvent event) {
            destroyAndCreate(false);
        }

        @Override
        public String getUsageText() {
            return "rewind the current life form back to generation 0";
        }
    };

    private final KeyCallback callbackRandomLife = new KeyCallback(SHORTCUT_RANDOM_FILE) {
        @Override
        public void onKeyEvent(KeyEvent event) {

            destroyAndCreate(true);

        }

        @Override
        public String getUsageText() {
            return "get a random life form from the built-in library";
        }
    };

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


    private final KeyCallback callbackPaste = new KeyCallback('v', KeyEvent.META, KeyCallback.MAC) {
        @Override
        public void onKeyEvent(KeyEvent event) {
            pasteLifeForm();
        }

        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    };



    /*
     * private final KeyCallback callbackPasteWindows = new KeyCallback('v',
     * KeyEvent.CTRL, KeyCallback.NON_MAC) {
     *
     * @Override
     * public void onKeyEvent(KeyEvent event) {
     * pasteLifeForm();
     * }
     *
     * @Override
     * public String getUsageText() {
     * return
     * "paste a new lifeform into the app - currently only supports RLE encoded lifeforms"
     * ;
     * }
     * };
     */

    private final KeyCallback callbackPause = new KeyCallback(' ') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            if (countdownText.isDisplaying) {
                countdownText.interruptCountdown();
            } else {
                running = !running;
            }
        }

        @Override
        public String getUsageText() {
            return "press space to pause and unpause";
        }
    };

}
