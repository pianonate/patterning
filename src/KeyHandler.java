import java.util.HashSet;

class KeyHandler {
    HashSet<Integer> pressedKeys;


    KeyHandler() {
        pressedKeys = new HashSet<>();
    }

    public void keyPressed(char key, int keyCode) {
        // Handle key press events
        pressedKeys.add(keyCode);
    }

    public void keyReleased(char key, int keyCode) {
        // Handle key release events
        pressedKeys.remove(keyCode);
    }

    public boolean isKeyHeld(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    private int keyToKeyCode(char key) {
        return java.awt.event.KeyEvent.getExtendedKeyCodeForChar(key);
    }

    public boolean isKeyHeld(char key) {
        return isKeyHeld(keyToKeyCode(key));
    }


}