package patterning.actions

/**
 * P2D and P3D handled repeated keys differentl so we need to aggregate that
 * and present it more simply in our own key handler so we can
 * deal with single shot and repeated key presses
 */
enum class KeyState(val value: String) {
    PRESSED("pressed"),
    RELEASED("released"),
    TYPED("typed");
    
    override fun toString(): String {
        return value
    }
}