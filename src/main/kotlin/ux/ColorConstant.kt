package ux

import processing.core.PApplet

class ColorConstant {
    private var currentColor = 0
    private var previousColor = 0
    private var transitionStartTime: Long = 0
    private var transitionInProgress = false
    private var p: PApplet? = null
    private var isInitialized = false
    fun setColor(newColor: Int, p: PApplet?) {
        if (isInitialized) {
            previousColor = currentColor
            transitionInProgress = true
            transitionStartTime = System.currentTimeMillis()
            this.p = p
        } else {
            previousColor = newColor
            this.isInitialized = true
        }
        currentColor = newColor
    }

    val color: Int
        get() {
            if (p == null || !transitionInProgress) {
                return currentColor
            }
            val t = (System.currentTimeMillis() - transitionStartTime).toFloat() / TRANSITION_DURATION
            return if (t < 1.0) {
                p!!.lerpColor(previousColor, currentColor, t)
            } else {
                transitionInProgress = false
                currentColor
            }
        }

    companion object {
        private val TRANSITION_DURATION = UXThemeManager.instance.themeTransitionDuration // 2 seconds
    }
}