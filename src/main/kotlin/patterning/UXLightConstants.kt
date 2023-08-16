package patterning

import processing.core.PConstants

open class UXLightConstants internal constructor() {
    open val blendMode: Int
        get() = PConstants.BLEND
    
    // colors
    open val backgroundColor = 0xFFu
    open val cellColor = 0xFF000000u
    open val ghostColor = 0x11000000u
    open val controlColor = 0xDD404040u // partially transparent 40
    open val controlHighlightColor = 0xDD0000FFu // partially transparent 60
    open val controlMousePressedColor = 0xDDE1E1E1u // partially transparent 225
    open val hoverTextColor = 0xFFFFFFFFu
    open val textColorStart = 0xFFFFFFFFu
    open val textColor = 0xFFAAAAFFu
    
    /* currently not open */
    val defaultPanelColor
        get() = 0x00FFFFFFu
}