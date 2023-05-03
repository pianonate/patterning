import processing.core.PApplet;
import processing.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class KeyHandler {
    private final PApplet p;
    private final LifeUniverse life;
    private final LifeDrawer drawer;
    private final Set<Integer> pressedKeys;
    private int lastDirection = 0;
    private long lastIncreaseTime;
    private final float initialMoveAmount = 1;
    private float moveAmount = initialMoveAmount;
    private boolean displayBounds = false;

    public KeyHandler(PApplet p, LifeUniverse life, LifeDrawer drawer) {
        this.p = p;
        p.registerMethod("keyEvent", this);
        p.registerMethod("draw", this);
        this.drawer = drawer;
        this.life = life;
        this.pressedKeys = new HashSet<>();
        // todo: if you increase it here then any time you handle movement you will exceed
        //       the last increase time - this seems like a bug to me
        this.lastIncreaseTime = System.currentTimeMillis();

    }

    public void keyEvent(KeyEvent e) {

        switch (e.getAction()) {
            case KeyEvent.PRESS -> handleKeyPressed();
            case KeyEvent.RELEASE -> handleKeyReleased();
        }
    }

    public void handleKeyPressed() {
        int keyCode = p.keyCode;
        pressedKeys.add(keyCode);

        // zoom, fit_bounds, center_view, etc. - they don't actually draw
        // they just put the drawer into a state that on the next redraw
        // the right thing will happen
        // that's why it's cool for them to be invoked on keypress

        switch (p.key) {
            case '+', '=' -> zoom(false);
            case '-' -> zoom(true);
            case ']' -> handleStep(true);
            case '[' -> handleStep(false);
            case 'B', 'b' -> displayBounds = !displayBounds;
            case 'C', 'c' -> drawer.center_view(life.getRootBounds());
            case 'F', 'f' -> {
                System.out.println("KeyHandler Bounds: " + life.getRootBounds().toString());
                drawer.fit_bounds(life.getRootBounds());
            }
            case 'R', 'r' -> rewind();
            case 'V', 'v' -> handlePaste();
            case ' ' -> ((GameOfLife) p).spaceBarHandler();
            default -> {
                // System.out.println("key: " + key + " keycode: " + keyCode);
            }
        }

        handleMovementKeys();
    }

    private void rewind() {
        life.restoreRewindState();
        life.setStep(0);
        ((GameOfLife)p).stop();
        drawer.fit_bounds(life.getRootBounds());
    }

    // todo - i think you can't let step be less than 1
    private void handleStep(Boolean faster) {

        int increment = (faster) ? 1 : -1;

        System.out.println((faster) ? "faster requested" : "slower requested");
        ((GameOfLife) p).incrementTarget(increment);
    }

    private void handlePaste() {
        if (p.keyCode == 86) {
            ((GameOfLife) p).pasteHandler();
        }
    }

    // call the zoom implementation that zooms in or out based on
    // the current position of the mouse (but you could go in or out based on any position
    private void zoom(boolean out) {
        drawer.zoom_at(out, p.mouseX, p.mouseY);
    }

    private void handleKeyReleased() {
        int keyCode = p.keyCode;
        pressedKeys.remove(keyCode);

        handleMovementKeys();
    }

    private void handleMovementKeys() {

        float moveX = 0;
        float moveY = 0;

        int[][] directions = {{p.LEFT, 0, -1}, {p.UP, -1, 0}, {p.RIGHT, 0, 1}, {p.DOWN, 1, 0}, {p.UP + p.LEFT, -1, -1}, {p.UP + p.RIGHT, -1, 1}, {p.DOWN + p.LEFT, 1, -1}, {p.DOWN + p.RIGHT, 1, 1}};

        for (int[] direction : directions) {
            boolean isMoving = pressedKeys.contains(direction[0]);
            if (isMoving) {
                moveX += direction[2] * moveAmount;
                moveY += direction[1] * moveAmount;
            }
        }

        // Check if the direction has changed
        int currentDirection = 0;
        for (int[] direction : directions) {
            boolean isMoving = pressedKeys.contains(direction[0]);
            if (isMoving) {
                currentDirection += direction[0];
            }
        }

        if (currentDirection != lastDirection) {
            // Reset moveAmount if direction has changed
            moveAmount = initialMoveAmount;
            lastIncreaseTime = System.currentTimeMillis();
        } else {
            // Increase moveAmount if enough time has passed since last increase
            long currentTime = System.currentTimeMillis();
            long increaseInterval = 500;
            if (currentTime - lastIncreaseTime >= increaseInterval) {
                moveAmount += 0.5f;
                lastIncreaseTime = currentTime;
            }
        }

        drawer.move(moveX, moveY);
        lastDirection = currentDirection;
    }

    // draw is a registeredMethod guaranteed to be invoked
    // (I believe) during the draw event - i think after
    // it handles draw functions that must be invoked each draw cycle
    // related to the keyboard driven interface of this thing
    // such as moving it around or displaying the boundary
    public void draw() {

        for (int i = 0; i < 2; i++) {
            handleMovementKeys();
        }

        if (displayBounds) {

            drawer.draw_bounds(life.getRootBounds());
        }

    }
}
