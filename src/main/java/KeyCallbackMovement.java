import java.util.Set;

import processing.core.PApplet;


abstract class KeyCallbackMovement extends ProcessingKeyCallback {

    KeyCallbackMovement() {
        super(Set.of((char) PApplet.LEFT, (char) PApplet.RIGHT, (char) PApplet.UP, (char) PApplet.DOWN));
    }
    protected abstract void move(Set<Integer> pressedKeys);
    protected abstract void stopMoving();

}
