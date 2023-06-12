package patterning.ux

import processing.core.PApplet

object Theme {
    private lateinit var p: PApplet

    // colors
    private lateinit var _backgroundColor: ColorConstant
    private lateinit var _cellColor: ColorConstant
    private lateinit var _controlColor: ColorConstant
    private lateinit var _controlHighlightColor: ColorConstant
    private lateinit var _controlMousePressedColor: ColorConstant
    private lateinit var _defaultPanelColor: ColorConstant
    private lateinit var _textColor: ColorConstant
    private lateinit var _textColorStart: ColorConstant  // for lerping purposes

    val backGroundColor: Int
        get() = _backgroundColor.color

    val cellColor: Int
        get() = _cellColor.color

    val controlColor: Int
        get() = _controlColor.color

    val controlHighlightColor: Int
        get() = _controlHighlightColor.color

    val controlMousePressedColor: Int
        get() = _controlMousePressedColor.color

    val defaultPanelColor: Int
        get() = _defaultPanelColor.color

    val textColor: Int
        get() = _textColor.color

    val textColorStart: Int
        get() = _textColorStart.color

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
    var countdownText: String = ""
        private set
    var fontName: String = ""
        private set
    var iconPath: String = ""
        private set
    var shortcutParenStart: String = "'"
        private set
    var shortcutParenEnd: String = ""
        private set
    var startupText: String = ""
        private set

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

    fun setTheme(newTheme: ThemeType) {
        val themeConstants = newTheme.themeConstants
        // colors
        _backgroundColor.setColor(themeConstants.backgroundColor)
        _cellColor.setColor(themeConstants.cellColor)
        _controlColor.setColor(themeConstants.controlColor)
        _controlHighlightColor.setColor(themeConstants.controlHighlightColor)
        _controlMousePressedColor.setColor(themeConstants.controlMousePressedColor)
        _defaultPanelColor.setColor(themeConstants.defaultPanelColor)
        _textColor.setColor(themeConstants.textColor)
        _textColorStart.setColor(themeConstants.textColorStart)

        //durations
        controlHighlightDuration = themeConstants.controlHighlightDuration
        controlPanelTransitionDuration = themeConstants.controlPanelTransitionDuration
        shortTransitionDuration = themeConstants.shortTransitionDuration
        singleModeToggleDuration = themeConstants.singleModeToggleDuration
        startupTextDisplayDuration = themeConstants.startupTextDisplayDuration
        startupTextFadeInDuration = themeConstants.startupTextFadeInDuration
        startupTextFadeOutDuration = themeConstants.startupTextFadeOutDuration
        themeTransitionDuration = themeConstants.themeTransitionDuration

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

        // strings
        countdownText = themeConstants.countdownText
        fontName = themeConstants.fontName
        iconPath = themeConstants.iconPath
        shortcutParenStart = themeConstants.shortcutParenStart
        shortcutParenEnd = themeConstants.shortcutParenEnd
        startupText = themeConstants.startupText
    }

    fun initialize(p: PApplet) {
        this.p = p
        _backgroundColor = ColorConstant(p)
        _cellColor = ColorConstant(p)
        _controlColor = ColorConstant(p)
        _controlHighlightColor = ColorConstant(p)
        _controlMousePressedColor = ColorConstant(p)
        _defaultPanelColor = ColorConstant(p)
        _textColor = ColorConstant(p)
        _textColorStart = ColorConstant(p) // for lerping purposes
        setTheme(ThemeType.DARK) // Default theme
    }

}