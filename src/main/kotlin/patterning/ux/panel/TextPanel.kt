package patterning.ux.panel

import patterning.ux.Drawable
import patterning.ux.DrawableManager
import patterning.ux.Theme
import patterning.ux.informer.DrawingInfoSupplier
import patterning.ux.informer.DrawingInformer
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector
import java.util.*
import java.util.function.IntSupplier
import kotlin.math.ceil

class TextPanel private constructor(builder: Builder) : Panel(builder), Drawable {

    // sizes
    private val textMargin = Theme.defaultTextMargin
    private val doubleTextMargin = textMargin * 2
    private val textSize: Float
    private val textWidth: OptionalInt
    private val textWidthSupplier: Optional<IntSupplier>
    private val wrap: Boolean

    // optional capabilities
    private val fadeInDuration: OptionalInt
    private val fadeOutDuration: OptionalInt
    private val displayDuration // long to compare to System.currentTimeMillis()
            : OptionalLong

    // text countdown variables
    private val countdownFrom: OptionalInt
    private val runMethod: Runnable?
    private val initialMessage: String
    private val keepShortCutTogether: Boolean
    private var transitionTime: Long = 0
    private var fadeValue = 0

    // the message
    private var message: String
    private var lastMessage: String
    private var messageLines: List<String>? = null


    // state management
    private var state: State? = null

    init {
        // construct the TextPanel with the default Panel constructor
        // after that we'll figure out the variations we need to support
        message = builder.message
        lastMessage = builder.message

        // just for keyboard shortcuts for now
        keepShortCutTogether = builder.keepShortCutTogether
        textSize = builder.textSize
        textWidth = builder.textWidth
        textWidthSupplier = builder.textWidthSupplier
        wrap = builder.wrap
        displayDuration = builder.displayDuration
        fadeInDuration = builder.fadeInDuration
        fadeOutDuration = builder.fadeOutDuration

        // text countdown variables
        runMethod = builder.runMethod
        countdownFrom = builder.countdownFrom
        initialMessage = builder.message

        // create initial panelBuffer for the text
        panelBuffer = getTextPanelBuffer(drawingInformer.supplyPGraphics())

        // automatically start the display unless we're a countdown
        // which needs to be manually invoked by the caller...
        if (countdownFrom.isPresent) {
            startCountdown()
        } else {
            startDisplay()
        }
    }

    // Countdown methods
    private fun startCountdown() {
        setMessage(initialMessage)
        startDisplay()
    }

    private fun startDisplay() {
        state = FadeInState()
        transitionTime = System.currentTimeMillis() // start displaying immediately
    }

    private fun getTextPanelBuffer(parentBuffer: PGraphics): PGraphics {

        val testMessage = if (countdownFrom.isPresent) getCountdownMessage(countdownFrom.asInt.toLong()) else message
        messageLines = wrapText(testMessage, parentBuffer)


        // Adjust the text size if it exceeds the bounds of the screen
        val adjustedTextSize = getAdjustedTextSize(parentBuffer, messageLines, textSize)

        // Compute the maximum width and total height of all lines in case there is
        // word wrapping
        var maxWidth = 0f
        var totalHeight = 0f
        for (line in messageLines!!) {
            if (parentBuffer.textWidth(line) > maxWidth) {
                maxWidth = parentBuffer.textWidth(line)
            }
            totalHeight += parentBuffer.textAscent() + parentBuffer.textDescent()
        }

        // Adjust the width and height according to the size of the wrapped text
        height = ceil((totalHeight + textMargin).toDouble()).toInt()

        // take either the specified width - which has just been sized to fit
        // or the width of the longest line of text in case of word wrapping
        width = getTextWidth().coerceAtMost(ceil((maxWidth + doubleTextMargin).toDouble()).toInt())
        val textBuffer = parentBuffer.parent.createGraphics(width, height)

        // set the font for this PGraphics as it will not change
        textBuffer.beginDraw()
        setFont(textBuffer, adjustedTextSize)
        textBuffer.endDraw()
        return textBuffer
    }

    fun setMessage(message: String) {
        lastMessage = this.message
        this.message = message
    }

    // for sizes to be correctly calculated, the font must be the same
    // on both the parent and the new textBuffer
    // necessary because createGraphics doesn't inherit the font from the parent
    private fun setFont(buffer: PGraphics, textSize: Float) {
        val informer = drawingInformer as DrawingInformer
        val shouldInitialize = !informer.isDrawing()
        if (shouldInitialize) buffer.beginDraw()
        buffer.textFont(buffer.parent.createFont(Theme.fontName, textSize))
        buffer.textSize(textSize)
        if (shouldInitialize) buffer.endDraw()
    }

    private fun getCountdownMessage(count: Long): String {
        return "$initialMessage: $count"
    }

    private fun wrapText(theMessage: String, buffer: PGraphics): List<String> {
        val words: MutableList<String> =
            ArrayList(listOf(*theMessage.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()))
        val lines: MutableList<String> = ArrayList()
        var line = StringBuilder()
        if (!wrap) {
            lines.add(theMessage)
            return lines
        }
        setFont(buffer, textSize)
        val evalWidth = getTextWidth()
        while (words.isNotEmpty()) {
            val word = words[0]
            val prospectiveLineWidth = buffer.textWidth(line.toString() + word)

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
                if (words.size == 2 && buffer.textWidth(line.toString() + words[0] + " " + words[1]) > evalWidth) {
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
        return lines
    }

    // used to make sure that a long line will fit on the screen at this size
    private fun getAdjustedTextSize(parentBuffer: PGraphics, lines: List<String>?, startingSize: Float): Float {

        // inherently we can have word wrapped text so make sure that we find the longest line to get the width
        // set the font on the buffer we're using to evaluate text width
        setFont(parentBuffer, startingSize)
        var adjustedTextSize = startingSize
        var longestLine: String? = ""

        // Determine the longest line
        for (line in lines!!) {
            if (parentBuffer.textWidth(line) > parentBuffer.textWidth(longestLine)) {
                longestLine = line
            }
        }
        val targetWidth = getTextWidth()

        // fit within the width minus a margin
        while (parentBuffer.textWidth(longestLine) > targetWidth - doubleTextMargin /*|| ((parentBuffer.textAscent() + parentBuffer.textDescent()) > (parentBuffer.height - doubleTextMargin))*/) {
            adjustedTextSize -= .1.toFloat() // smooth baby
            adjustedTextSize = adjustedTextSize.coerceAtLeast(1f) // Prevent the textSize from going below 1
            parentBuffer.textSize(adjustedTextSize)
        }
        return adjustedTextSize
    }

    private fun getTextWidth(): Int {
        return if (textWidth.isPresent) {
            textWidth.asInt
        } else {
            textWidthSupplier.map { intSupplier: IntSupplier -> OptionalInt.of(intSupplier.asInt) }.get().asInt
        }
    }

    /* called on subclasses to give them the opportunity to swap out the panelBuffer necessary to draw on */
    override fun updatePanelBuffer() {
        if (
            (lastMessage != message) ||
            (drawingInformer.isResized())
        ) {
            panelBuffer = getTextPanelBuffer(drawingInformer.supplyPGraphics())
            messageLines = wrapText(message, drawingInformer.supplyPGraphics())
        }
    }

    override fun panelSubclassDraw() {

        // used for fading in the text and the various states
        // a patterning.ux.panel.TextPanel can advance through
        state!!.update()

        drawMultiLineText()
    }

    private fun drawMultiLineText() {

        panelBuffer.beginDraw()
        panelBuffer.pushStyle()

        panelBuffer.textAlign(hAlign!!.toPApplet(), vAlign!!.toPApplet())

        // Determine where to start drawing the text based on the alignment
        var x = textMargin.toFloat()
        var y = 0f
        when (hAlign) {
            AlignHorizontal.LEFT -> x = textMargin.toFloat()
            AlignHorizontal.CENTER -> x = panelBuffer.width / 2f
            AlignHorizontal.RIGHT -> x = (panelBuffer.width - textMargin).toFloat()
            else -> {}
        }

        // Determine the starting y position based on the alignment
        //todo: for multiline you can use textLeading to control the actual spacing...
        val lineHeight = panelBuffer.textAscent() + panelBuffer.textDescent()
        val totalTextHeight = lineHeight * messageLines!!.size
        when (vAlign) {
            AlignVertical.CENTER -> y = panelBuffer.height / 2f - totalTextHeight / 2f + doubleTextMargin
            AlignVertical.BOTTOM -> y = (panelBuffer.height - textMargin).toFloat()
            else -> {}
        }

        // Interpolate between start and end colors
        // fade value goes from 0 to 255 to make this happen
        val startColor = Theme.textColorStart
        val endColor = Theme.textColor
        val currentColor = panelBuffer.lerpColor(startColor, endColor, fadeValue / 255.0f)

        for (i in messageLines!!.indices) {
            val line = messageLines!![i]
            val lineY = y + lineHeight * i

            // Draw the actual text in the calculated color
            panelBuffer.fill(currentColor)
            //println(currentColor)
            panelBuffer.text(line, x, lineY)

        }
        panelBuffer.popStyle()
        panelBuffer.endDraw()
    }

    fun interruptCountdown() {
        runMethod?.run()
        removeFromDrawableList()
    }

    private fun removeFromDrawableList() {
        drawableManager!!.remove(this)
    }

    private interface State {
        fun update()
        fun transition()
    }

    class Builder : Panel.Builder<Builder> {
        internal val message: String
        internal var wrap = false
        internal var textSize = Theme.defaultTextSize
        internal var fadeInDuration = OptionalInt.empty()
        internal var fadeOutDuration = OptionalInt.empty()
        internal var displayDuration = OptionalLong.empty()

        // Countdown variables
        internal var countdownFrom = OptionalInt.empty()
        internal var textWidth = OptionalInt.empty()
        internal var textWidthSupplier = Optional.empty<IntSupplier>()
        internal var runMethod: Runnable? = null
        internal var keepShortCutTogether = false

        constructor(
            informer: DrawingInfoSupplier?,
            message: String,
            hAlign: AlignHorizontal?,
            vAlign: AlignVertical?
        ) : super(
            informer!!, hAlign!!, vAlign!!
        ) {
            this.message = message
        }

        constructor(
            informer: DrawingInfoSupplier?,
            message: String,
            position: PVector?,
            hAlign: AlignHorizontal?,
            vAlign: AlignVertical?
        ) : super(
            informer!!, position!!, hAlign!!, vAlign!!
        ) {
            this.message = message
        }

        fun textSize(textSize: Int): Builder {
            this.textSize = textSize.toFloat()
            return this
        }

        fun fadeInDuration(fadeInDuration: Int): Builder {
            this.fadeInDuration = OptionalInt.of(fadeInDuration)
            return this
        }

        fun fadeOutDuration(fadeOutDuration: Int): Builder {
            this.fadeOutDuration = OptionalInt.of(fadeOutDuration)
            return this
        }

        fun countdownFrom(countdownFrom: Int): Builder {
            this.countdownFrom = OptionalInt.of(countdownFrom)
            return this
        }

        fun textWidth(textWidth: Int): Builder {
            this.textWidth = OptionalInt.of(textWidth)
            check(!textWidthSupplier.isPresent) { "Cannot set both int textWidth and Optional<IntSupplier> textWidth" }
            return this
        }

        fun textWidth(textWidth: Optional<IntSupplier>): Builder {
            textWidthSupplier = textWidth
            check(!this.textWidth.isPresent) { "Cannot set both int textWidth and Optional<IntSupplier> textWidth" }
            return this
        }

        fun wrap(): Builder {
            wrap = true
            return this
        }

        fun keepShortCutTogether(): Builder {
            keepShortCutTogether = true
            return this
        }

        fun runMethod(runMethod: Runnable?): Builder {
            this.runMethod = runMethod
            return this
        }

        fun displayDuration(displayDuration: Long): Builder {
            this.displayDuration = OptionalLong.of(displayDuration)
            return this
        }


        override fun self(): Builder {
            return this
        }

        override fun build(): TextPanel {
            if (textWidth.isEmpty && textWidthSupplier.isEmpty) {
                textWidth = OptionalInt.of(drawingInformer.supplyPGraphics().width)
            }
            return TextPanel(this)
        }
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
            if (displayDuration.isPresent && elapsedTime > displayDuration.asLong) {
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
            setMessage(getCountdownMessage(newCount))
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
                    setMessage("$initialMessage: $newCount")
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

    companion object {
        private val drawableManager = DrawableManager.instance
    }
}