import processing.event.KeyEvent;
import processing.core.PApplet;

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessingKeyHandler {
    private final Map<Set<Integer>, ProcessingKeyCallback> keyCallbacks;

    private final String currentOS;

    public ProcessingKeyHandler(PApplet processing) {
        this.keyCallbacks = new LinkedHashMap<>();
        processing.registerMethod("keyEvent", this);
        this.currentOS = System.getProperty("os.name").toLowerCase();
    }

    public void addKeyCallback(ProcessingKeyCallback callback) {
        Set<Integer> keyCodes = callback.getKeyCodes();
        Set<Integer> duplicateKeyCodes = findDuplicateKeyCodes(keyCodes);

        if (!duplicateKeyCodes.isEmpty()) {
            String duplicateKeyCodesString = duplicateKeyCodes.stream()
                    .map(k -> keyCodeToText(k, 0))
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("The following key codes are already associated with another callback: " + duplicateKeyCodesString);
        }

        keyCallbacks.put(keyCodes, callback);
    }



    private Set<Integer> findDuplicateKeyCodes(Set<Integer> keyCodes) {
        Set<Integer> duplicateKeyCodes = new HashSet<>();

        for (Map.Entry<Set<Integer>, ProcessingKeyCallback> entry : keyCallbacks.entrySet()) {
            Set<Integer> existingKeyCodes = entry.getKey();
            for (Integer keyCode : keyCodes) {
                if (existingKeyCodes.contains(keyCode)) {
                    duplicateKeyCodes.add(keyCode);
                }
            }
        }

        return duplicateKeyCodes;
    }


    public void keyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.PRESS) {
            for (ProcessingKeyCallback callback : keyCallbacks.values()) {
                if (callback.matches(event)) {
                    callback.onKeyEvent();
                    break;
                }
            }
        }
    }

    public String generateUsageText() {
        StringBuilder usageText = new StringBuilder("Key Usage:\n\n");

        Set<ProcessingKeyCallback> processedCallbacks = new HashSet<>();

        int maxKeysWidth = keyCallbacks.entrySet().stream()
                .filter(e -> e.getValue().isValidForCurrentOS())
                .mapToInt(entry -> {
                    StringBuilder keysStringBuilder = new StringBuilder();
                    for (Integer k : entry.getKey()) {
                        if (entry.getValue().isValidForCurrentOS()) {
                            String keyStr = keyCodeToText(k, entry.getValue().getModifiers());
                            keysStringBuilder.append(keyStr).append(", ");
                        }
                    }
                    return keysStringBuilder.length()-2;
                })
                .max().orElse(0);

        for (Map.Entry<Set<Integer>, ProcessingKeyCallback> entry : keyCallbacks.entrySet()) {
            Set<Integer> keyCodes = entry.getKey();
            ProcessingKeyCallback callback = entry.getValue();

            if (!callback.isValidForCurrentOS() || processedCallbacks.contains(callback)) {
                continue;
            }

            StringBuilder keysStringBuilder = new StringBuilder();
            for (Integer keyCode : keyCodes) {
                if (callback.isValidForCurrentOS()) {
                    String keyStr = keyCodeToText(keyCode, callback.getModifiers());
                    keysStringBuilder.append(keyStr).append(", ");
                }
            }

            String keysString = keysStringBuilder.toString().replaceAll(", $", "");

            String usageDescription = callback.getUsageText();
            usageText.append(String.format("%-" + (maxKeysWidth+1) + "s: %s\n", keysString, usageDescription));

            processedCallbacks.add(callback);
        }

        return usageText.toString();
    }


    private String keyCodeToText(int keyCode, int modifiers) {
        StringBuilder keyTextBuilder = new StringBuilder();

        if ((modifiers & KeyEvent.META) != 0 && currentOS.contains("mac")) {
            keyTextBuilder.append("Cmd+");
        }
        if ((modifiers & KeyEvent.CTRL) != 0) {
            keyTextBuilder.append("Ctrl+");
        }

        if (keyCode == 32) {
            keyTextBuilder.append("Space");
        } else {
            keyTextBuilder.append((char) keyCode);
        }

        return keyTextBuilder.toString();
    }


}
