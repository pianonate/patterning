package ux;

public class UXTheme {
    private static UXTheme instance = null;

    // sizes
    private int controlSize;
    private int controlHighlightCornerRadius;
    private int hoverTextSize;
    private int hoverTextMaxWidth;
    private int hoverTextBuffer;

    // colors
    private int backGroundColor;
    private int cellColor;
    private int controlColor;
    private int controlHighlightColor;

    private int controlMousePressedColor;

    private int defaultPanelColor;

    private int textColor;
    private int textColorStart; // for lerping purposes

    // durations
    private int controlHighlightDuration;
    private long longTransitionDuration;
    private long shortTransitionDuration;

    private UXTheme() {
        setTheme(UXThemeType.DARK);  // Default theme
    }

    public static UXTheme getInstance() {
        if (instance == null) {
            instance = new UXTheme();
        }
        return instance;
    }

    public void setTheme(UXThemeType newTheme) {
        UXConstants themeConstants = newTheme.getThemeConstants();

        // sizes and radii
        controlSize = themeConstants.getControlSize();
        controlHighlightCornerRadius = themeConstants.getControlHighlightCornerRadius();
        hoverTextSize = themeConstants.getHoverTextSize();
        hoverTextMaxWidth = themeConstants.getHoverTextMaxWidth();
        hoverTextBuffer = themeConstants.getHoverTextBuffer();


        // colors
        backGroundColor = themeConstants.getBackgroundColor();
        cellColor = themeConstants.getCellColor();
        controlColor = themeConstants.getControlColor();
        controlHighlightColor = themeConstants.getControlHighlightColor();
        controlMousePressedColor = themeConstants.getControlMousePressedColor();
        defaultPanelColor = themeConstants.getDefaultPanelColor();
        textColor = themeConstants.getTextColor();
        textColorStart = themeConstants.getTextColorStart();

        //durations
        controlHighlightDuration = themeConstants.getControlHighlightDuration();
        longTransitionDuration = themeConstants.getLongTransitionDuration();
        shortTransitionDuration = themeConstants.getShortTransitionDuration();

    }

    // sizes
    public int getControlSize() {
        return controlSize;
    }
    public int getControlHighlightCornerRadius() {
        return controlHighlightCornerRadius;
    }
    public int getHoverTextBuffer() {return hoverTextBuffer;}
    public int getHoverTextMaxWidth() {return hoverTextMaxWidth;}
    public int getHoverTextSize() {return hoverTextSize;}

    // colors
    public int getBackGroundColor() {
        return backGroundColor;
    }

    public int getCellColor() {
        return cellColor;
    }

    public int getControlColor() {
        return controlColor;
    }

    public int getControlHighlightColor() {
        return controlHighlightColor;
    }
    public int getControlMousePressedColor() { return controlMousePressedColor;}

    public int getDefaultPanelColor() {
        return defaultPanelColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getTextColorStart() {return textColorStart;}

    // durations

    public int getControlHighlightDuration() {
        return controlHighlightDuration;
    }
    public long getLongTransitionDuration() {
        return longTransitionDuration;
    }

    public long getShortTransitionDuration() {
        return shortTransitionDuration;
    }
}

