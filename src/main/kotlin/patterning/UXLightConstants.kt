package patterning

import processing.core.PConstants

open class UXLightConstants internal constructor() {
    open val blendMode: Int
        get() = PConstants.BLEND

    // colors
    open val backgroundColor = 0xFFFFFFFFu
    open val cellColor = 0xFF000000u

    open val controlColor = 0xDD404040u
    open val controlHighlightColor = 0xDDAAAAAAu
    open val controlMousePressedColor = 0xDDFFFFFFu

    open val ghostAlpha = 26
    open val cubeAlpha = 60

    open val hoverTextColor = 0xFFFFFFFFu
    open val textColorStart = 0xFFFFFFFFu
    open val textColor = 0xFF777777u

    /* currently not open */
    val defaultPanelColor = 0x00FFFFFFu
}