import processing.core.PApplet;
import processing.data.JSONObject;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;

/**
 * todo
 * initial visualization
 * Add mc parser support
 * do you need to manage the size of the hashmap?
 * create an alternate implementation - extend the hashmap class and override resize method to capture the timing of the resize
 */
public class GameOfLife extends PApplet {

    public static void main(String[] args) {

        PApplet.main("GameOfLife");
    }

    private LifeUniverse life;
    private LifeDrawer drawer;
    private Result result;

    private HUDInfo hudInfo;

    private boolean running;
    private boolean DEBUG_MODE = false;

    private int prevWidth, prevHeight, lastFrameCount, lastFrameRate;

    void setupPattern() {
        life.clearPattern();
        Bounds bounds = life.getBounds(result.field_x, result.field_y);
        life.makeCenter(result.field_x, result.field_y, bounds);
        life.setupField(result.field_x, result.field_y, bounds);

        bounds = life.getRootBounds();
        drawer.fit_bounds(bounds);
    }

    public void settings() {
        // on startup read the size from the window.json file
        // eventually find a better place for it - by default it is putting it in
        // documents/data - maybe you hide it somewhere useful
        JSONObject properties;
        String propertiesFileName = "window.json";
        // this dataPath() was apparently the required way to save and find it
        File propertiesFile = new File(dataPath(propertiesFileName));

        int width = 800, height = 800;

        if (propertiesFile.exists()) {
            // Load window size from JSON file
            properties = loadJSONObject(dataPath(propertiesFileName));
            width = properties.getInt("width", width);
            height = properties.getInt("height", height);
        }

        // Set the window size
        size(width, height);
    }

    public void setup() {

        surface.setResizable(true);

        // "pre" will get invoked before draw - using this to manage window resizing as that's a useful thing to do only at that time
        registerMethod("pre", this);

        background(255);

        running = false;
        drawer = new LifeDrawer(this, 4);
        // create a new LifeUniverse object with the given points
        life = new LifeUniverse();

        hudInfo = new HUDInfo();

        // a bunch of stuff you discovered with chat gpt to get it so it loads and a position you last had it
        loadSavedWindowPositions();
    }


    private void loadSavedWindowPositions() {

        JSONObject properties;
        String propertiesFileName = "window.json";
        File propertiesFile = new File(dataPath(propertiesFileName));

        int x = 100, y = 100, screenIndex = 0;

        if (propertiesFile.exists()) {
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

        Frame frame;

        // chatGPT thinks that this will only work with processing 4 so...
        // probably it would be helpful to have a non-processing mechanism
        // but you're also a little bit reliant on the processing environment so...
        Component comp = (Component) getSurface().getNative();
        while (!(comp instanceof Frame)) {
            comp = comp.getParent();
        }
        frame = (Frame) comp;

        frame.setLocation(x, y);

        // Save window position, size, and screen to JSON file on window close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JSONObject properties = new JSONObject();

                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice[] screens = ge.getScreenDevices();

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

                saveJSONObject(properties, dataPath("window.json"));
            }
        });
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

        // result is null until a value has been passed in from a copy/paste of a RLE (currently)
        if (result != null) {

            if (life.generation == 0) {
                life.saveRewindState();
            }

            life.nextGeneration(true);
            drawer.redraw(life.root);

            Bounds bounds = life.getRootBounds();
            drawer.fit_bounds(bounds);

        }

        drawHud();

    }

    public void keyPressed() {

        // space bar toggles running
        if (key == ' ') {
            running = !running;
        }
        // r resets (pauses) and creates a blank grid
        else if (key == 'r' || key == 'R') {
            running = false;
        }
        // Check if Ctrl is pressed and the key is 'v' or 'V'
        else if ((key == 'v' || key == 'V') && ((keyCode == 86))) {
            pasteBaby();
        }
    }


    /**
     * extracted from draw() for readability
     */
    private void drawHud() {

        textSize(16);
        fill(0);
        Node root = life.root;

        hudInfo.addOrUpdate("fps", parseInt(frameRate));
        hudInfo.addOrUpdate("generation", parseInt(life.generation));
        hudInfo.addOrUpdate("population", root.population);
        hudInfo.addOrUpdate("maxLoad", life.maxLoad);
        hudInfo.addOrUpdate("lastID", life.lastId);

        textAlign(RIGHT, BOTTOM);
        String hud = hudInfo.getFormattedString(frameCount, 12, " | ");
        text(hud, width-50, height-5);

    }


    private void pasteBaby() {

        String potentialLife;

        try {
            // Get the system clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Check if the clipboard contains text data
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                // Get the text data from the clipboard
                potentialLife = (String) clipboard.getData(DataFlavor.stringFlavor);

                Formats parser = new Formats();
                result = parser.parseRLE(potentialLife);

                if (DEBUG_MODE) {
                    println("title: " + result.title);
                    println("author: " + result.author);
                    println("comments: " + result.comments);
                    println("width: " + result.width);
                    println("height: " + result.height);
                    println("rule_s: " + result.rule_s);
                    println("rule_b: " + result.rule_b);
                    println("rule: " + result.rule);
                    println("field_x: " + result.field_x);
                    println("field_y: " + result.field_y);
                    println("instructions: " + result.instructions);
                    println("length of instructions: " + result.instructions.length());
                }

                setupPattern();

                running = false;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        } catch (NotLifeException e) {
            println("get a life - here's what failed:\n\n" + e.getMessage());
        }
    }

}
