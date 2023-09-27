package patterning.pattern

import patterning.events.KeyboardShortcut
import processing.event.KeyEvent

class SimpleCommand(
    keyboardShortcuts: LinkedHashSet<KeyboardShortcut>,
    private val invokeFeatureLambda: (() -> Unit),
    private val isEnabledLambda: () -> Boolean = { true },
    override val usage: String,
    override val invokeEveryDraw: Boolean = false,
    override val invokeAfterDelay: Boolean = false,
) : Command {

    override fun invokeFeature() = invokeFeatureLambda()
    override val isEnabled: Boolean
        get() = isEnabledLambda()

    override val keyboardShortcuts: Set<KeyboardShortcut> = keyboardShortcuts.toSet()
    override val validKeyCombosForCurrentOS: Set<KeyboardShortcut> = keyboardShortcuts.filter { it.isValidForCurrentOS }.toSet()
    override val isValidForCurrentOS: Boolean = keyboardShortcuts.any { it.isValidForCurrentOS }

    override fun matches(event: KeyEvent): Boolean = keyboardShortcuts.any { it.matches(event) }

    override fun toString(): String {
        val keysStrings = validKeyCombosForCurrentOS.map { it.toString() }
        // special case
        return if (keysStrings.mapNotNull { it.toIntOrNull() } == (1..9).toList()) {
            "1...9"
        } else {
            keysStrings.joinToString(", ")
        }
    }
}