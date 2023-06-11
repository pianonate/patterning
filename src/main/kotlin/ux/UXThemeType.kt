package ux

enum class UXThemeType(//
    // Add more themes as needed...
    @JvmField val themeConstants: UXConstants
) {
    DEFAULT(UXConstants()),
    DARK(UXDarkConstants())

}