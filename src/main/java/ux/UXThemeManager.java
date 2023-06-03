package ux;

import processing.core.PApplet;

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
    private final ColorConstant backGroundColor;
    private final ColorConstant cellColor;
    private final ColorConstant controlColor;
    private final ColorConstant controlHighlightColor;
    private final ColorConstant controlMousePressedColor;
    private final ColorConstant defaultPanelColor;
    private final ColorConstant textColor;
    private final ColorConstant textColorStart; // for lerping purposes

    // durations
    private int controlHighlightDuration;
    private int controlPanelTransitionDuration;
    private int shortTransitionDuration;
    private int singleModeToggleDuration;
    private int startupTextDisplayDuration;
    private int startupTextFadeInDuration;
    private int startupTextFadeOutDuration;
    private int themeTransitionDuration;

    // strings
    private String fontName;
    private String iconPath;
    private String shortcutParenStart;
    private String shortcutParenEnd;
    private String startupText;

    private UXThemeManager() {
        backGroundColor = new ColorConstant();
        cellColor = new ColorConstant();
        controlColor = new ColorConstant();
        controlHighlightColor = new ColorConstant();
        controlMousePressedColor =new ColorConstant();
        defaultPanelColor = new ColorConstant();
        textColor = new ColorConstant();
        textColorStart = new ColorConstant();
        setTheme(UXThemeType.DARK, null, true);  // Default theme
    }

    private void setTheme(UXThemeType newTheme, PApplet processing, boolean underConstruction) {
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
        backGroundColor.setColor(themeConstants.getBackgroundColor(), processing);
        cellColor.setColor(themeConstants.getCellColor(), processing);
        controlColor.setColor(themeConstants.getControlColor(), processing);
        controlHighlightColor.setColor(themeConstants.getControlHighlightColor(), processing);
        controlMousePressedColor.setColor(themeConstants.getControlMousePressedColor(), processing);
        defaultPanelColor.setColor(themeConstants.getDefaultPanelColor(), processing);
        textColor.setColor(themeConstants.getTextColor(), processing);
        textColorStart.setColor(themeConstants.getTextColorStart(), processing);

        //durations
        controlHighlightDuration = themeConstants.getControlHighlightDuration();
        controlPanelTransitionDuration = themeConstants.getControlPanelTransitionDuration();
        shortTransitionDuration = themeConstants.getShortTransitionDuration();
        singleModeToggleDuration = themeConstants.getSingleModeToggleDuration();
        startupTextDisplayDuration = themeConstants.getStartupTextDisplayDuration();
        startupTextFadeInDuration = themeConstants.getStartupTextFadeInDuration();
        startupTextFadeOutDuration = themeConstants.getStartupTextFadeOutDuration();
        themeTransitionDuration = themeConstants.getThemeTransitionDuration();

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

    public void setTheme(UXThemeType newTheme, PApplet processing) {
        setTheme(newTheme, processing, false);
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
        return backGroundColor.getColor();
    }

    public int getCellColor() {

        return cellColor.getColor();
    }

    public int getControlColor() {
        return controlColor.getColor();
    }

    public int getControlHighlightColor() {
        return controlHighlightColor.getColor();
    }

    public int getControlMousePressedColor() {
        return controlMousePressedColor.getColor();
    }

    public int getDefaultPanelColor() {
        return defaultPanelColor.getColor();
    }

    public int getTextColor() {
        return textColor.getColor();
    }

    public int getTextColorStart() {
        return textColorStart.getColor();
    }

    // durations

    public int getControlHighlightDuration() {
        return controlHighlightDuration;
    }

    public int getControlPanelTransitionDuration() {
        return controlPanelTransitionDuration;
    }

    public int getShortTransitionDuration() {
        return shortTransitionDuration;
    }

    public int getSingleModeToggleDuration() {
        return singleModeToggleDuration;
    }

    public int getStartupTextDisplayDuration() {
        return startupTextDisplayDuration;
    }

    public int getStartupTextFadeInDuration() {
        return startupTextFadeInDuration;
    }

    public int getStartupTextFadeOutDuration() {
        return startupTextFadeOutDuration;
    }

    public int getThemeTransitionDuration() {
        return themeTransitionDuration;
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

