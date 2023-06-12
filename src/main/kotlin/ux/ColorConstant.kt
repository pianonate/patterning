package ux

import processing.core.PApplet

class ColorConstant(private val p: PApplet) {
    private var currentColor = 0
    private var previousColor = 0
    private var transitionStartTime: Long = 0
    private var transitionInProgress = false
    private var isInitialized = false

    fun setColor(newColor: Int) {
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
                return currentColor
            }
            val t = (System.currentTimeMillis() - transitionStartTime).toFloat() / Theme.themeTransitionDuration
            return if (t < 1.0) {
                p.lerpColor(previousColor, currentColor, t)
            } else {
                transitionInProgress = false
                currentColor
            }
        }
}