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
    private int iconMargin;
    private int startupTextSize;

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
    private int controlPanelTransitionDuration;
    private int longTransitionDuration;
    private int shortTransitionDuration;
    private int startupTextDisplayDuration;
    private int startupTextFadeInDuration;
    private int startupTextFadeOutDuration;

    // strings
    private String fontName;
    private String iconPath;
    private String shortcutParenStart;
    private String shortcutParenEnd;
    private String startupText;

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
        iconMargin = themeConstants.getIconMargin();
        startupTextSize = themeConstants.getStartupTextSize();

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
        controlPanelTransitionDuration = themeConstants.getControlPanelTransitionDuration();
        longTransitionDuration = themeConstants.getLongTransitionDuration();
        shortTransitionDuration = themeConstants.getShortTransitionDuration();
        startupTextDisplayDuration = themeConstants.getStartupTextDisplayDuration();
        startupTextFadeInDuration = themeConstants.getStartupTextFadeInDuration();
        startupTextFadeOutDuration = themeConstants.getStartupTextFadeOutDuration();

        // strings
        fontName = themeConstants.getFontName();
        iconPath = themeConstants.getIconPath();
        shortcutParenStart = themeConstants.getShortcutParenStart();
        shortcutParenEnd = themeConstants.getShortcutParenEnd();
        startupText = themeConstants.getStartupText();

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

    public int getIconMargin() {
        return iconMargin;
    }

    public int getStartupTextSize() {
        return startupTextSize;
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
    public int getControlPanelTransitionDuration() { return controlPanelTransitionDuration;}

    public int getLongTransitionDuration() {
        return longTransitionDuration;
    }

    public int getShortTransitionDuration() {
        return shortTransitionDuration;
    }

    public int getStartupTextDisplayDuration() {
        return startupTextDisplayDuration;
    }

    ;

    public int getStartupTextFadeInDuration() {
        return startupTextFadeInDuration;
    }

    public int getStartupTextFadeOutDuration() {
        return startupTextFadeOutDuration;
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

    public String getStartupText() {
        return startupText;
    }
}

