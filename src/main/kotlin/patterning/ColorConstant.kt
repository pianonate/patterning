package patterning

import processing.core.PApplet

class ColorConstant(private val p: PApplet) {
    internal var transitionInProgress = false

    private var currentColor = 0u
    private var previousColor = 0u
    private var transitionStartTime: Long = 0
    private var isInitialized = false

    fun setColor(newColor: UInt) {
        if (isInitialized) {
            previousColor = currentColor
            transitionInProgress = true
            transitionStartTime = System.currentTimeMillis()
        } else {
            previousColor = newColor
            this.isInitialized = true
        }
        currentColor = newColor
    }

    val color: Int
        get() {
            if (!transitionInProgress) {
                return currentColor.toInt()
            }
            val t = (System.currentTimeMillis() - transitionStartTime).toFloat() / Theme.themeTransitionDuration
            return if (t < 1.0) {
                p.lerpColor(previousColor.toInt(), currentColor.toInt(), t)
            } else {
                transitionInProgress = false
                currentColor.toInt()
            }
        }
}