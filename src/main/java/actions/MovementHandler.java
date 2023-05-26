package actions;

import processing.core.PApplet;
import ux.PatternDrawer;

import java.util.Set;

// this class exists merely to encapsulate movement handling as it is a bit complex
// and i don't want to clutter the main class anymore than it already
public class MovementHandler {

    private final static Set<Integer> pressedKeys = KeyHandler.getPressedKeys();

    private int lastDirection = 0;
    private long lastIncreaseTime;
    private final float initialMoveAmount = 1;
    private float moveAmount = initialMoveAmount;

    private final PatternDrawer drawer;

    public MovementHandler(PatternDrawer drawer) {
        this.drawer = drawer;
    }


    public void handleRequestedMovement() {
        for (Integer key : pressedKeys) {
            switch (key) {
                case PApplet.LEFT, PApplet.RIGHT, PApplet.UP, PApplet.DOWN -> handleMovementKeys();
                default ->
                    // Ignore other keys and set lastDirection to 0
                        lastDirection = 0;
            }
        }
    }


    private void handleMovementKeys() {

        float moveX = 0;
        float moveY = 0;

        int[][] directions = {
                {PApplet.LEFT, 0, -1},
                {PApplet.UP, -1, 0},
                {PApplet.RIGHT, 0, 1},
                {PApplet.DOWN, 1, 0},
                {PApplet.UP + PApplet.LEFT, -1, -1},
                {PApplet.UP + PApplet.RIGHT, -1, 1},
                {PApplet.DOWN + PApplet.LEFT, 1, -1},
                {PApplet.DOWN + PApplet.RIGHT, 1, 1}
        };

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
            long increaseInterval = 100;
            if (currentTime - lastIncreaseTime >= increaseInterval) {
                moveAmount += 0.6f;
                lastIncreaseTime = currentTime;
            }
        }

        lastDirection = currentDirection;

        drawer.move(moveX, moveY);

    }

}