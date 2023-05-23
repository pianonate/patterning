import processing.core.PApplet;
import processing.event.KeyEvent;

import java.util.Objects;


enum ValidOS {
    ANY,
    MAC,
    NON_MAC
}

public class KeyCombo {
    static ValidOS currentOS = determineCurrentOS();
    private final int keyCode;
    private final int modifiers;
    private final ValidOS validOS;
    public KeyCombo(int keyCode) {
        this(keyCode, 0, ValidOS.ANY);
    }
    public KeyCombo(int keyCode, int modifiers, ValidOS validOS) {
        this.keyCode = Character.isLowerCase(keyCode) ? Character.toUpperCase(keyCode) : keyCode;
        this.modifiers = modifiers;
        this.validOS = validOS;
    }

    public KeyCombo(int keyCode, int modifiers) {
        this(keyCode, modifiers, ValidOS.ANY);
    }

    public KeyCombo(char keyCode, int modifiers) {
        this(keyCode, modifiers, ValidOS.ANY);
    }

    private static ValidOS determineCurrentOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            return ValidOS.MAC;
        } else {
            return ValidOS.NON_MAC;
        }
    }

    // use this for testing purposes only
    static void setCurrentOS(ValidOS os) {
        currentOS = os;
    }

    public ValidOS getValidOS() {
        return validOS;
    }

    // used at runtime to make sure that the incoming keyEvent matches a particular KeyCombo
    public boolean matches(KeyEvent event) {
        return this.getKeyCode() == event.getKeyCode() &&
                this.getModifiers() == event.getModifiers() &&
                isValidForCurrentOS();
    }

    public int getKeyCode() {
        return keyCode;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isValidForCurrentOS() {

        return switch (validOS) {
            case ANY -> true;
            case MAC -> currentOS == ValidOS.MAC;
            case NON_MAC -> currentOS == ValidOS.NON_MAC;
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCode, modifiers, validOS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyCombo keyCombo = (KeyCombo) o;
        return keyCode == keyCombo.keyCode &&
                modifiers == keyCombo.modifiers &&
                validOS == keyCombo.validOS;
    }

    @Override
    public String toString() {
        // Special case for Shift+=, display as +
        if ((modifiers & KeyEvent.SHIFT) != 0 && keyCode == '=') {
            return "+";
        }

        StringBuilder keyTextBuilder = new StringBuilder();

        if ((modifiers & KeyEvent.META) != 0) {
            keyTextBuilder.append("⌘");
        }
        if ((modifiers & KeyEvent.CTRL) != 0) {
            keyTextBuilder.append("^");
        }
        if ((modifiers & KeyEvent.SHIFT) != 0) {
            keyTextBuilder.append("↑");
        }
        if ((modifiers & KeyEvent.ALT) != 0) {
            keyTextBuilder.append("⌥");
        }

        switch (keyCode) {
            case PApplet.UP -> keyTextBuilder.append("↑");
            case PApplet.DOWN -> keyTextBuilder.append("↓");
            case PApplet.LEFT -> keyTextBuilder.append("←");
            case PApplet.RIGHT -> keyTextBuilder.append("→");
            case 32 -> keyTextBuilder.append("Space");
            default -> keyTextBuilder.append((char) keyCode);
        }

        return keyTextBuilder.toString();
    }
}
