package patterning

import processing.core.PConstants

class UXDarkConstants internal constructor() : UXLightConstants() {

    override val blendMode: Int
        get() = PConstants.SCREEN

    override val backgroundColor = 0u
    override val boxOutlineColor = 0x22000000u
    override val cellColor = 0xFFFFFFFFu

    override val controlColor = 0xDD404040u
    override val controlHighlightColor = 0xDDAAAAAAu
    override val controlMousePressedColor = 0xDDFFFFFFu

    override val hoverTextColor = 0xFFFFFFFFu
    override val textColorStart = 0xFF000000u
    override val textColor = 0xFFDDDDDDu

}