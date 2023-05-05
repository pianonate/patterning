import processing.core.PApplet;
import processing.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public class KeyHandler {

    private final PApplet processing;
    private final  GameOfLife gol;
    private final LifeUniverse life;
    private final LifeDrawer drawer;
    private final Set<Integer> pressedKeys;
    private int lastDirection = 0;
    private long lastIncreaseTime;
    private final float initialMoveAmount = 1;
    private float moveAmount = initialMoveAmount;

    public KeyHandler(PApplet processing,LifeUniverse life, LifeDrawer drawer) {

        this.processing = processing;
        this.gol = (GameOfLife) processing;
        this.drawer = drawer;
        this.life = life;

        this.pressedKeys = new HashSet<>();
        this.lastIncreaseTime = System.currentTimeMillis();

        processing.registerMethod("keyEvent", this);
        processing.registerMethod("draw", this);
    }

    public void keyEvent(KeyEvent e) {

        switch (e.getAction()) {
            case KeyEvent.PRESS -> handleKeyPressed();
            case KeyEvent.RELEASE -> handleKeyReleased();
        }
    }

    public void handleKeyPressed() {
        int keyCode = processing.keyCode;
        pressedKeys.add(keyCode);

        // zoom, fit_bounds, center_view, etc. - they don't actually draw
        // they just put the drawer into a state that on the next redraw
        // the right thing will happen
        // that's why it's cool for them to be invoked on keypress

        switch (processing.key) {
           // case ' ' -> gol.spaceBarHandler();
            default -> {
                // System.out.println("key: " + key + " keycode: " + keyCode);
            }
        }

        handleMovementKeys();
    }



    private void handleKeyReleased() {
        int keyCode = processing.keyCode;
        pressedKeys.remove(keyCode);

        handleMovementKeys();
    }

    private void handleMovementKeys() {

        float moveX = 0;
        float moveY = 0;

        int[][] directions = {{processing.LEFT, 0, -1}, {processing.UP, -1, 0}, {processing.RIGHT, 0, 1}, {processing.DOWN, 1, 0}, {processing.UP + processing.LEFT, -1, -1}, {processing.UP + processing.RIGHT, -1, 1}, {processing.DOWN + processing.LEFT, 1, -1}, {processing.DOWN + processing.RIGHT, 1, 1}};

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

    }
}
