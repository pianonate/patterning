package patterning.actions

import processing.event.KeyEvent

class SimpleKeyCallback(
    keyCombos: LinkedHashSet<KeyCombo>,
    private val invokeFeatureLambda: (() -> Unit),
    override val usage: String,
    override val invokeEveryDraw: Boolean,
) : KeyCallback {

    constructor(
        key: Char,
        invokeFeatureLambda: () -> Unit,
        usage: String,
        invokeEveryDraw: Boolean = false
    ) : this(
        linkedSetOf(KeyCombo(key.code)),
        invokeFeatureLambda,
        usage,
        invokeEveryDraw
    )

    constructor(
        keys: Set<Char>,
        invokeFeatureLambda: () -> Unit,
        usage: String,
        invokeEveryDraw: Boolean = false
    ) : this(
        keys.mapTo(LinkedHashSet()) { KeyCombo(keyCode = it.code) },
        invokeFeatureLambda,
        usage,
        invokeEveryDraw,
    )

    constructor(
        keyCombos: Collection<KeyCombo>,
        invokeFeatureLambda: () -> Unit,
        usage: String,
        invokeEveryDraw: Boolean = false
    ) : this(
        LinkedHashSet(keyCombos),
        invokeFeatureLambda,
        usage,
        invokeEveryDraw,
    )

    // the interface of KeyCallback
    override fun invokeFeature() = invokeFeatureLambda()

    // properties
    override val keyCombos: Set<KeyCombo> = keyCombos.toSet()
    override val validKeyCombosForCurrentOS: Set<KeyCombo> = keyCombos.filter { it.isValidForCurrentOS }.toSet()
    override val isValidForCurrentOS: Boolean = keyCombos.any { it.isValidForCurrentOS }

    // methods
    override fun matches(event: KeyEvent): Boolean = keyCombos.any { it.matches(event) }

    override fun toString(): String {
        val keysStrings = validKeyCombosForCurrentOS.map { it.toString() }
        // special case
        return if (keysStrings.map { it.toIntOrNull() }.filterNotNull() == (1..9).toList()) {
            "1...9"
        } else {
            keysStrings.joinToString(", ")
        }
    }
}