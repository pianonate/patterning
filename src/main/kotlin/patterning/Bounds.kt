package patterning

data class Bounds(
    var top: FlexibleInteger,
    var left: FlexibleInteger,
    var bottom: FlexibleInteger,
    var right: FlexibleInteger
) {
    constructor(top: FlexibleInteger, left: FlexibleInteger) : this(top, left, top, left)
}