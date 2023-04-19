import processing.core.PApplet;
import processing.data.JSONObject;

import java.awt.*;

import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;

/**
 * DONE: don't start on paste - you want to look at it first
 * DONE: count down to start and allow for a space to interrupt it
 * todo: research "debounce"
 * todo: move with arrow keys
 * todo: move with mouse
 * todo: hairline grid drawing -toggle with the letter g
 * todo: display keyboard shortcuts in a panel and allow for it to be moved around the screen
 * todo: move HUD to upper right with a panel with an expander/collapser
 * todo: display pasted in metadata in a HUD section
 * todo: detect periodic stability - it seems that the lastID stops growing in the model - is that the detector?
 * todo: Add mc parser support
 * todo: do you need to manage the size of the hashmap?
 * todo: possibly simplification create an alternate implementation - extend the hashmap class and override resize method to capture the timing of the resize
 * todo: here's what would be cool - zoom over a section - if your mouse is over a section of what's going on, you can see the details at a much higher zoom level
 * todo: load RLEs from a file
 * todo: save all pasted in valid RLEs in a folder.  check if it's already there and if it's different.
 * todo: allow for creation and then saving as an RLE with associated metadata - from the same place where you allow editing
 */
public class GameOfLife extends PApplet {

    public static void main(String[] args) {

        PApplet.main("GameOfLife");
    }

    private LifeUniverse life;
    private LifeDrawer drawer;
    private Result result;
    private HUDStringBuilder hudInfo;
    private CountdownText countdownText;
    private boolean running;
    private boolean fitToWindow;
    // used for resize detection
    private int prevWidth, prevHeight;
    // right now this is the only way that life gets into the game
    private String storedLife;
    private static final String PROPERTY_FILE_NAME = "GameOfLife.json";

    void setupPattern() {

        life.clearPattern();
        Bounds bounds = life.getBounds(result.field_x, result.field_y);

        // the following was in the original but i'm not sure if it's needed right now...
        // life.makeCenter(result.field_x, result.field_y, bounds);

        life.setupField(result.field_x, result.field_y, bounds);

        // the following was in the original but you're not operating the same way so...
        // i think you can let it go but wait till you've done some other stuff to decide
        // bounds = life.getRootBounds();
        // drawer.fit_bounds(bounds);
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

        // "pre" will get invoked before draw - using this to manage window resizing as that's a useful thing to do only at that time
        registerMethod("pre", this);

        background(255);

        frameRate(120);

        running = false;
        fitToWindow = true;

        drawer = new LifeDrawer(this, 4);
        // create a new LifeUniverse object with the given points
        life = new LifeUniverse();

        hudInfo = new HUDStringBuilder();

        loadSavedWindowPositions();

        // good life was saved prior
        if (!(storedLife == null || storedLife.isEmpty())) {
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

    public void pre() {

        if (prevWidth != width || prevHeight != height) {
            // System.out.println("Window resized to: " + width + " x " + height);
            drawer.canvas_width = width;
            drawer.canvas_height = height;
        }
        prevWidth = width;
        prevHeight = height;
    }

    public void draw() {

        background(255);

        // result is null until a value has been passed in from a copy/paste or load of RLE (currently)
        if (result != null) {

            if (life.generation == 0) {
                life.saveRewindState();
            }

            if (running) {
                life.nextGeneration(true);
            }

            drawer.redraw(life.root);

            Bounds bounds = life.getRootBounds();

            // default is to constrain to screen size
            // but if people start using +/- to zoom in and out then
            // fit bounds stops until then f is invoked to "refit"
            if (fitToWindow)
                drawer.fit_bounds(bounds);

            if (countdownText != null) {
                countdownText.update();
                countdownText.draw();
            }

        }

        drawHUD();

    }

    public void keyPressed() {

        // Assuming key is of type char and running is a boolean variable
        switch (key) {
            case '+', '=' -> {
                // zoom in -
                // stop fitting to window first so just let it ride out
                fitToWindow = false;
                drawer.zoom_centered(false);
            }
            case '-' -> {
                // zoom out -
                // stop fitting to window first so just let it ride out
                fitToWindow = false;
                drawer.zoom_centered(true);
            }
            case 'F', 'f' -> fitToWindow = true;
            case ' ' -> {
                // it's been created and we're not in the process of counting down
                boolean que = countdownText!=null && !countdownText.isCountingDown;
                if (countdownText!=null && countdownText.isCountingDown) {
                    countdownText.interruptCountdown();
                } else {
                    running = !running;
                }


            }
            case 'V', 'v' -> {
                if (keyCode == 86) {
                    pasteBaby();
                }
            }
            default -> {
                // Handle other keys if needed
            }
        }

    }

    private void drawHUD() {

        Node root = life.root;

        hudInfo.addOrUpdate("fps", parseInt(frameRate));
        hudInfo.addOrUpdate("generation", parseInt(life.generation));
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

    private void pasteBaby() {

        try {
            // Get the system clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Check if the clipboard contains text data and then get it
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                storedLife = (String) clipboard.getData(DataFlavor.stringFlavor);

                parseStoredLife();
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
    }

    private void parseStoredLife() {

        try {

            running = false;

            Formats parser = new Formats();
            result = parser.parseRLE(storedLife);

            setupPattern();

            countdownText = new CountdownText(this, this::run, "counting down - press space to begin immediately: ");
            countdownText.startCountdown();

        } catch (NotLifeException e) {
            println("get a life - here's what failed:\n\n" + e.getMessage());
        }
    }

    public void run() {
        running = true;
    }

}
