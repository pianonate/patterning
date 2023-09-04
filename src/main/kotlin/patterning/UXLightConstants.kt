package patterning

import processing.core.PConstants

open class UXLightConstants internal constructor() {
    open val blendMode: Int
        get() = PConstants.BLEND

    // colors
    open val backgroundColor = 0xFFu
    open val boxOutlineColor = 0xDDFFFFFFu
    open val cellColor = 0xFF000000u
    open val ghostColor = 0x11000000u
    open val controlColor = 0xDD404040u
    open val controlHighlightColor = 0xDDFFFFFFu
    open val controlMousePressedColor = 0xDDE1E1E1u
    open val hoverTextColor = 0xFFFFFFFFu
    open val textColorStart = 0xFFFFFFFFu
    open val textColor = 0xFF777777u

    /* currently not open */
    val defaultPanelColor
        get() = 0x00FFFFFFu
}