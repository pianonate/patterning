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
    private val backGroundColor: ColorConstant
    private val cellColor: ColorConstant
    private val controlColor: ColorConstant
    private val controlHighlightColor: ColorConstant
    private val controlMousePressedColor: ColorConstant
    private val defaultPanelColor: ColorConstant
    private val textColor: ColorConstant
    private val textColorStart // for lerping purposes
            : ColorConstant

    // durations
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
    // strings
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
        backGroundColor = ColorConstant()
        cellColor = ColorConstant()
        controlColor = ColorConstant()
        controlHighlightColor = ColorConstant()
        controlMousePressedColor = ColorConstant()
        defaultPanelColor = ColorConstant()
        textColor = ColorConstant()
        textColorStart = ColorConstant()
        setTheme(UXThemeType.DARK, null, true) // Default theme
    }

    private fun setTheme(newTheme: UXThemeType, processing: PApplet?, underConstruction: Boolean) {
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

    fun setTheme(newTheme: UXThemeType, processing: PApplet?) {
        setTheme(newTheme, processing, false)
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