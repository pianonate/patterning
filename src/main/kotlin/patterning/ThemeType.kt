package patterning

enum class ThemeType(//
    // Add more themes as needed...
    val themeConstants: UXLightConstants
) {
    DEFAULT(UXLightConstants()),
    DARK(UXDarkConstants())

}