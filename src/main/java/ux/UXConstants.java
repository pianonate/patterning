package ux;

public class UXConstants {

    UXConstants() {
    }

    // sizes
    int getControlSize() {
        return 35;
    }

    int getControlHighlightCornerRadius() {
        return 10;
    }

    int getDefaultTextMargin() {
        return 5;
    }

    float getDefaultTextSize() {
        return 30F;
    }

    int getHoverTextMargin() {
        return 5;
    }

    int getHoverTextMaxWidth() {
        return 225;
    }

    int getHoverTextSize() {
        return 14;
    }

    int getIconMargin() {
        return 5;
    }

    int getStartupTextSize() {
        return 50;
    }

    // colors

    int getBackgroundColor() {
        return 255;
    }

    int getCellColor() {
        return 0;
    }

    int getControlColor() {
        return 0xC8505050; // partially transparent 40
    }

    int getControlHighlightColor() {
        return 0xC8909090; // partially transparent 60
    }

    int getControlMousePressedColor() {
        return 0xC8E1E1E1; // partially transparent 225
    }

    int getDefaultPanelColor() {
        return 0x00FFFFFF;
    }

    int getTextColorStart() {
        return 0xFFFFFFFF;
    }

    int getTextColor() {
        return 0xFF000000;
    }

    // durations
    int getControlHighlightDuration() {
        return 1000;
    }
    int getControlPanelTransitionDuration() { return 1500;}

    int getLongTransitionDuration() {
        return 3000;
    }

    int getShortTransitionDuration() {
        return 300;
    }

    int getStartupTextDisplayDuration() {
        return 5000;
    }

    int getStartupTextFadeInDuration() {
        return 2000;
    }

    int getStartupTextFadeOutDuration() {
        return 2000;
    }

    // names
    String getFontName() {
        return "Verdana";
    }

    String getIconPath() {
        return "icon/";
    }

    String getShortcutParenStart() {
        return " (";
    }

    String getShortcutParenEnd() {
        return ")";
    }

    String getStartupText() {
        return "patterning";
    }

}
