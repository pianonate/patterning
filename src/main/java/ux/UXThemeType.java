package ux;

public enum UXThemeType {
    DEFAULT(new UXConstants()),
    DARK(new UXDarkConstants());  //
    // Add more themes as needed...

    private final UXConstants themeConstants;

    UXThemeType(UXConstants themeConstants) {
        this.themeConstants = themeConstants;
    }

    public UXConstants getThemeConstants() {
        return this.themeConstants;
    }
}