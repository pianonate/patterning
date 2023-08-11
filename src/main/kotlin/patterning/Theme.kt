package patterning

import processing.core.PApplet

object Theme {
    
    var currentThemeType = ThemeType.DARK
        private set
    var blendMode = 0 // used to provide text readability in dark and light mode
        private set
    
    val isTransitioning get() = _backgroundColor.transitionInProgress
    
    // durations
    const val controlHighlightDuration = 500
    const val controlPanelTransitionDuration = 1500
    const val shortTransitionDuration = 300
    const val startupTextDisplayDuration = 2000
    const val startupTextFadeInDuration = 3000
    const val startupTextFadeOutDuration = 1000
    const val themeTransitionDuration = 500
    
    // names
    const val countdownText = "press space to begin immediately"
    const val fontName = "Verdana"
    const val iconPath = "icon/"
    const val shortcutParenStart = " ("
    const val shortcutParenEnd = ")"
    const val startupText = "patterning"
    
    // PGraphics names
    const val uxGraphics = "ux"
    const val patternGraphics = "pattern"
    const val sizingGraphics = "sizing"
    
    // sizes
    const val controlSize = 35
    const val controlHighlightCornerRadius = 10
    const val dashedLineDashLength = 6f
    const val dashedLineSpaceLength = 2f
    const val defaultTextMargin = 5
    const val defaultTextSize = 30f
    const val hoverTextMargin = 5
    const val hoverTextWidth = 225
    const val hoverTextSize = 14
    const val iconMargin = 5
    const val startupTextSize = 50
    const val strokeWeightBounds = 3f
    const val strokeWeightDashedLine = 1f
    
    /**
     * everything related to theme colors inluding the init
     * is below here - just to allow for easy access to constants
     * at the top of the file
     */
    private lateinit var _backgroundColor: ColorConstant
    private lateinit var _cellColor: ColorConstant
    private lateinit var _controlColor: ColorConstant
    private lateinit var _controlHighlightColor: ColorConstant
    private lateinit var _controlMousePressedColor: ColorConstant
    private lateinit var _defaultPanelColor: ColorConstant
    private lateinit var _textColor: ColorConstant
    private lateinit var _textColorStart: ColorConstant  // for lerping purposes
    
    val backGroundColor get() = _backgroundColor.color
    val cellColor get() = _cellColor.color
    val controlColor get() = _controlColor.color
    val controlHighlightColor get() = _controlHighlightColor.color
    val controlMousePressedColor get() = _controlMousePressedColor.color
    val defaultPanelColor get() = _defaultPanelColor.color
    val textColor get() = _textColor.color
    val textColorStart get() = _textColorStart.color
    
    fun init(pApplet: PApplet) {
        _backgroundColor = ColorConstant(pApplet)
        _cellColor = ColorConstant(pApplet)
        _controlColor = ColorConstant(pApplet)
        _controlHighlightColor = ColorConstant(pApplet)
        _controlMousePressedColor = ColorConstant(pApplet)
        _defaultPanelColor = ColorConstant(pApplet)
        _textColor = ColorConstant(pApplet)
        _textColorStart = ColorConstant(pApplet) // for lerping purposes
        
        setTheme(ThemeType.DARK)
    }
    
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
        
    }
}