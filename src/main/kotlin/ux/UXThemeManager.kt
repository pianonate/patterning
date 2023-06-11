package ux

import processing.core.PApplet

class UXThemeManager private constructor() {
    // sizes
    // sizes
    var controlSize = 0
        private set
    var controlHighlightCornerRadius = 0
        private set
    var defaultTextMargin = 0
        private set
    var defaultTextSize = 0f
        private set
    var hoverTextSize = 0
        private set
    var hoverTextWidth = 0
        private set
    var hoverTextMargin = 0
        private set
    var iconMargin = 0
        private set
    var startupTextSize = 0
        private set

    // colors
    private val backGroundColor: ColorConstant = ColorConstant()
    private val cellColor: ColorConstant = ColorConstant()
    private val controlColor: ColorConstant = ColorConstant()
    private val controlHighlightColor: ColorConstant = ColorConstant()
    private val controlMousePressedColor: ColorConstant = ColorConstant()
    private val defaultPanelColor: ColorConstant = ColorConstant()
    private val textColor: ColorConstant = ColorConstant()
    private val textColorStart : ColorConstant = ColorConstant() // for lerping purposes

    // durations
    var controlHighlightDuration = 0
        private set
    var controlPanelTransitionDuration = 0
        private set
    var shortTransitionDuration = 0
        private set
    var singleModeToggleDuration = 0
        private set
    var startupTextDisplayDuration = 0
        private set
    var startupTextFadeInDuration = 0
        private set
    var startupTextFadeOutDuration = 0
        private set
    var themeTransitionDuration = 0
        private set

    // names
    var countdownText: String? = null
        private set
    var fontName: String? = null
        private set
    var iconPath: String? = null
        private set
    var shortcutParenStart: String? = null
        private set
    var shortcutParenEnd: String? = null
        private set
    var startupText: String? = null
        private set

    init {
        setTheme(UXThemeType.DARK, null) // Default theme
    }

    fun setTheme(newTheme: UXThemeType, processing: PApplet?) {
        val themeConstants = newTheme.themeConstants

        // sizes and radii
        controlSize = themeConstants.controlSize
        controlHighlightCornerRadius = themeConstants.controlHighlightCornerRadius
        defaultTextMargin = themeConstants.defaultTextMargin
        defaultTextSize = themeConstants.defaultTextSize
        hoverTextSize = themeConstants.hoverTextSize
        hoverTextWidth = themeConstants.hoverTextMaxWidth
        hoverTextMargin = themeConstants.hoverTextMargin
        iconMargin = themeConstants.iconMargin
        startupTextSize = themeConstants.startupTextSize

        // colors
        backGroundColor.setColor(themeConstants.backgroundColor, processing)
        cellColor.setColor(themeConstants.cellColor, processing)
        controlColor.setColor(themeConstants.controlColor, processing)
        controlHighlightColor.setColor(themeConstants.controlHighlightColor, processing)
        controlMousePressedColor.setColor(themeConstants.controlMousePressedColor, processing)
        defaultPanelColor.setColor(themeConstants.defaultPanelColor, processing)
        textColor.setColor(themeConstants.textColor, processing)
        textColorStart.setColor(themeConstants.textColorStart, processing)

        //durations
        controlHighlightDuration = themeConstants.controlHighlightDuration
        controlPanelTransitionDuration = themeConstants.controlPanelTransitionDuration
        shortTransitionDuration = themeConstants.shortTransitionDuration
        singleModeToggleDuration = themeConstants.singleModeToggleDuration
        startupTextDisplayDuration = themeConstants.startupTextDisplayDuration
        startupTextFadeInDuration = themeConstants.startupTextFadeInDuration
        startupTextFadeOutDuration = themeConstants.startupTextFadeOutDuration
        themeTransitionDuration = themeConstants.themeTransitionDuration

        // strings
        countdownText = themeConstants.countdownText
        fontName = themeConstants.fontName
        iconPath = themeConstants.iconPath
        shortcutParenStart = themeConstants.shortcutParenStart
        shortcutParenEnd = themeConstants.shortcutParenEnd
        startupText = themeConstants.startupText
    }

    // colors
    fun getBackGroundColor(): Int {
        return backGroundColor.color
    }

    fun getCellColor(): Int {
        return cellColor.color
    }

    fun getControlColor(): Int {
        return controlColor.color
    }

    fun getControlHighlightColor(): Int {
        return controlHighlightColor.color
    }

    fun getControlMousePressedColor(): Int {
        return controlMousePressedColor.color
    }

    fun getDefaultPanelColor(): Int {
        return defaultPanelColor.color
    }

    fun getTextColor(): Int {
        return textColor.color
    }

    fun getTextColorStart(): Int {
        return textColorStart.color
    }

    companion object {
        @JvmStatic
        val instance: UXThemeManager by lazy { UXThemeManager() }
    }

}