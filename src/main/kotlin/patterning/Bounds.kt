package patterning

import java.math.BigInteger

data class Bounds(
    var top: BigInteger,
    var left: BigInteger,
    var bottom: BigInteger,
    var right: BigInteger
) {
    constructor(top: BigInteger, left: BigInteger) : this(top, left, top, left)
}