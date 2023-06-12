package patterning.ux

enum class ThemeType(//
    // Add more themes as needed...
    val themeConstants: UXConstants
) {
    DEFAULT(UXConstants()),
    DARK(UXDarkConstants())

}