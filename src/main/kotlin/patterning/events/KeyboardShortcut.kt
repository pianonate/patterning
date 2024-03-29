package patterning.events

import processing.core.PApplet
import processing.event.KeyEvent

class KeyboardShortcut(
    private val keyCode: Int,
    private val modifiers: Int = 0,
    private val validOS: ValidOS = ValidOS.ANY
) {

    private val cachedKeyCode: Int = if (Character.isLowerCase(keyCode)) Character.toUpperCase(keyCode) else keyCode
    private val processingKeyCode: Int
        get() = cachedKeyCode

    constructor(keyCode: Char, modifiers: Int) : this(keyCode.code, modifiers, ValidOS.ANY)

    // used at runtime to make sure that the incoming keyEvent matches a particular patterning.actions.KeyCombo
    fun matches(event: KeyEvent): Boolean {
        return processingKeyCode == event.keyCode && modifiers == event.modifiers && isValidForCurrentOS
    }

    val isValidForCurrentOS: Boolean
        get() = when (validOS) {
            ValidOS.ANY -> true
            ValidOS.MAC -> currentOS === ValidOS.MAC
            ValidOS.NON_MAC -> currentOS === ValidOS.NON_MAC
        }

    // later:  “␛” or better yet: ⎋ - but large enough to see - use it for closing info panels
    override fun toString(): String {
        // Special case for Shift+=, display as +
        if (modifiers and KeyEvent.SHIFT != 0 && keyCode == '='.code) {
            return "+"
        }
        val keyTextBuilder = StringBuilder()

        when {
            modifiers and KeyEvent.META != 0 -> keyTextBuilder.append(META_KEY)
            modifiers and KeyEvent.CTRL != 0 -> keyTextBuilder.append("^")
            modifiers and KeyEvent.SHIFT != 0 -> keyTextBuilder.append("⇧")
            modifiers and KeyEvent.ALT != 0 -> keyTextBuilder.append("⌥")
        }

        when (processingKeyCode) {
            PApplet.UP -> keyTextBuilder.append("↑")
            PApplet.DOWN -> keyTextBuilder.append("↓")
            PApplet.LEFT -> keyTextBuilder.append("←")
            PApplet.RIGHT -> keyTextBuilder.append("→")
            32 -> keyTextBuilder.append("Space")
            else -> keyTextBuilder.append(processingKeyCode.toChar())
        }

        return keyTextBuilder.toString()
    }

    companion object {
        val currentOS = determineCurrentOS()

        private fun determineCurrentOS(): ValidOS {
            val osName = System.getProperty("os.name").lowercase()
            return if (osName.contains("mac")) ValidOS.MAC else ValidOS.NON_MAC

        }

        const val META_KEY = "⌘"
    }
}