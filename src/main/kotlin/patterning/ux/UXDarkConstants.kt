package patterning.ux

class UXDarkConstants internal constructor() : UXConstants() {

    override val backgroundColor: UInt
        get() = 0u
    override val cellColor: UInt
        get() = 0xFFFFFFFFu
    override val textColorStart: UInt
        get() = 0xFF000000u
    override val textColor: UInt
        get() = 0xCCCCCCCCu

    override val controlColor: UInt
        get() = 0xDD303030u
    override val controlHighlightColor: UInt
        get() = 0xDD606060u
    override val controlMousePressedColor: UInt
        get() = 0xDDCCCCCCu

}