package actions

import processing.core.PApplet
import processing.event.KeyEvent

class KeyCombo @JvmOverloads constructor(private val keyCode: Int, private val modifiers: Int = 0, private val validOS: ValidOS = ValidOS.ANY) {

    // we only want to do the conversion once so cache it in the val
    // we do it this way so that we can make this a data class and still have val semantics
    // then the dataclass doesn't need to create an explicit hashCode
    private val cachedKeyCode: Int = if (Character.isLowerCase(keyCode)) Character.toUpperCase(keyCode) else keyCode
    private val processingKeyCode: Int
        get() = cachedKeyCode

    constructor(keyCode: Char, modifiers: Int) : this(keyCode.code, modifiers, ValidOS.ANY)

    // used at runtime to make sure that the incoming keyEvent matches a particular actions.KeyCombo
    fun matches(event: KeyEvent): Boolean {
        return processingKeyCode == event.keyCode && modifiers == event.modifiers &&
                isValidForCurrentOS
    }

    val isValidForCurrentOS: Boolean
        get() = when (validOS) {
            ValidOS.ANY -> true
            ValidOS.MAC -> currentOS === ValidOS.MAC
            ValidOS.NON_MAC -> currentOS === ValidOS.NON_MAC
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val keyCombo = other as KeyCombo
        return processingKeyCode == keyCombo.processingKeyCode && modifiers == keyCombo.modifiers && validOS === keyCombo.validOS
    }

    // later:  “␛” or better yet: ⎋ - but large enough to see - use it for closing info panels
    override fun toString(): String {

        val keyTextBuilder = StringBuilder()

        when {
            modifiers and KeyEvent.META != 0 -> keyTextBuilder.append("⌘")
            modifiers and KeyEvent.CTRL != 0 -> keyTextBuilder.append("^")
            modifiers and KeyEvent.SHIFT != 0 -> keyTextBuilder.append("⇧")
            modifiers and KeyEvent.ALT != 0 -> keyTextBuilder.append("⌥")
        }

        when {
            //Special case for Shift+=, display as +
            modifiers and KeyEvent.SHIFT != 0 && processingKeyCode == '='.code -> keyTextBuilder.append("+")
            processingKeyCode == PApplet.UP -> keyTextBuilder.append("↑")
            processingKeyCode == PApplet.DOWN -> keyTextBuilder.append("↓")
            processingKeyCode == PApplet.LEFT -> keyTextBuilder.append("←")
            processingKeyCode == PApplet.RIGHT -> keyTextBuilder.append("→")
            processingKeyCode == 32 -> keyTextBuilder.append("Space")
            else -> keyTextBuilder.append(processingKeyCode.toChar())
        }

        return keyTextBuilder.toString()
    }

    override fun hashCode(): Int {
        var result = keyCode
        result = 31 * result + modifiers
        result = 31 * result + validOS.hashCode()
        return result
    }

    companion object {
        val currentOS = determineCurrentOS()

        private fun determineCurrentOS(): ValidOS {
            val osName = System.getProperty("os.name").lowercase()
            return if (osName.contains("mac")) {
                ValidOS.MAC
            } else {
                ValidOS.NON_MAC
            }
        }
    }
}