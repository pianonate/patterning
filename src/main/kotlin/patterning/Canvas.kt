
import processing.core.PApplet
import processing.core.PGraphics

class Canvas(private val pApplet: PApplet) {
    
    fun getPGraphics(): PGraphics {
        return pApplet.createGraphics(pApplet.width, pApplet.height)
    }
}