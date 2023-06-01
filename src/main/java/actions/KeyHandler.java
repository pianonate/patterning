package actions;

import processing.core.PApplet;
import processing.event.KeyEvent;
import ux.DrawRateManager;

import java.util.*;
import java.util.stream.Collectors;

public class KeyHandler {
    private final Map<Set<KeyCombo>, KeyCallback> keyCallbacks;
    private final static Set<Integer> pressedKeys = new HashSet<>();

    public static Set<Integer> getPressedKeys() {
        return Collections.unmodifiableSet(pressedKeys);
    }

    private KeyHandler(Builder builder) {
        this.keyCallbacks = builder.keyCallbacks;
        builder.processing.registerMethod("keyEvent", this);
    }

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
                    callback.invokeFeature();
                    callback.notifyKeyObservers();
                    ;
                    DrawRateManager.getInstance().drawImmediately();
                }

                if (event.getAction() == KeyEvent.RELEASE) {
                    pressedKeys.remove(keyCode);
                    callback.cleanupFeature();
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
                .mapToInt(callback -> callback.getValidKeyCombosForCurrentOS().stream()
                        .map(KeyCombo::toString)
                        .collect(Collectors.joining(", ")).length())
                .max().orElse(0);


        for (KeyCallback callback : keyCallbacks.values()) {
            // valid for OS and not already processed
            if (!callback.isValidForCurrentOS() || processedCallbacks.contains(callback)) {
                continue;
            }

            // Only include key combos that are valid for the current OS
            String keysString = callback.getValidKeyCombosForCurrentOS().stream()
                    .map(KeyCombo::toString)
                    .collect(Collectors.joining(", "));

            String usageDescription = callback.getUsageText();
            usageText.append(String.format("%-" + (maxKeysWidth + 1) + "s: %s\n", keysString, usageDescription));

            processedCallbacks.add(callback);
        }

        return usageText.toString();
    }

    // Here's our Builder class
    public static class Builder {
        private final Map<Set<KeyCombo>, KeyCallback> keyCallbacks = new LinkedHashMap<>();
        private final PApplet processing;

        public Builder(PApplet processing) {
            this.processing = processing;
        }

        public Builder addKeyCallback(KeyCallback callback) {
            Set<KeyCombo> keyCombos = callback.getKeyCombos();
            Set<KeyCombo> duplicateKeyCombos = findDuplicateKeyCombos(keyCombos);

            if (!duplicateKeyCombos.isEmpty()) {
                String duplicateKeyCombosString = duplicateKeyCombos.stream()
                        .map(KeyCombo::toString)
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("The following key combos are already associated with another callback: " + duplicateKeyCombosString);
            }

            keyCallbacks.put(keyCombos, callback);
            return this; // this allows us to chain method calls
        }

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

        public KeyHandler build() {
            return new KeyHandler(this);
        }
    }
}
