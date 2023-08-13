package patterning

import processing.core.PConstants

class UXDarkConstants internal constructor() : UXLightConstants() {
    
    override val blendMode:Int
        get() = PConstants.SCREEN
    
    override val backgroundColor = 0u
    override val cellColor = 0xFFFFFFFFu
    override val ghostColor = 0x11FFFFFFu
    override val textColorStart = 0xFF000000u
    override val textColor = 0xFFFFFFFFu
    override val controlColor = 0xDD303030u
    override val controlHighlightColor = 0xDD606060u
    override val controlMousePressedColor = 0xDDCCCCCCu

}