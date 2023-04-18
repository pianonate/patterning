import processing.core.PApplet;
import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.io.IOException;

/*
  todo
 initial visualization
 Add mc parser support
*/
public class GameOfLife extends PApplet {

    public static void main(String[] args) {
        PApplet.main("GameOfLife");
    }

    LifeUniverse life;
    LifeDrawer drawer;
    Result result;

    boolean running;
    final boolean DEBUG_MODE = false;

    void setupPattern() {
        life.clearPattern();
        Bounds bounds = life.getBounds(result.field_x, result.field_y);
        life.makeCenter(result.field_x, result.field_y, bounds);
        life.setupField(result.field_x, result.field_y, bounds);

        bounds = life.getRootBounds();
        drawer.fit_bounds(bounds);
    }

    public void settings() {
        size(800, 800);
    }
    public void setup() {


        background(255);
        running = false;

        drawer = new LifeDrawer(this, 4);


        // create a new LifeUniverse object with the given points
        life = new LifeUniverse();
    }

    public void draw() {

        if (result!=null) {
            if (life.generation==0) {
                life.saveRewindState();
            }

            life.nextGeneration(true);
            drawer.redraw(life.root);



            Bounds bounds = life.getRootBounds();
            drawer.fit_bounds(bounds);

            drawHud(bounds);

        }
    }

    public void keyPressed() {

        // debugOut(str(key) + " keycode:" + keyCode);

        // spacebar toggles running
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


    /*
        extracted from draw() for readability
    */
    private void drawHud(Bounds bounds) {

        textAlign(CENTER, CENTER);
        textSize(14);
        Node root = life.root;

        String msg = "generation " + nf(life.generation,1,0)
                + " | population " + root.population
                + " | maxLoad " + life.maxLoad
                + " | lastID " + life.lastId;

        text(msg, width/2, height-20);

        text(bounds.left, 50, height / 2);
        text(bounds.top, width / 2, 50);
        text(bounds.right, width - 50, height / 2);
        text(bounds.bottom, width / 2, height - 50);
    }


    private void pasteBaby() {

        String potentialLife = "";

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

                running=false;
            }
        }
        catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
        catch (NotLifeException e) {
            println("get a life\n\n" + e.getMessage());
        }
    }

}
