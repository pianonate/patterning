package patterning

enum class ThemeType(//
    // Add more themes as needed...
    val themeConstants: UXConstants
) {
    DEFAULT(UXConstants()),
    DARK(UXDarkConstants())

}