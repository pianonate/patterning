package patterning.actions

import processing.event.KeyEvent

class SimpleKeyCallback(
    keyCombos: LinkedHashSet<KeyCombo>,
    private val invokeFeatureLambda: (() -> Unit),
    private val isEnabledLambda: () -> Boolean = { true },
    override val usage: String,
    override val invokeEveryDraw: Boolean = false,
    override val invokeAfterDelay: Boolean = false,
) : KeyCallback {

    // the interface of KeyCallback
    override fun invokeFeature() = invokeFeatureLambda()
    override val isEnabled: Boolean
        get() = isEnabledLambda()

    // properties
    override val keyCombos: Set<KeyCombo> = keyCombos.toSet()
    override val validKeyCombosForCurrentOS: Set<KeyCombo> = keyCombos.filter { it.isValidForCurrentOS }.toSet()
    override val isValidForCurrentOS: Boolean = keyCombos.any { it.isValidForCurrentOS }

    // methods
    override fun matches(event: KeyEvent): Boolean = keyCombos.any { it.matches(event) }

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