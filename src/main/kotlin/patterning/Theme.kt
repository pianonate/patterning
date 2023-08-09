package patterning

import processing.core.PApplet

object Theme {
    
    var blendMode = 0
        private set
    
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
    var dashedLineDashLength = 0f
        private set
    var dashedLineSpaceLength = 0f
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
    var strokeWeightBounds = 0f
        private set
    var strokeWeightDashedLine = 0f
        private set
    var currentThemeType: ThemeType = ThemeType.DARK
        private set
    
    fun setTheme(newTheme: ThemeType) {
        currentThemeType = newTheme
        
        val themeConstants = newTheme.themeConstants
        blendMode = themeConstants.blendMode
        
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
        startupTextDisplayDuration = themeConstants.startupTextDisplayDuration
        startupTextFadeInDuration = themeConstants.startupTextFadeInDuration
        startupTextFadeOutDuration = themeConstants.startupTextFadeOutDuration
        themeTransitionDuration = themeConstants.themeTransitionDuration
        
        // sizes and radii
        controlSize = themeConstants.controlSize
        controlHighlightCornerRadius = themeConstants.controlHighlightCornerRadius
        dashedLineDashLength = themeConstants.dashedLineDashLength
        dashedLineSpaceLength = themeConstants.dashedLineSpaceLength
        defaultTextMargin = themeConstants.defaultTextMargin
        defaultTextSize = themeConstants.defaultTextSize
        hoverTextSize = themeConstants.hoverTextSize
        hoverTextWidth = themeConstants.hoverTextMaxWidth
        hoverTextMargin = themeConstants.hoverTextMargin
        iconMargin = themeConstants.iconMargin
        startupTextSize = themeConstants.startupTextSize
        strokeWeightBounds = themeConstants.strokeWeightBounds
        strokeWeightDashedLine = themeConstants.strokeWeightDashedLine
        
        // strings
        countdownText = themeConstants.countdownText
        fontName = themeConstants.fontName
        iconPath = themeConstants.iconPath
        shortcutParenStart = themeConstants.shortcutParenStart
        shortcutParenEnd = themeConstants.shortcutParenEnd
        startupText = themeConstants.startupText
        
    }
    
    internal val isTransitioning: Boolean
        get() = _backgroundColor.transitionInProgress
    
    fun init(pApplet: PApplet) {
        _backgroundColor = ColorConstant(pApplet)
        _cellColor = ColorConstant(pApplet)
        _controlColor = ColorConstant(pApplet)
        _controlHighlightColor = ColorConstant(pApplet)
        _controlMousePressedColor = ColorConstant(pApplet)
        _defaultPanelColor = ColorConstant(pApplet)
        _textColor = ColorConstant(pApplet)
        _textColorStart = ColorConstant(pApplet) // for lerping purposes
        setTheme(ThemeType.DARK) // Default theme
    }
    
}