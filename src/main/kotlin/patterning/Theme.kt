package patterning

import processing.core.PApplet

object Theme {
    var currentThemeType = ThemeType.DARK
        private set
    var blendMode = 0 // used to provide text readability in dark and light mode
        private set

    // durations
    const val CONTROL_HIGHLIGHT_DURATION = 100
    const val CONTROL_PANEL_TRANSITION_DURATION = 300
    const val COUNTDOWN_FROM = 2
    const val SHORT_TRANSITION_DURATION = 300
    const val STARTUP_TEXT_DISPLAY_DURATION = 2000
    const val STARTUP_TEXT_FADE_IN_DURATION = 1000
    const val STARTUP_TEXT_FADE_OUT_DURATION = 2000
    const val STARTUP_TEXT_TRANSITION_DURATION = 2000
    const val THEME_TRANSITION_DURATION = 500

    // names
    const val COUNTDOWN_TEXT = "press a key to start"
    const val FONT_NAME = "Verdana"
    const val ICON_PATH = "icon/"
    const val PAREN_START = " ("
    const val PAREN_END = ")"
    const val STARTUP_TEXT = "patterning"

    // PGraphics names
    const val GRAPHICS_UX = "ux"
    const val GRAPHICS_PATTERN = "pattern"
    const val GRAPHICS_ACCUMULATOR = "accumulator"
    const val GRAPHICS_SIZING = "sizing"

    // sizes
    const val CONTROL_SIZE = 35
    const val CONTROL_HIGHLIGHT_CORNER_RADIUS = 14
    const val DASHED_LINE_DASH_LENGTH = 8
    const val DASHED_LINE_SPACE_LENGTH = 5
    const val DEFAULT_TEXT_MARGIN = 5
    const val DEFAULT_TEXT_SIZE = 30f
    const val HOVER_TEXT_MARGIN = 5
    const val HOVER_TEXT_WIDTH = 225
    const val HOVER_TEXT_SIZE = 14
    const val HUD_TEXT_SIZE = 16
    const val ICON_MARGIN = 5
    const val MOUSE_CIRCLE_SIZE = 10f
    const val STARTUP_TEXT_SIZE = 50
    const val STROKE_WEIGHT_BOUNDS = 1f
    const val STROKE_WEIGHT_DASHED_LINES = 1f

    /**
     * everything related to theme colors including the init
     * is below here - just to allow for easy access to constants
     * at the top of the file
     */
    @Suppress("unused")
    const val RED = 0xFFFF0000.toInt()
    @Suppress("unused")
    const val BLUE = 0xFF0000FF.toInt()
    @Suppress("unused")
    const val GREEN = 0xFF00FF00.toInt()

    private lateinit var _backgroundColor: ColorConstant
    private lateinit var _cellColor: ColorConstant
    private lateinit var _controlColor: ColorConstant
    private lateinit var _controlHighlightColor: ColorConstant
    private lateinit var _controlMousePressedColor: ColorConstant
    private lateinit var _defaultPanelColor: ColorConstant

    private lateinit var _hoverTextColor: ColorConstant
    private lateinit var _textColor: ColorConstant
    private lateinit var _textColorStart: ColorConstant

    private var _ghostAlpha = 0
    private var _cubeAlpha = 0

    val backgroundColor get() = _backgroundColor.color
    val cellColor get() = _cellColor.color
    val controlColor get() = _controlColor.color
    val controlHighlightColor get() = _controlHighlightColor.color
    val controlMousePressedColor get() = _controlMousePressedColor.color
    val defaultPanelColor get() = _defaultPanelColor.color
    val ghostAlpha get() = _ghostAlpha
    val cubeAlpha get() = _cubeAlpha
    const val OPAQUE = 255

    val hoverTextColor get() = _hoverTextColor.color
    val textColor get() = _textColor.color
    val textColorStart get() = _textColorStart.color

    fun init(pApplet: PApplet) {
        _backgroundColor = ColorConstant(pApplet)
        _cellColor = ColorConstant(pApplet)
        _controlColor = ColorConstant(pApplet)
        _controlHighlightColor = ColorConstant(pApplet)
        _controlMousePressedColor = ColorConstant(pApplet)
        _hoverTextColor = ColorConstant(pApplet)
        _defaultPanelColor = ColorConstant(pApplet)
        _textColor = ColorConstant(pApplet)
        _textColorStart = ColorConstant(pApplet)

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
        _hoverTextColor.setColor(themeConstants.hoverTextColor)
        _ghostAlpha = themeConstants.ghostAlpha
        _cubeAlpha = themeConstants.cubeAlpha
        _textColor.setColor(themeConstants.textColor)
        _textColorStart.setColor(themeConstants.textColorStart)

    }
}