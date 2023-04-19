import processing.core.PApplet;
import processing.data.JSONObject;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;

/**
 * todo
 * Add mc parser support
 * do you need to manage the size of the hashmap?
 * create an alternate implementation - extend the hashmap class and override resize method to capture the timing of the resize
 * here's what would be cool - zoom over a section - if your mouse is over a section of what's going on, you can see the details at a much higher zoom level
 */
public class GameOfLife extends PApplet {

    public static void main(String[] args) {

        PApplet.main("GameOfLife");
    }

    private LifeUniverse life;
    private LifeDrawer drawer;
    private Result result;
    private HUDStringBuilder hudInfo;
    private boolean running;
    private boolean fitToWindow;
    // used for resize detection
    private int prevWidth, prevHeight;
    // right now this is the only way that life gets into the game
    private String pastedLife;
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
            this.pastedLife = properties.getString("lifeForm", "");
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
        if (!(pastedLife == null || pastedLife.isEmpty())) {
            // invoke the logic you already have to reify this
            pasteBaby();
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
        properties.setString("lifeForm", pastedLife);

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

        // result is null until a value has been passed in from a copy/paste of RLE (currently)
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

        }

        drawHUD();

    }

    public void keyPressed() {

        // Assuming key is of type char and running is a boolean variable
        switch (key) {
            case '+' -> {
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
            case 'F', 'f' -> {
                fitToWindow = true;
            }
            case ' ' -> {
                running = !running;
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

    /**
     * extracted from draw() for readability
     */
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

            // Check if the clipboard contains text data
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) //noinspection CommentedOutCode
            {
                // Get the text data from the clipboard
                pastedLife = (String) clipboard.getData(DataFlavor.stringFlavor);

                Formats parser = new Formats();
                result = parser.parseRLE(pastedLife);

                /*
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
                */

                setupPattern();

                running = true;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        } catch (NotLifeException e) {
            println("get a life - here's what failed:\n\n" + e.getMessage());
        }
    }

}
