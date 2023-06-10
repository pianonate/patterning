package patterning;

import actions.MouseEventManager;
import processing.core.PApplet;
import processing.data.JSONObject;
import ux.DrawRateManager;
import ux.PatternDrawer;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Patterning extends PApplet {
    private static final String PROPERTY_FILE_NAME = "patterning_autosave.json";
    public boolean draggingDrawing = false;
    private LifeUniverse life;
    private DrawRateManager drawRateManager;
    private ComplexCalculationHandler<Integer> complexCalculationHandlerSetStep;
    private ComplexCalculationHandler<Void> complexCalculationHandlerNextGeneration;
    private final MouseEventManager mouseEventManager = MouseEventManager.getInstance();
    private PatternDrawer drawer;
    // used to control dragging the image around the screen with the mouse
    private float last_mouse_x;
    private float last_mouse_y;
    // used to control whether drag behavior should be invoked
    // when a mouse has been pressed over a mouse event receiver
    private String storedLife;
    private int targetStep;
    private boolean running;
    private boolean mousePressedOverReceiver = false;
    private boolean singleStepMode = false;

    public static void main(String[] args) {
        PApplet.main("patterning.Patterning");
    }

    public boolean isRunning() {
        return running;
    }

    public void toggleRun() {
        running = !running;
    }

    public void run() {
        running = true;
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

        surface.setResizable(true);

        frameRate(DrawRateManager.MAX_FRAME_RATE);

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

        this.drawRateManager = DrawRateManager.getInstance();

        this.drawer = new PatternDrawer(this, drawRateManager);

        // on startup, storedLife may be loaded from the properties file but if it's not
        // just get a random one
        if (null == this.storedLife || this.storedLife.isEmpty()) {
            getRandomLifeform(false);
        }

        // life will have been loaded in prior - either from saved life
        // or from the packaged resources so this doesn't need extra protection
        instantiateLifeform();

    }

    public void draw() {


        // we want independent control over how often we update and display
        // the next generation of drawing
        // the frameRate can and should run faster so the user experience is responsive
        drawRateManager.adjustDrawRate(frameRate);

        boolean shouldDraw = drawRateManager.shouldDraw();

        boolean shouldDrawLifeForm = shouldDraw && lifeIsThreadSafe();

        // goForwardInTime (below) requests a nextGeneration and or a step change
        // both of which can take a while
        // given they operate on a separate thread for performance reasons
        // we don't want to put them before the draw - it will almost always be showing
        // that it is updatingLife().  so we need to put goForwardInTime _after_ the request to draw
        // and we tell the drawer whether it is still updating life since the last frame
        // we also tell the drawer whether the drawRateController thinks that it's time to draw the life form
        // in case the user has slowed it down a lot to see what's going on, it's okay for it to be going slow

        drawer.draw(life, shouldDrawLifeForm);


        // as mentioned above - this runs on a separate thread
        // and we don't want it to go any faster than the draw rate throttling mechanism
        if (shouldDraw) {
            goForwardInTime();
        }
    }

    private boolean lifeIsThreadSafe() {

        // don't start if either of these calculations are currently running
        boolean setStepRunning = complexCalculationHandlerSetStep.isCalculationInProgress();
        boolean nextGenerationRunning = complexCalculationHandlerNextGeneration.isCalculationInProgress();

        return (!nextGenerationRunning && !setStepRunning);

    }

    private void goForwardInTime() {

        if (shouldStartComplexCalculationSetStep()) {
            int step = life.step;
            step += (step < targetStep) ? 1 : -1;
            complexCalculationHandlerSetStep.startCalculation(step);
            return;
        }

        // don't run generations if we're not running
        if (!running)
            return;

        if (shouldStartComplexCalculationNextGeneration()) {
            complexCalculationHandlerNextGeneration.startCalculation(null);
        }

        if (running && singleStepMode)
            toggleRun();
    }

    private boolean shouldStartComplexCalculationSetStep() {

        // if we're not running a complex task, and we're expecting to change the step
        return (lifeIsThreadSafe() && (life.step != targetStep));

    }

    // only start these if you're not running either one
    private boolean shouldStartComplexCalculationNextGeneration() {

        return lifeIsThreadSafe();
    }

    public void mousePressed() {
        last_mouse_x += mouseX;
        last_mouse_y += mouseY;

        assert mouseEventManager != null;
        mouseEventManager.onMousePressed();

        mousePressedOverReceiver = mouseEventManager.isMousePressedOverAnyReceiver();

        // If the mouse press is not over any MouseEventReceiver, we consider it as over the drawing.
        draggingDrawing = !mousePressedOverReceiver;
    }

    public void mouseReleased() {
        if (draggingDrawing) {
            draggingDrawing = false;
            // if the DPS is slow then the screen won't update correctly
            // so tell the system to draw immediately
            // we used to have drawImmediately() called in move
            // but it had a negative effect of freezing the screen while drawing
            // this is a good enough compromise as most of the time DPS is not really low
            drawRateManager.drawImmediately();
        } else {
            mousePressedOverReceiver = false;
            assert mouseEventManager != null;
            mouseEventManager.onMouseReleased();
        }
        last_mouse_x = 0;
        last_mouse_y = 0;
    }

    public void mouseDragged() {
        if (draggingDrawing) {
            float dx = Math.round(mouseX - last_mouse_x);
            float dy = Math.round(mouseY - last_mouse_y);

            drawer.move(dx, dy);

            last_mouse_x += dx;
            last_mouse_y += dy;
        }
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

    private void performComplexCalculationSetStep(Integer step) {
        life.setStep(step);
        // todo for some reason this needs to exist or maximum volatility gun goes nuts if you step too quickly
        drawer.clearUndoDeque();
    }

    private void performComplexCalculationNextGeneration() {
        life.nextGeneration();
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

    public void getRandomLifeform(boolean reset) {

        try {
            this.storedLife = ResourceManager.getInstance().getRandomResourceAsString(ResourceManager.RLE_DIRECTORY);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if (reset)
            destroyAndCreate();
    }

    private void instantiateLifeform() {

        try {
            // static on patterning.Patterning
            running = false;
            life = new LifeUniverse();

            // instance variables - do they need to be?
            LifeFormats parser = new LifeFormats();
            LifeForm newLife = parser.parseRLE(storedLife);

            targetStep = 0;
            life.setStep(0);

            life.setupField(newLife.field_x, newLife.field_y);

            // new instances only created in instantiateLife to keep things simple
            // lifeForm not made local as it is intended to be used with display functions in the future

            drawer.setupNewLife(life);


        } catch (NotLifeException e) {
            // todo: on failure you need to
            println("get a life - here's what failed:\n\n" + e.getMessage());
        }
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

    // either bring us back to the start on the current life form
    // or get a random one from the well...
    public void destroyAndCreate() {

        ComplexCalculationHandler.lock();
        try {

            instantiateLifeform();

        } finally {
            ComplexCalculationHandler.unlock();
        }
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

    public void handleStep(boolean faster) {

        int increment = (faster) ? 1 : -1;

        if (this.targetStep + increment < 0)
            increment = 0;
        this.targetStep += increment;
    }

    public void fitUniverseOnScreen() {
        drawer.center(life.getRootBounds(), true, true);
    }

    public void getNumberedLifeForm() {

        // subclasses of PApplet will have a keyCode
        // so this isn't magical
        int number = keyCode - '0';

        try {
            this.storedLife = ResourceManager.getInstance().getResourceAtFileIndexAsString(ResourceManager.RLE_DIRECTORY, number);
            destroyAndCreate();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public void centerView() {
        drawer.center(life.getRootBounds(), false, true);
    }

    public void toggleSingleStep() {
        if (running && !singleStepMode)
            running=false;

        singleStepMode = !singleStepMode;
    }
}