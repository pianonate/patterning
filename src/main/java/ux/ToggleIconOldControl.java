package ux;

import actions.KeyCallback;
import actions.KeyObservable;
import processing.core.PImage;

public class ToggleIconOldControl extends OldControl {

    PImage toggledIcon; // right now only used with play / pause
    boolean iconToggled = false;

    PImage currentIcon;

    ToggleIconOldControl(PImage icon, PImage toggledIcon, int size, OldPanelPosition panelPosition, KeyCallback callback) {
        super(icon, size, panelPosition, callback);
        this.currentIcon = icon;
        this.toggledIcon = toggledIcon;
    }

    @Override
    void mouseReleased() {
        super.mouseReleased();
        toggleIcon();
    }

    @Override
    public void notifyKeyPress(KeyObservable o) {
        toggleIcon();
    }

    @Override
    protected PImage getIcon() {
        return iconToggled ? toggledIcon : icon;
    }


    void toggleIcon() {
        currentIcon = (iconToggled) ? icon : toggledIcon;
        iconToggled = !iconToggled;
    }

}
