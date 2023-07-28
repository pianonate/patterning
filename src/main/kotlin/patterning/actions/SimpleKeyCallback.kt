package patterning.actions

import processing.event.KeyEvent

abstract class SimpleKeyCallback(
    keyCombos: LinkedHashSet<KeyCombo> = LinkedHashSet(),
    private val invokeFeatureLambda: (() -> Unit),
    private val getUsageTextLambda: (() -> String),
) : KeyCallback {

    private val _keyCombos = keyCombos

    // the interface of KeyCallback
    override fun invokeFeature() = invokeFeatureLambda.invoke()
    override fun getUsageText(): String = getUsageTextLambda.invoke()


    // properties
    override val keyCombos: Set<KeyCombo> = _keyCombos.toSet()
    override val validKeyCombosForCurrentOS: Set<KeyCombo> = _keyCombos.filter { it.isValidForCurrentOS }.toSet()
    override val isValidForCurrentOS: Boolean = _keyCombos.any { it.isValidForCurrentOS }

    // methods
    override fun matches(event: KeyEvent): Boolean = _keyCombos.any { it.matches(event) }

    override fun toString(): String = validKeyCombosForCurrentOS.joinToString(", ") { "$it" }

    companion object {
        // a bunch of factory methods for ease of use
        fun createKeyCallback(
            key: Char,
            invokeFeatureLambda: () -> Unit,
            getUsageTextLambda: () -> String,
        ): SimpleKeyCallback {
            return object : SimpleKeyCallback(
                linkedSetOf(KeyCombo(key.code)),
                invokeFeatureLambda,
                getUsageTextLambda,
            ) {}
        }

        fun createKeyCallback(
            keys: Set<Char>,
            invokeFeatureLambda: () -> Unit,
            getUsageTextLambda: () -> String,
        ): SimpleKeyCallback {
            val keyCombos = keys.mapTo(LinkedHashSet()) { KeyCombo(keyCode = it.code) }
            return object : SimpleKeyCallback(
                keyCombos,
                invokeFeatureLambda,
                getUsageTextLambda,
            ) {}
        }

        fun createKeyCallback(
            keyCombos: Collection<KeyCombo>,
            invokeFeatureLambda: () -> Unit,
            getUsageTextLambda: () -> String,
        ): SimpleKeyCallback {
            return object : SimpleKeyCallback(
                LinkedHashSet(keyCombos),
                invokeFeatureLambda,
                getUsageTextLambda,
            ) {}
        }
    }
}