// import g4p_controls.G4P;
// import g4p_controls.GCustomSlider;

import processing.core.PApplet;
import processing.event.KeyEvent;

import processing.data.JSONObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

import java.math.BigInteger;
import java.util.Set;

/**
 * todo: create a test for LifeDrawer that allows you to know if it actually is improved
 * todo: stop all throttling and try instead to use the complexCalculationHandler
 * todo: move imagery around cached images into the ImageCacheEntry routine
 * todo: binary bit array - clearing - too complicated - needs to be on automatic or you'll screw up
 * todo: grid out the screen based on the pressed number key so you can see what level of the tree is that grid
 * todo: add RLE parser tests that can double as tests for the app
 * todo: clean up todo's
 * todo: reorganize the code for cleanliness and testing with 4.0's help
 * todo: click on node and it will tell you the information about it at the last selected grid level (or something) - mnaybe it recurses up to show info about all levels nearby
 * todo: investigate making all sections of the screen build up from imageCacheEntries...with a LRU, shouldn't this be no problem?
 * todo: nextgeneration as well as setstep need to have a throttle back mechanism
 * todo: gracefully hahdle cell_width so that you can scale down to fitting something on screen again
 *       without having to sacrifice the bs
 * todo: with help - change KeyCallback to work with KeyData key, modifiers,validOS - but make simple mechanisms to create one
 * todo: show what level you have zoomed to using fade in face out text on screen
 * todo: same for rewinding
 * todo: what about drawing a box showing the edge of the universe (label it)
 * todo: create the mc in LifeDrawer suitable to the 2^1024 possible width - make it a constant so that you
 * todo: cache of Boolean array
 *      of any unchanged node that has visibility on screen of what is visible and what are its bounds and blast that out to the screen
 *          also should we have processing create the image offline and then show it?
 * todo: notification that you have pasted followed by the countdown text
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
 * todo: save all pasted in valid RLEs in a folder.  check if it's already there and if it's different.
 * todo: allow for creation and then saving as an RLE with associated metadata - from the same place where you allow editing
 * todo: allow for rotating the images for visual appeal
 * todo: copy / paste selections
 *
 */
public class GameOfLife extends PApplet {

    public static void main(String[] args) {
        PApplet.main("GameOfLife");
    }

    private LifeUniverse life;
    private LifeDrawer drawer;

    private Set<Integer> pressedKeys;
    private MovementHandler movementHandler;

    private FrameRateNotifier frameRateNotifier;

    float targetFrameRate = 30f;
    private ThrottleController setStepThrottle = new ThrottleController("setStep",
            20f,
            25f,
            1000 / (int) targetFrameRate);

    private ThrottleController nextGenerationThrottle = new ThrottleController("nextGeneration",
            5f,
            15f,
            1000 / (int) targetFrameRate);

    float last_mouse_x;
    float last_mouse_y;

    private HUDStringBuilder hudInfo;
    private CountdownText countdownText;
    private boolean running, fitted;
    // todo: refactor result to have a more useful name
    private LifeForm lifeForm;

    // private GCustomSlider stepSlider;

    // used for resize detection
    private int prevWidth, prevHeight;
    // right now this is the only way that life gets into the game
    private String storedLife;
    private static final String PROPERTY_FILE_NAME = "GameOfLife.json";

    private int targetStep;
    private boolean displayBounds;


    void instantiateLifeForm(LifeForm newLife) {

        // todo: you have to set the step size here and in life.clearPattern() to 0
        // so... that's not encapsulated..
        this.targetStep = 0;
        life.setStep(0);
        life.clearPattern();
        Bounds bounds = life.getBounds(newLife.field_x, newLife.field_y);
        life.setupField(newLife.field_x, newLife.field_y, bounds);

        drawer.fit_bounds(life.getRootBounds());
        // this is tough to have to know - somehow we need to have the drawer
        // know when newLife comes into existence so we don't have to
        // remember to clear it's cache...
        drawer.clearCache();

        life.saveRewindState();
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

        // Set the window size
        size(width, height);

        noSmooth();


    }

    public void setup() {

        setupKeyHandler();

        surface.setResizable(true);

        // "pre" will get invoked before draw - using this to manage window resizing as that's a useful thing to do only at that time
        registerMethod("pre", this);

        background(255);

        frameRate(targetFrameRate);

        // create a new LifeUniverse object with the given points
        this.life = new LifeUniverse();
        this.drawer = new LifeDrawer(this, 4);
        this.movementHandler = new MovementHandler(this.drawer);

        this.frameRateNotifier = new FrameRateNotifier();
        frameRateNotifier.addListener(setStepThrottle);
        frameRateNotifier.addListener(nextGenerationThrottle);

        this.fitted = false;
        this.targetStep = 0;
        this.displayBounds = false;

        this.hudInfo = new HUDStringBuilder();

        loadSavedWindowPositions();
        prevWidth = width;
        prevHeight = height;

        // good life was saved prior
        if (!(this.storedLife == null || this.storedLife.isEmpty())) {
            // invoke the logic you already have to reify this
            parseStoredLife();
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
        Frame frame;

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

        // this took a lot of chatting with GPT 4.0 to finally land on something that would work
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
    public void pre() {

        if (prevWidth != width || prevHeight != height) {
            // moral equivalent of a resize
            drawer.surfaceResized(width, height);

        }
        prevWidth = width;
        prevHeight = height;

        // putting this here to not clutter the main draw as it should only happen at startup
        // maybe because of loading saved window location it gets different height values from the
        // first call to fit_bounds than after the app is launched
        // so calling it once here makes sense
        // there has to be a more elegant way to get the window value correct the first time
        // but for now, here it is. if you can get the window value correct you could put this
        // in the setupPattern method where it belongs
        if (!fitted) {
            drawer.fit_bounds(life.getRootBounds());
            fitted = true;
        }
    }

    public void draw() {

        background(255);

        // result is null until a value has been passed in from a copy/paste or load of RLE (currently)
        if (lifeForm != null) {

            // notify all throttles (or any other code that needs the frameRate)
            frameRateNotifier.notifyListeners(frameRate);

            goForwardInTime();

            movementHandler.move(pressedKeys);

            // make it so
            drawer.redraw(life.root);

            if (displayBounds) {
                drawer.draw_bounds(life.getRootBounds());
            }

            if (countdownText != null) {
                countdownText.update();
                countdownText.draw();
            }

        }

        // always draw HUD
        drawHUD();
    }

    private void goForwardInTime() {
        if (!running) return;

        int step = life.step;

        if (step != targetStep) {

            if (setStepThrottle.shouldProceed()) {
                // smooth steps - once per frame unless
                //                throttling is preventing that
                // todo - somwhere on the screen show
                //        fade in the target step and the current
                //        step until they're one and the same and then
                //        fade out
                step += (step < targetStep) ? 1 : -1;
                life.setStep(step);
                drawer.clearCache();
                println("step updated to: " + step + " out of " + targetStep);
            }
        }

        if (nextGenerationThrottle.shouldProceed()) {
            life.nextGeneration();
        }
    }

    public void mousePressed() {
        //  if (stepSlider.hasFocus()) return;
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
        //if (stepSlider.hasFocus()) return;

        float dx = Math.round(mouseX - last_mouse_x);
        float dy = Math.round(mouseY - last_mouse_y);

        drawer.move(dx, dy);

        last_mouse_x += dx;
        last_mouse_y += dy;
    }

   /* public void onSliderChange(int value) {
        println("slider changed:" + value);

    }*/

    private void drawHUD() {

        Node root = life.root;

        hudInfo.addOrUpdate("fps", Math.round(frameRate));
        hudInfo.addOrUpdate("cell", drawer.getCell_width());
        hudInfo.addOrUpdate("running", (running) ? "running" : "stopped");

        hudInfo.addOrUpdate("level: ", life.root.level);
        BigInteger bigStep = new BigInteger("2").pow(life.step);
        hudInfo.addOrUpdate("step", bigStep);
        hudInfo.addOrUpdate("generation", life.generation);
        hudInfo.addOrUpdate("population", root.population);
        hudInfo.addOrUpdate("maxLoad", life.maxLoad);
        hudInfo.addOrUpdate("lastID", life.lastId);

        textAlign(RIGHT, BOTTOM);
        // use the default delimiter
        String hud = hudInfo.getFormattedString(frameCount, 12);

        fill(0);
        float hudTextSize = 24;
        textSize(hudTextSize);

        float hudMargin = 10;

        while ((textWidth(hud) + (2 * hudMargin) > width) || ((textAscent() + textDescent()) + (2 * hudMargin) > height)) {
            hudTextSize--;
            hudTextSize = max(hudTextSize, 1); // Prevent the textSize from going below 1
            textSize(hudTextSize);
        }

        text(hud, width - hudMargin, height - 5);

    }

    public void pasteLifeForm() {

        try {
            // Get the system clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Check if the clipboard contains text data and then get it
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                storedLife = (String) clipboard.getData(DataFlavor.stringFlavor);

                // parses and displays the lifeform
                parseStoredLife();

                // todo: it would be better if this could be called from setupPattern
                // or parseStoredLife - it's a drag as this seems like duplication
                // look at the comment in the pre method for more info
                // drawer.fit_bounds(life.getRootBounds());
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
    }

    private void parseStoredLife() {

        try {

            stop();

            Formats parser = new Formats();
            LifeForm newLife = parser.parseRLE(storedLife);

            instantiateLifeForm(newLife);

            lifeForm = newLife;

            countdownText = new CountdownText(this, this::run, this::stop, "counting down - press space to begin immediately: ");
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

        ProcessingKeyHandler keyHandler = new ProcessingKeyHandler(this);

        keyHandler.addKeyCallback(callbackPause);
        keyHandler.addKeyCallback(callbackZoomIn);
        keyHandler.addKeyCallback(callbackZoomOut);
        keyHandler.addKeyCallback(callbackStepFaster);
        keyHandler.addKeyCallback(callbackStepSlower);
        keyHandler.addKeyCallback(callbackDisplayBounds);
        keyHandler.addKeyCallback(callbackCenterView);
        keyHandler.addKeyCallback(callbackFitUniverseOnScreen);
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
            GameOfLife.this.pressedKeys = pressedKeys;
        }

        @Override
        public void stopMoving() {
            GameOfLife.this.pressedKeys = null;
            GameOfLife.this.movementHandler.stop();
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
            drawer.zoom_at(true, mouseX, mouseY);
        }

        @Override
        public String getUsageText() {
            return "zoom in";
        }
    };

    private final KeyCallback callbackZoomOut = new KeyCallback('-') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            drawer.zoom_at(false, mouseX, mouseY);
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

        if (this.targetStep + increment < 0) increment = 0;
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
            drawer.center_view(life.getRootBounds());
        }

        @Override
        public String getUsageText() {
            return "center the view on the universe - regardless of its size";
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
        drawer.fit_bounds(life.getRootBounds());
    }

    private final KeyCallback callbackRewind = new KeyCallback('r') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            stop();
            life.restoreRewindState();

            // todo: stop() also sets the targetStep to 0, feels as if this should be refactored...
            //       wait until you bring spacebarhandler over - which i think also uses stop()
            life.setStep(0);
            fitUniverseOnScreen();
        }

        @Override
        public String getUsageText() {
            return "rewind the current life form back to generation 0";
        }
    };


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

    private final KeyCallback callbackPasteWindows = new KeyCallback('v', KeyEvent.CTRL, KeyCallback.NON_MAC) {
        @Override
        public void onKeyEvent(KeyEvent event) {
            pasteLifeForm();
        }

        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    };

    private final KeyCallback callbackPause = new KeyCallback(' ') {
        @Override
        public void onKeyEvent(KeyEvent event) {
            if (countdownText != null && countdownText.isCountingDown) {
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
