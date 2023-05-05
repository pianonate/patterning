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
 * todo: with help - change ProcessingKeyCallback to work with KeyData key, modifiers,validOS - but make simple mechanisms to create one
 * todo: show what level you have zoomed to using fade in face out text on screen
 * todo: same for rewinding
 * todo: what about drawing a box showing the edge of the universe (label it)
 * todo: create the mc in LifeDrawer suitable to the 2^1024 possible width - make it a constant so that you
 * todo: fix the cell_width so it is a proper multiple of the window size
 * todo: notification that you have pasted followed by the countdown text
 * todo: single step mode
 * todo: out of memory error
 * todo: use touch interface as it looks as if TOUCH is an enum in the KeyEvent class - maybe maybe... provide squeeze to zoom
 * todo: is it possible to bind keyboard shortcuts to methods?
 * todo: is it possible to
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
 */
public class GameOfLife extends PApplet {

    public static void main(String[] args) {

        PApplet.main("GameOfLife");
        System.out.println("testing buildfirst");
    }

    private LifeUniverse life;
    private LifeDrawer drawer;

    float last_mouse_x;
    float last_mouse_y;

    float targetFrameRate = 30f;
    float lowerFrameRateThreshold = 20f;
    float higherFrameRateThreshold = 25f;
    boolean generateNewGenerations = true;

    int framesToSkip = 0;
    int currentSkip = 0;
    int scalingFactor = 0;
    int recoveryThreshold = 1000 / (int) targetFrameRate; // in milliseconds
    int bigPauseThreshold = 5000; // in milliseconds
    int lastRecoveryTime;


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


    void setupPattern() {

        // todo: you have to set the step size here and in life.clearPattern() to 0
        // so... that's not encapsulated..
        this.targetStep = 0;
        life.setStep(0);
        life.clearPattern();
        Bounds bounds = life.getBounds(lifeForm.field_x, lifeForm.field_y);
        life.setupField(lifeForm.field_x, lifeForm.field_y, bounds);
        drawer.fit_bounds(life.getRootBounds());
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

        this.fitted = false;
        this.targetStep = 0;
        this.displayBounds = false;


        KeyHandler keyHandler = new KeyHandler(this, life, drawer);


        this.hudInfo = new HUDStringBuilder();

        loadSavedWindowPositions();

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
        properties.setInt("width", frame.getWidth());
        properties.setInt("height", frame.getHeight());
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
            System.out.println("Initial Bounds: " + life.getRootBounds().toString());

            drawer.fit_bounds(life.getRootBounds());
            fitted = true;
        }
    }

    public void draw() {

        background(255);

        // result is null until a value has been passed in from a copy/paste or load of RLE (currently)
        if (lifeForm != null) {

            goForwardInTime();

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

    // don't try increasing steps or invoking nextGeneration unless things are going fast enough
    private void goForwardInTime() {
        if (!running) return;

        float currentFrameRate = frameRate;
        int currentTime = millis();
        int elapsedTime = currentTime - lastRecoveryTime;
        int pauseFactor = elapsedTime >= bigPauseThreshold ? 25 : 5;

        // Check if the current frame rate is below the lower threshold
        if (currentFrameRate < lowerFrameRateThreshold) {
            generateNewGenerations = false;

            if (elapsedTime >= recoveryThreshold) {
                scalingFactor += pauseFactor;
                framesToSkip += scalingFactor;
                lastRecoveryTime = currentTime;
            }

            println("frameRate too slow: " + frameRate
                    + " framesToSkip: " + framesToSkip
                    + " currentSkip: " + currentSkip
                    + " elapsedTime: " + elapsedTime
                    + " recoveryThreshold: " + recoveryThreshold
                    + " elapsedTime >= recoveryThreshold: " + (elapsedTime >= recoveryThreshold)
            );

        }

        // Check if the current frame rate is above the higher threshold
        if (currentFrameRate > higherFrameRateThreshold) {

            scalingFactor = 0;
            framesToSkip = 0;
            lastRecoveryTime = currentTime;

            if (!generateNewGenerations) {
                println("frameRate fast enough again: " + frameRate + " framesToSkip: " + framesToSkip + " currentSkip: " + currentSkip);
            }
            generateNewGenerations = true;
        }

        if (currentSkip >= framesToSkip) {

            // if (generateNewGenerations) {


            // smooth steps - once per frame regardless of
            // request rate from the keyhandler
            // todo - somwhere on the screen show
            //        fade in the target step and the current
            //        step until they're one and the same and then
            //        fade out
            int step = life.step;
            if (step != targetStep) {
                step += (step < targetStep) ? 1 : -1;
                life.setStep(step);
                println("step updated to: " + step + " out of " + targetStep);
            }
            currentSkip = 0;
        } else {
            // increase currentSkip until things are running fast enough
            currentSkip += 1;
        }

        life.nextGeneration();

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
            lifeForm = parser.parseRLE(storedLife);

            setupPattern();

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

        System.out.println(keyHandler.generateUsageText());
    }

    // Implement the getActionDescription() method for the zoom callback
    private final ProcessingKeyCallback callbackZoomIn = new ProcessingKeyCallback(Set.of('+', '=')) {
        @Override
        public void onKeyEvent() {
            drawer.zoom_at(true, mouseX, mouseY);
        }

        @Override
        public String getUsageText() {
            return "zoom in";
        }
    };

    private final ProcessingKeyCallback callbackZoomOut = new ProcessingKeyCallback('-') {
        @Override
        public void onKeyEvent() {
            drawer.zoom_at(false, mouseX, mouseY);
        }

        @Override
        public String getUsageText() {
            return "zoom out";
        }
    };

    private final ProcessingKeyCallback callbackStepFaster = new ProcessingKeyCallback(']') {
        @Override
        public void onKeyEvent() {
            handleStep(true);
        }

        @Override
        public String getUsageText() {
            return "go faster";
        }

    };

    private final ProcessingKeyCallback callbackStepSlower = new ProcessingKeyCallback('[') {
        @Override
        public void onKeyEvent() {
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

    private final ProcessingKeyCallback callbackDisplayBounds = new ProcessingKeyCallback('b') {
        @Override
        public void onKeyEvent() {
            displayBounds = !displayBounds;
        }

        @Override
        public String getUsageText() {
            return "draw a rectangle around the part of the universe that is 'alive'";
        }
    };

    private final ProcessingKeyCallback callbackCenterView = new ProcessingKeyCallback('c') {
        @Override
        public void onKeyEvent() {
            drawer.center_view(life.getRootBounds());
        }

        @Override
        public String getUsageText() {
            return "center the view on the universe - regardless of its size";
        }
    };

    private final ProcessingKeyCallback callbackFitUniverseOnScreen = new ProcessingKeyCallback('f') {
        @Override
        public void onKeyEvent() {
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

    private final ProcessingKeyCallback callbackRewind = new ProcessingKeyCallback('r') {
        @Override
        public void onKeyEvent() {
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


    private final ProcessingKeyCallback callbackPaste = new ProcessingKeyCallback('v', KeyEvent.META, ProcessingKeyCallback.MAC) {
        @Override
        public void onKeyEvent() {
            pasteLifeForm();
        }

        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    };

    private final ProcessingKeyCallback callbackPasteWindows = new ProcessingKeyCallback('v', KeyEvent.CTRL, ProcessingKeyCallback.NON_MAC) {
        @Override
        public void onKeyEvent() {
            pasteLifeForm();
        }

        @Override
        public String getUsageText() {
            return "paste a new lifeform into the app - currently only supports RLE encoded lifeforms";
        }
    };

    private final ProcessingKeyCallback callbackPause = new ProcessingKeyCallback(' ') {
        @Override
        public void onKeyEvent() {
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
