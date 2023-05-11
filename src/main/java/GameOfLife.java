// import g4p_controls.G4P;
// import g4p_controls.GCustomSlider;

import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.core.PGraphics;

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
 * todo: splash message if nothing loaded... otherwise it's a blank canvas
 * todo: you can't rewind while there's a long operaation running - you'll have to queue it up
 * todo: somewhere on the screen show fade in the target step and the current step until they're one and the same and then fade out
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
 * todo: create a test for LifeDrawer that allows you to know if it actually is improved in performance
 * todo: doubleclick to zoom
 * todo: smooth zoom
 * todo: click for info
 * todo: directional big jump
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

    PGraphics buffer;

    ComplexCalculationHandler<Integer> complexCalculationHandlerSetStep;

    private int stepGuard = 0;
    ComplexCalculationHandler<Void> complexCalculationHandlerNextGeneration;

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
    private boolean running;
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

        life.setupField(newLife.field_x, newLife.field_y);

        drawer.center(life.getRootBounds(), true);
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
            buffer = createGraphics(width, height);
        }
        prevWidth = width;
        prevHeight = height;
    }

    public void draw() {


        Bounds bounds = life.getRootBounds();

        // result is null until a value has been passed in from a copy/paste or load of RLE (currently)
        if (lifeForm != null) {

            // notify all throttles (or any other code that needs the frameRate)
            // frameRateNotifier.notifyListeners(frameRate);

            if (updatingLife())
            {                
                image(buffer, 0, 0);
 
            } else {
                buffer.beginDraw();
                buffer.background(255);

                movementHandler.move(pressedKeys);

                // use this with LogPoints to tie how long redraw takes with and without bounds or with and without image cache (which was probably an unnecessaary optimization)
                // final long startTime = System.nanoTime();


                // make it so
                drawer.redraw(life.root, buffer);

                if (displayBounds) {
                    drawer.drawBounds(bounds, buffer);
                }

                // drawHUD is in this class - probably it would be better to put all drawing related items in
                // the drawer, and then have it maintain its own buffer - esp. when reize events happen
                drawHUD(bounds);

                if (countdownText != null) {
                    countdownText.update();
                    countdownText.draw();
                }

                buffer.endDraw();

                image(buffer, 0, 0);

            }

        }

        goForwardInTime();

    }


    // possibly this will help when returning from screensaver
    public void focusGained() {
        //println("Focus gained");
        redraw();
    }



    private void goForwardInTime() {

        // don't start anything complex if we're not running
        if (!running) return;

        if (shouldStartComplexCalculationSetStep()) {
            int step = life.step;
            step += (step < targetStep) ? 1 : -1;
            complexCalculationHandlerSetStep.startCalculation(step, (p, v) -> onCalculationSetStepComplete(p));
        }

        if (shouldStartComplexCalculationNextGeneration()) {
            complexCalculationHandlerNextGeneration.startCalculation(null, (v, result) -> onCalculationNextGenerationComplete());
        }
    }

    private void onCalculationNextGenerationComplete() {
        //if (frameCount % 300 == 0)
        //  System.out.println("nextGeneration complete " + frameCount);
    }

    private void onCalculationSetStepComplete(Integer step) {
        if (life.step==step) {
            println("step updated to: " + step + " out of " + targetStep);
            stepGuard = 30; // always wait 30 between steps for smoother animation
        }
        else {
            // todo: could you move this guard into complexcalculationhandler so that it keeps track
            // of backoff interval when things don't work?
            stepGuard = 60; // wit 60 if you've pushed it too much
        }
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

       /* if (setStepRunning)
            println("setStep is running, wait");

        if (nextGenerationRunning)
            println("nextGeneration is running, wait"); */

        return (nextGenerationRunning || setStepRunning);

    }

    private boolean shouldStartComplexCalculationSetStep() {
        // stepGuard sets an amount of frames to wait until attempting to call
        // set step again in case we've been too aggressive and have excced the leevl
        // of the tree with our step requests
        if (stepGuard > 0) {
            stepGuard--;
            return false;
        }

        // if we're not running a complex task and we're expecting to advance forward in time:
        return (!updatingLife() && (life.step < targetStep));

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

        buffer.fill(0);
        float hudTextSize = 24;
        buffer.textSize(hudTextSize);

        float hudMargin = 10;

        while ((buffer.textWidth(hud) + (2 * hudMargin) > buffer.width) || ((buffer.textAscent() + buffer.textDescent()) + (2 * hudMargin) > buffer.height)) {
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

                // parses and displays the lifeform
                parseStoredLife();

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

            countdownText = new CountdownText(this, buffer, this::run, this::stop, "counting down - press space to begin immediately: ");
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
            drawer.center(life.getRootBounds(),false);
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
        drawer.center(life.getRootBounds(), true);
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

    /* private final KeyCallback callbackPasteWindows = new KeyCallback('v', KeyEvent.CTRL, KeyCallback.NON_MAC) {
        @Override
        public void onKeyEvent(KeyEvent event) {
            pasteLifeForm();
        }

        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    }; */

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
