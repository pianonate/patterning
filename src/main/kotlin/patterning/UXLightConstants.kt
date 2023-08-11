package patterning

import processing.core.PConstants

open class UXLightConstants internal constructor() {
    open val blendMode: Int
        get() = PConstants.BLEND
    
    // colors
    open val backgroundColor = 0xFFu
    open val cellColor = 0xFF000000u
    open val controlColor = 0xDD404040u // partially transparent 40
    open val controlHighlightColor = 0xDD909090u // partially transparent 60
    open val controlMousePressedColor = 0xDDE1E1E1u // partially transparent 225
    open val textColorStart = 0xFFFFFFFFu
    open val textColor = 0xFF444444u
    
    /* currently not open */
    val defaultPanelColor
        get() = 0x00FFFFFFu
}