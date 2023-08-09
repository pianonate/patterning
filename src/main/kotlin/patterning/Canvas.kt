package patterning

import java.awt.Color
import java.awt.Component
import processing.core.PApplet
import processing.core.PGraphics

class Canvas(private val pApplet: PApplet) {
    
    private var prevWidth: Int
    private var prevHeight: Int
    var resized = false
        private set
    
    private var backgroundBuffer: PGraphics = PGraphics()
    
    init {
        prevWidth = pApplet.width
        prevHeight = pApplet.height
    }
    
    fun getPGraphics(): PGraphics {
        return pApplet.createGraphics(pApplet.width, pApplet.height)
    }
    
    internal fun drawBackground() {
        resized = (pApplet.width != prevWidth || pApplet.height != prevHeight)
        handleResize()
        prevWidth = pApplet.width
        prevHeight = pApplet.height
        pApplet.image(backgroundBuffer, 0f, 0f)
    }
    
    /**
     * what is going on here? why is this necessary?
     * using background() in the draw loop draws on top of the default grey color for a
     * processing sketch. when you resize a window, for a split second it shows that grey
     * background. which makes the screen flicker massively
     *
     * this is a hacky way to get around that.
     *
     * first off - we have to get the int value of the new theme's background color.
     * the reason we use UInts for all colors is so they can be specified in hex format
     * and it makes it easy to specify the alpha part. just a choice.
     * but in mitigateFlicker we're getting at the surface.native AWT component and we need to
     * set it's background Color. The AWT Color can't just be given an int so we need
     * Processing's PApplet.color fun to convert it to something Color can use and then
     * be applied to the background
     *
     * the discovery on how to fix this flickering took awhile so the documentation is way
     * longer than the code itself :)
     *
     * see for yourself what it looks ike if you change the color to red .color(255,0,0) and
     * then resize the window
     */
    private fun mitigateFlicker() {
        val nativeSurface = pApplet.surface.native as Component
        nativeSurface.background = Color(pApplet.color(Theme.backGroundColor))
    }
    
    /**
     * updating the background needs to happen on any of the following conditions
     * 1. the window has been resized
     * 2. the theme is transitioning (i.e., the background color is changing)
     * 3. the background has not been initialized
     */
    private fun handleResize() {
        if (resized || Theme.isTransitioning || backgroundBuffer.width == 0) {
            backgroundBuffer = getPGraphics()
            backgroundBuffer.beginDraw()
            mitigateFlicker()
            backgroundBuffer.background(Theme.backGroundColor)
            backgroundBuffer.endDraw()
        }
    }
}