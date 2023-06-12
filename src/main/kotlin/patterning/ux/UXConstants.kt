package patterning.ux

open class UXConstants internal constructor() {
    val controlSize: Int
        // sizes
        get() = 35
    val controlHighlightCornerRadius: Int
        get() = 10
    val defaultTextMargin: Int
        get() = 5
    val defaultTextSize: Float
        get() = 30f
    val hoverTextMargin: Int
        get() = 5
    val hoverTextMaxWidth: Int
        get() = 225
    val hoverTextSize: Int
        get() = 14
    val iconMargin: Int
        get() = 5
    val startupTextSize: Int
        get() = 50
    open val backgroundColor: Int
        // colors
        get() = 255
    open val cellColor: Int
        get() = -0x1000000
    val controlColor: Int
        get() = -0x37afafb0 // partially transparent 40
    val controlHighlightColor: Int
        get() = -0x376f6f70 // partially transparent 60
    val controlMousePressedColor: Int
        get() = -0x371e1e1f // partially transparent 225
    val defaultPanelColor: Int
        get() = 0x00FFFFFF
    open val textColorStart: Int
        get() = -0x1
    open val textColor: Int
        get() = -0x1000000
    val controlHighlightDuration: Int
        // durations
        get() = 1000
    val controlPanelTransitionDuration: Int
        get() = 1500
    val shortTransitionDuration: Int
        get() = 300
    val singleModeToggleDuration: Int
        get() = 200
    val startupTextDisplayDuration: Int
        get() = 5000
    val startupTextFadeInDuration: Int
        get() = 2000
    val startupTextFadeOutDuration: Int
        get() = 2000
    val themeTransitionDuration: Int
        get() = 500
    val countdownText: String
        // names
        get() = "press space to begin immediately"
    val fontName: String
        get() = "Verdana"
    val iconPath: String
        get() = "icon/"
    val shortcutParenStart: String
        get() = " ("
    val shortcutParenEnd: String
        get() = ")"
    val startupText: String
        get() = "patterning"
}