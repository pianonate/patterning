package ux;

import actions.KeyCallback;
import actions.KeyObservable;
import processing.core.PImage;

public class ToggleHighlightOldControl extends OldControl {
    ToggleHighlightOldControl(PImage icon, int size, OldPanelPosition panelPosition, KeyCallback callback) {
        super(icon, size, panelPosition, callback);
    }

    @Override
    void mouseReleased() {
        super.mouseReleased();
        highlight = !highlight;
    }

    // in ToggleHighlightOldControl class
    @Override
    public void notifyKeyPress(KeyObservable o) {
        // Specific behavior for ToggleHighlightOldControl
        highlight = !highlight;
    }
}
