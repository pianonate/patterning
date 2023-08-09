package patterning

import java.awt.Color
import java.awt.Component
import java.math.BigDecimal
import processing.core.PApplet
import processing.core.PGraphics

class Canvas(private val pApplet: PApplet) {
    
    private var prevWidth: Int = 0
    private var prevHeight: Int = 0
    var resized = false
        private set
    var width: BigDecimal = BigDecimal.ZERO
        private set
    var height: BigDecimal = BigDecimal.ZERO
        private set
    
    private var backgroundBuffer: PGraphics = PGraphics()
    
    init {
        updateDimensions()
    }
    
    fun getPGraphics(): PGraphics {
        return pApplet.createGraphics(pApplet.width, pApplet.height)
    }
    
    internal fun drawBackground() {
        resized = (pApplet.width != prevWidth || pApplet.height != prevHeight)
        handleResize()
        pApplet.image(backgroundBuffer, 0f, 0f)
    }
    
    internal fun updateDimensions() {
        prevWidth = pApplet.width
        prevHeight = pApplet.height
        width = BigDecimal(pApplet.width)
        height = BigDecimal(pApplet.height)
    }
    
    /**
     * what is going on here? why is this necessary?
     * using background() in the draw loop draws on top of the default grey color for a
     * processing sketch. when you resize a window, for a split second it shows that grey
     * background. which makes the screen flicker massively
     *
     * this is a hacky way to get around that.
     *
     * first off - we have to get the theme's background color. we use this because when
     * the theme is transitioning, this color changes for the duration of the transition.
     * to mitigateFlicker we get the surface.native AWT component and
     * set it's background Color. The AWT Color can't just be given an int so we need
     * Processing's PApplet.color fun to convert it to something Color can use and then
     * be applied to the native AWT background
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
            updateDimensions()
            
            backgroundBuffer = getPGraphics()
            backgroundBuffer.beginDraw()
            mitigateFlicker()
            backgroundBuffer.background(Theme.backGroundColor)
            backgroundBuffer.endDraw()
        }
    }
}