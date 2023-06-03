package actions;

import processing.event.KeyEvent;

import java.util.*;
import java.util.stream.Collectors;

public abstract class KeyCallback implements KeyObservable {
    private final LinkedHashSet<KeyCombo> keyCombos;
    private final Set<KeyObserver> keyObservers = new HashSet<>();

    public KeyCallback(char key) {
        this(new KeyCombo(key));
    }

    /**
     * Constructor for actions.KeyCallback class that takes a variable number of actions.KeyCombo objects.
     * The actions.KeyCombo objects are converted into a List, then into a Set to ensure no duplicates,
     * and finally assigned to the keyCombos field.
     *
     * @param keyCombos The variable number of actions.KeyCombo objects
     */
    public KeyCallback(KeyCombo... keyCombos) {
        this.keyCombos = new LinkedHashSet<>(Arrays.asList(keyCombos));
    }

    /**
     * Constructor for actions.KeyCallback class that takes a Set of Characters.
     * Each character is converted to its uppercase equivalent (if not already),
     * cast to an integer ASCII value, and then used to create a new actions.KeyCombo object.
     * The resulting set of actions.KeyCombo objects is assigned to the keyCombos field.
     *
     * @param keys A set of characters representing the keys
     */
    public KeyCallback(Set<Character> keys) {
       /* keyCombos = keys.stream()
                .mapToInt(c -> (int) c)
                .mapToObj(KeyCombo::new)
                .collect(Collectors.toSet());*/

        List<Character> keyList = new ArrayList<>(keys);
        keyCombos = keyList.stream()
                .mapToInt(c -> (int) c)
                .mapToObj(KeyCombo::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void addObserver(KeyObserver o) {
        keyObservers.add(o);
    }


    public void notifyKeyObservers() {
        for (KeyObserver keyObserver : keyObservers) {
            keyObserver.notifyKeyPress(this);
        }
    }

    public boolean invokeModeChange() {return false;}

    public void deleteObserver(KeyObserver o) {
        keyObservers.remove(o);
    }

    public abstract void invokeFeature();

    public void cleanupFeature() {
        // do nothing by default
    }

    public abstract String getUsageText();

    public Set<KeyCombo> getKeyCombos() {
        return keyCombos;
    }

    public Set<KeyCombo> getValidKeyCombosForCurrentOS() {
        return keyCombos.stream()
                .filter(KeyCombo::isValidForCurrentOS)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean matches(KeyEvent event) {
        return keyCombos.stream().anyMatch(kc -> kc.matches(event));
    }

    // The isValidForCurrentOS() method is now a part of actions.KeyCombo, so we don't need it here.
    // However, if you still want to provide a way to check if any actions.KeyCombo is valid for the current OS, you can add a method like this:

    public boolean isValidForCurrentOS() {

        return keyCombos.stream().anyMatch(KeyCombo::isValidForCurrentOS);
    }

    // used to calculate the maximum size to show for usage
 /*   public String getComboTexts() {
        return keyCombos.stream()
                .filter(KeyCombo::isValidForCurrentOS)
                .map(KeyCombo::toString)
                .collect(Collectors.joining(", "));
    }*/

    @Override
    public String toString() {
        return getValidKeyCombosForCurrentOS().stream()
                .map(KeyCombo::toString)
                .collect(Collectors.joining(", "));
    }
}

