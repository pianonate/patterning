import processing.core.PApplet;
import processing.event.KeyEvent;

import java.util.*;
import java.util.stream.Collectors;

public class KeyHandler {
    private final Map<Set<KeyCombo>, KeyCallback> keyCallbacks;
    //private final Set<Integer> pressedKeys;

    private final static Set<Integer> pressedKeys = new HashSet<>();

    public static Set<Integer> getPressedKeys() {
        return Collections.unmodifiableSet(pressedKeys);
    }

    public KeyHandler(PApplet processing) {
        this.keyCallbacks = new LinkedHashMap<>();
       // this.pressedKeys = new HashSet<>();
        processing.registerMethod("keyEvent", this);
    }

    public void addKeyCallback(KeyCallback callback) {
        Set<KeyCombo> keyCombos = callback.getKeyCombos();
        Set<KeyCombo> duplicateKeyCombos = findDuplicateKeyCombos(keyCombos);

        if (!duplicateKeyCombos.isEmpty()) {
            String duplicateKeyCombosString = duplicateKeyCombos.stream()
                    .map(KeyCombo::toString)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("The following key combos are already associated with another callback: " + duplicateKeyCombosString);
        }

        keyCallbacks.put(keyCombos, callback);
    }

    // make sure that no KeyCombos have already been set up that match the one added
    private Set<KeyCombo> findDuplicateKeyCombos(Set<KeyCombo> keyCombos) {
        Set<KeyCombo> duplicateKeyCombos = new HashSet<>();

        for (Map.Entry<Set<KeyCombo>, KeyCallback> entry : keyCallbacks.entrySet()) {
            Set<KeyCombo> existingKeyCombos = entry.getKey();
            for (KeyCombo keyCombo : keyCombos) {
                if (existingKeyCombos.contains(keyCombo)) {
                    duplicateKeyCombos.add(keyCombo);
                }
            }
        }

        return duplicateKeyCombos;
    }


    public void keyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        for (KeyCallback callback : keyCallbacks.values()) {
            if (callback.matches(event)) {

                if (event.getAction() == KeyEvent.PRESS) {
                    pressedKeys.add(keyCode);
                    callback.onKeyPress(event);

                }

                if (event.getAction() == KeyEvent.RELEASE) {
                    pressedKeys.remove(keyCode);
                    callback.onKeyRelease(event);
                }

                break;
            }
        }
    }

    public String getUsageText() {
        StringBuilder usageText = new StringBuilder("Key Usage:\n\n");

        Set<KeyCallback> processedCallbacks = new HashSet<>();

        int maxKeysWidth = keyCallbacks.values().stream()
                .filter(KeyCallback::isValidForCurrentOS)
                .mapToInt(callback -> callback.getComboTexts().length())
                .max().orElse(0);

        for (KeyCallback callback : keyCallbacks.values()) {
            // valid for OS and not already processed
            if (!callback.isValidForCurrentOS() || processedCallbacks.contains(callback)) {
                continue;
            }

            // Only include key combos that are valid for the current OS
            String keysString = callback.getKeyCombos().stream()
                    .filter(KeyCombo::isValidForCurrentOS)
                    .map(KeyCombo::toString)
                    .collect(Collectors.joining(", "));

            String usageDescription = callback.getUsageText();
            usageText.append(String.format("%-" + (maxKeysWidth + 1) + "s: %s\n", keysString, usageDescription));

            processedCallbacks.add(callback);
        }

        return usageText.toString();
    }
}