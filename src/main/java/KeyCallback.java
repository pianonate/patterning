import processing.event.KeyEvent;

import java.util.stream.Collectors;
import java.util.Set;

public abstract class KeyCallback {
    public final static String MAC = "mac";
    public final static String NON_MAC = "nonmac";
    private final static String currentOS = System.getProperty("os.name").toLowerCase();
    private final Set<Integer> keyCodes;
    private final Integer modifiers;
    private final String validOS;

    public KeyCallback(char key) {
        this(Set.of((int) Character.toUpperCase(key)), null, null);
    }

    public KeyCallback(Set<Character> keys) {
        this(keys.stream().mapToInt(c -> (int) Character.toUpperCase(c)).boxed().collect(Collectors.toSet()), null, null);
    }

    public KeyCallback(char key, Integer modifiers, String validOS) {
        this(Set.of((int) Character.toUpperCase(key)), modifiers, validOS);
    }

    private KeyCallback(Set<Integer> keys, Integer modifiers, String validOS) {
        this.keyCodes = keys;
        this.modifiers = modifiers != null ? modifiers : 0;
        this.validOS = validOS;
    }

    public abstract void onKeyEvent(KeyEvent event);

    public abstract String getUsageText();

    public Set<Integer> getKeyCodes() {
        return keyCodes;
    }

    public Integer getModifiers() {
        return modifiers;
    }

    public String getValidOS() {
        return validOS;
    }

    public boolean matches(KeyEvent event) {
        return getKeyCodes().contains(event.getKeyCode()) && event.getModifiers() == getModifiers() && isValidForCurrentOS();
    }


    public boolean isValidForCurrentOS() {

        if (validOS == null) {
            return true;
        }

        if (validOS.equalsIgnoreCase(KeyCallback.MAC) && currentOS.contains(KeyCallback.MAC)) {
            return true;
        } else {
            return validOS.equalsIgnoreCase(KeyCallback.NON_MAC) && !currentOS.contains(KeyCallback.MAC);
        }
    }
}

