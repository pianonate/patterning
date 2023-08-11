package patterning.panel

import java.util.OptionalInt
import kotlin.math.ceil
import patterning.Canvas
import patterning.DrawBuffer
import patterning.Drawable
import patterning.Drawer
import patterning.Theme
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector

class TextPanel private constructor(builder: Builder) : Panel(builder), Drawable {
    
    // sizes
    private val textMargin = Theme.defaultTextMargin
    private val doubleTextMargin = textMargin * 2
    private val textSize: Float
    private val textWidth: OptionalInt
    private val wrap: Boolean
    private val offsetBottom: Boolean
    
    // optional capabilities
    private val fadeInDuration: OptionalInt
    private val fadeOutDuration: OptionalInt
    private val displayDuration: OptionalInt
    
    // text countdown variables
    private val countdownFrom: OptionalInt
    private val runMethod: Runnable?
    private val initialMessage: String
    private val keepShortCutTogether: Boolean
    private var transitionTime: Long = 0
    private var fadeValue = 0
    
    // the message
    private var lastMessage: String = ""
    private var messageLines: List<String> = mutableListOf()
    
    
    // state management
    private lateinit var state: State
    
    var message: String = ""
        set(value) {
            if (message.isNotEmpty()) lastMessage = field
            field = value
        }
    
    private val sizing: DrawBuffer = canvas.getDrawBuffer(Theme.sizingBuffer, resizable = false)
    
    init {
        
        // construct the TextPanel with the default Panel constructor
        // after that we'll figure out the variations we need to support
        message = builder.message
        
        // just for keyboard shortcuts for now
        keepShortCutTogether = builder.keepShortCutTogether
        textSize = builder.textSize
        textWidth = builder.textWidth
        wrap = builder.wrap
        
        offsetBottom = builder.offsetBottom
        
        displayDuration = builder.displayDuration
        fadeInDuration = builder.fadeInDuration
        fadeOutDuration = builder.fadeOutDuration
        
        // text countdown variables
        runMethod = builder.runMethod
        countdownFrom = builder.countdownFrom
        initialMessage = builder.message
        setTextPanelBuffer()
        
        startDisplay()
    }
    
    private fun startDisplay() {
        state = FadeInState()
        transitionTime = System.currentTimeMillis() // start displaying immediately
    }
    
    /**
     * we go through the above to get the actual
     * height and width of the text so that we can
     * create a PGraphics of that size
     * and _then_ we have to also set font on it!
     *
     * we can't use the current UX.graphics as it causes flickering on it
     * when beginDraw, endDraw are called
     */
    private fun setTextPanelBuffer() {
        
        with(sizing.graphics) {
            beginDraw()
            setFont(graphics = this)
            wrapText(graphics = this)
            endDraw()
            setPanelSize(graphics = this)
        }
        
        with(canvas.getGraphics(width, height)) {
            beginDraw()
            setFont(graphics = this)
            endDraw()
            panelGraphics = this
        }
        
    }
    
    /**
     * the passed in graphics could be any graphics that has had the the correct
     * font set on it
     */
    private fun setPanelSize(graphics: PGraphics) {
        // Compute the maximum width and total height of all lines in case there is
        // word wrapping
        var maxWidth = 0f
        var totalHeight = 0f
        for (line in messageLines) {
            if (graphics.textWidth(line) > maxWidth) {
                maxWidth = graphics.textWidth(line)
            }
            totalHeight += graphics.textAscent() + graphics.textDescent()
        }
        
        // Adjust the width and height according to the size of the wrapped text
        height = ceil((totalHeight + textMargin).toDouble()).toInt()
        
        // take either the specified width - which has just been sized to fit
        // or the width of the longest line of text in case of word wrapping
        width = getTextWidth().coerceAtMost(ceil((maxWidth + doubleTextMargin).toDouble()).toInt())
    }
    
    /**
     * for sizes to be correctly calculated, the font must be the same
     * on both the parent and the newly created text PGraphics
     * necessary because createGraphics doesn't inherit the font from the parent
     */
    private fun setFont(graphics: PGraphics) {
        graphics.textFont(canvas.createFont(Theme.fontName, textSize))
        graphics.textSize(textSize)
    }
    
    private fun getCountdownMessage(count: Long): String {
        return "$initialMessage: $count"
    }
    
    private fun wrapText(graphics: PGraphics) {
        
        if (!wrap) {
            messageLines = listOf(message)
            return
        }
        
        val lines: MutableList<String> = ArrayList()
        
        var line = StringBuilder()
        
        val words: MutableList<String> = message.split(" ").filter { it.isNotEmpty() }.toMutableList()
        
        val evalWidth = getTextWidth()
        while (words.isNotEmpty()) {
            val word = words[0]
            val prospectiveLineWidth = graphics.textWidth(line.toString() + word)
            
            // If the word alone is wider than the wordWrapWidth, it should be put on its own line
            if (prospectiveLineWidth > evalWidth && line.isEmpty()) {
                line.append(word).append(" ")
                words.removeAt(0)
            } else if (prospectiveLineWidth <= evalWidth) {
                line.append(word).append(" ")
                words.removeAt(0)
            } else {
                lines.add(line.toString().trim { it <= ' ' })
                line = StringBuilder()
            }
            if (keepShortCutTogether) {
                // Check if there are exactly two words remaining and they don't fit on the current line
                if (words.size == 2 && graphics.textWidth(line.toString() + words[0] + " " + words[1]) > evalWidth) {
                    // Add the current line to the lines list
                    lines.add(line.toString().trim { it <= ' ' })
                    line = StringBuilder()
                    
                    // Add the last word to the new line
                    line.append(words[0]).append(" ")
                    words.removeAt(0)
                }
            }
        }
        if (line.isNotEmpty()) {
            lines.add(line.toString().trim { it <= ' ' })
        }
        messageLines = lines
    }
    
    private fun getTextWidth(): Int {
        return if (textWidth.isPresent) {
            textWidth.asInt
        } else {
            canvas.width.toInt()
        }
    }
    
    /* called on subclasses to give them the opportunity to swap out the panelBuffer necessary to draw on */
    override fun updatePanelBuffer() {
        if (lastMessage != message) {
            setTextPanelBuffer()
        }
    }
    
    override fun panelSubclassDraw() {
        
        // used for fading in the text and the various states
        // a patterning.ux.panel.TextPanel can advance through
        state.update()
        drawMultiLineText()
    }
    
    private fun drawMultiLineText() {
        
        panelGraphics.beginDraw()
        panelGraphics.pushStyle()
        
        panelGraphics.textAlign(hAlign.toPApplet(), vAlign.toPApplet())
        
        // Determine where to start drawing the text based on the alignment
        val x = when (hAlign) {
            AlignHorizontal.LEFT -> textMargin.toFloat()
            AlignHorizontal.CENTER -> panelGraphics.width / 2f
            AlignHorizontal.RIGHT -> (panelGraphics.width - textMargin).toFloat()
        }
        
        // Determine the starting y position based on the alignment
        val lineHeight = panelGraphics.textAscent() + panelGraphics.textDescent()
        val totalTextHeight = lineHeight * messageLines.size
        val y = when (vAlign) {
            AlignVertical.TOP -> 0f
            AlignVertical.CENTER -> panelGraphics.height / 2f - totalTextHeight / 2f + doubleTextMargin
            AlignVertical.BOTTOM -> (panelGraphics.height - textMargin).toFloat() - totalTextHeight + lineHeight
        }
        
        // Interpolate between start and end colors
        // fade value goes from 0 to 255 to make this happen
        val startColor = Theme.textColorStart
        val endColor = Theme.textColor
        val currentColor = panelGraphics.lerpColor(startColor, endColor, fadeValue / 255.0f)
        
        for (i in messageLines.indices) {
            val line = messageLines[i]
            val lineY = y + lineHeight * i
            
            // Draw the actual text in the calculated color
            panelGraphics.fill(currentColor)
            panelGraphics.text(line, x, lineY)
            
        }
        panelGraphics.popStyle()
        panelGraphics.endDraw()
    }
    
    fun interruptCountdown() {
        runMethod?.run()
        removeFromDrawableList()
    }
    
    private fun removeFromDrawableList() {
        Drawer.remove(this)
    }
    
    private interface State {
        fun update()
        fun transition()
    }
    
    class Builder : Panel.Builder {
        internal val message: String
        internal var wrap = false
        internal var textSize = Theme.defaultTextSize
        internal var fadeInDuration = OptionalInt.empty()
        internal var fadeOutDuration = OptionalInt.empty()
        internal var displayDuration = OptionalInt.empty()
        internal var offsetBottom = false
        
        // Countdown variables
        internal var countdownFrom = OptionalInt.empty()
        internal var textWidth = OptionalInt.empty()
        internal var runMethod: Runnable? = null
        internal var keepShortCutTogether = false
        
        constructor(
            canvas: Canvas,
            message: String = "",
            hAlign: AlignHorizontal,
            vAlign: AlignVertical
        ) : super(
            canvas, hAlign, vAlign
        ) {
            this.message = message
        }
        
        constructor(
            canvas: Canvas,
            message: String,
            position: PVector,
            offsetBottom: Boolean,
            hAlign: AlignHorizontal,
            vAlign: AlignVertical
        ) : super(
            canvas, position, hAlign, vAlign
        ) {
            this.message = message
            this.offsetBottom = offsetBottom
        }
        
        fun textSize(textSize: Int) = apply { this.textSize = textSize.toFloat() }
        
        fun fadeInDuration(fadeInDuration: Int) = apply { this.fadeInDuration = OptionalInt.of(fadeInDuration) }
        
        fun fadeOutDuration(fadeOutDuration: Int) = apply { this.fadeOutDuration = OptionalInt.of(fadeOutDuration) }
        
        fun countdownFrom(countdownFrom: Int) = apply { this.countdownFrom = OptionalInt.of(countdownFrom) }
        
        fun textWidth(textWidth: Int) = apply { this.textWidth = OptionalInt.of(textWidth) }
        
        fun wrap() = apply { wrap = true }
        
        fun keepShortCutTogether() = apply { keepShortCutTogether = true }
        
        fun runMethod(runMethod: Runnable) = apply { this.runMethod = runMethod }
        
        fun displayDuration(displayDuration: Int) = apply { this.displayDuration = OptionalInt.of(displayDuration) }
        
        override fun build() = TextPanel(this)
    }
    
    private inner class FadeInState : State {
        override fun update() {
            val elapsedTime = System.currentTimeMillis() - transitionTime
            fadeValue = if (fadeInDuration.isPresent) {
                PApplet.constrain(
                    PApplet.map(elapsedTime.toFloat(), 0f, fadeInDuration.asInt.toFloat(), 0f, 255f).toInt(),
                    0,
                    255
                )
            } else {
                // fade values range from 0 to 255 so the lerpColor will generate a value from 0 to 1
                255
            }
            if (fadeInDuration.isEmpty || elapsedTime >= fadeInDuration.asInt) {
                transition()
            }
        }
        
        override fun transition() {
            if (countdownFrom.isPresent) {
                state = CountdownState()
                transitionTime = System.currentTimeMillis()
                (state as CountdownState).update() // Force an immediate update after transitioning to the CountdownState
            } else {
                state = DisplayState()
            }
            transitionTime = System.currentTimeMillis()
        }
    }
    
    private inner class DisplayState : State {
        override fun update() {
            val elapsedTime = System.currentTimeMillis() - transitionTime
            if (displayDuration.isPresent && elapsedTime > displayDuration.asInt.toLong()) {
                transition()
            }
        }
        
        override fun transition() {
            if (fadeOutDuration.isPresent) {
                state = FadeOutState()
                transitionTime = System.currentTimeMillis()
            } else {
                removeFromDrawableList()
            }
        }
    }
    
    private inner class CountdownState : State {
        // can only be here if countdownFrom.isPresent()
        var newCount = countdownFrom.asInt.toLong()
        
        init {
            message = getCountdownMessage(newCount)
        }
        
        override fun update() {
            val elapsedTime = System.currentTimeMillis() - transitionTime
            if (elapsedTime >= 1000) { // a second has passed
                transitionTime = System.currentTimeMillis() // reset transitionTime
                newCount--
                if (newCount <= 0) {
                    // Stop the countdown when it reaches 0
                    transition()
                } else {
                    message = "$initialMessage: $newCount"
                }
            }
        }
        
        override fun transition() {
            interruptCountdown()
            state = FadeOutState()
            transitionTime = System.currentTimeMillis()
        }
    }
    
    private inner class FadeOutState : State {
        override fun update() {
            val elapsedTime = System.currentTimeMillis() - transitionTime
            
            // can't be in FadeOutState unless we have a fadeOutDuration - need for the IDE warning
            fadeValue = PApplet.constrain(
                PApplet.map(elapsedTime.toFloat(), 0f, fadeOutDuration.asInt.toFloat(), 255f, 0f).toInt(), 0, 255
            )
            if (elapsedTime >= fadeOutDuration.asInt) {
                transition()
            }
        }
        
        override fun transition() {
            removeFromDrawableList()
        }
    }
}