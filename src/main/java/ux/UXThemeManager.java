package ux;

public class UXThemeManager {
    private static UXThemeManager instance = null;

    // sizes
    private int controlSize;
    private int controlHighlightCornerRadius;
    private int defaultTextMargin;

    private float defaultTextSize;

    private int hoverTextSize;
    private int hoverTextWidth;
    private int hoverTextMargin;

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

    // strings
    private String fontName;
    private String iconPath;

    private String shortcutParenStart;
    private String shortcutParenEnd;

    private UXThemeManager() {
        setTheme(UXThemeType.DARK);  // Default theme
    }

    public void setTheme(UXThemeType newTheme) {
        UXConstants themeConstants = newTheme.getThemeConstants();

        // sizes and radii
        controlSize = themeConstants.getControlSize();
        controlHighlightCornerRadius = themeConstants.getControlHighlightCornerRadius();
        defaultTextMargin = themeConstants.getDefaultTextMargin();
        defaultTextSize = themeConstants.getDefaultTextSize();
        hoverTextSize = themeConstants.getHoverTextSize();
        hoverTextWidth = themeConstants.getHoverTextMaxWidth();
        hoverTextMargin = themeConstants.getHoverTextMargin();

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

        // strings
        fontName = themeConstants.getFontName();
        iconPath = themeConstants.getIconPath();
        shortcutParenStart = themeConstants.getShortcutParenStart();
        shortcutParenEnd = themeConstants.getShortcutParenEnd();

    }

    public static UXThemeManager getInstance() {
        if (instance == null) {
            instance = new UXThemeManager();
        }
        return instance;
    }

    // sizes

    public int getControlSize() {
        return controlSize;
    }

    public int getControlHighlightCornerRadius() {
        return controlHighlightCornerRadius;
    }

    public int getDefaultTextMargin() {
        return defaultTextMargin;
    }
    public float getDefaultTextSize() {
        return defaultTextSize;
    }


    public int getHoverTextMargin() {
        return hoverTextMargin;
    }

    public int getHoverTextWidth() {
        return hoverTextWidth;
    }

    public int getHoverTextSize() {
        return hoverTextSize;
    }

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

    public int getControlMousePressedColor() {
        return controlMousePressedColor;
    }

    public int getDefaultPanelColor() {
        return defaultPanelColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getTextColorStart() {
        return textColorStart;
    }

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

    // names
    public String getFontName() {
        return fontName;
    }
    public String getIconPath() {
        return iconPath;
    }
    public String getShortcutParenStart() {
        return shortcutParenStart;
    }
    public String getShortcutParenEnd() {
        return shortcutParenEnd;
    }
}

