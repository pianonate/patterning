import actions.KeyCallback;
import actions.KeyCombo;

import java.util.Set;

class MockKeyCallback extends KeyCallback {

    private boolean called = false;

    MockKeyCallback(char key) {
        super(key);
    }

    MockKeyCallback(Set<Character> keys) {
        super(keys);
    }

    MockKeyCallback(KeyCombo... keyCombos) {
        super(keyCombos);
    }

    @Override
    public void invokeFeature() {
        // no-op for this mock
        called = true;

    }

    @Override
    public String getUsageText() {
        return "Mock usage";
    }

    public boolean wasCalled() {
        return called;
    }
}