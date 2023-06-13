package patterning.actions

import processing.event.KeyEvent

abstract class KeyCallback(
    keyCombos: LinkedHashSet<KeyCombo> = LinkedHashSet(),
    private val invokeFeatureLambda: (() -> Unit)? = null,
    private val getUsageTextLambda: (() -> String)? = null,
    private val invokeModeChangeLambda: (() -> Boolean)? = null,
    private val cleanupFeatureLambda: (() -> Unit)? = null
) : KeyObservable {

    private val keyObservers: MutableSet<KeyObserver> = HashSet()
    private val _keyCombos = keyCombos

    // for the KeyObservable interface
    override fun addObserver(observer: KeyObserver) {
        keyObservers.add(observer)
    }

    // KeyObservable overrides
    override fun notifyKeyObservers() = keyObservers.forEach { it.notifyKeyPress(this) }
    override fun invokeModeChange(): Boolean = invokeModeChangeLambda?.invoke() ?: false

    // the interface of KeyCallback
    fun invokeFeature() = invokeFeatureLambda?.invoke() ?: Unit
    open fun cleanupFeature() = cleanupFeatureLambda?.invoke() ?: Unit
    fun getUsageText(): String = getUsageTextLambda?.invoke() ?: ""


    // properties
    val keyCombos: Set<KeyCombo> = _keyCombos.toSet()
    val validKeyCombosForCurrentOS: Set<KeyCombo> = _keyCombos.filter { it.isValidForCurrentOS }.toSet()
    val isValidForCurrentOS: Boolean = _keyCombos.any { it.isValidForCurrentOS }

    // methods
    fun matches(event: KeyEvent): Boolean = _keyCombos.any { it.matches(event) }

    override fun toString(): String = validKeyCombosForCurrentOS.joinToString(", ") { "$it" }

    companion object {
        // a bunch of factory methods for ease of use
        fun createKeyCallback(
            key: Char,
            invokeFeatureLambda: () -> Unit,
            getUsageTextLambda: () -> String,
            invokeModeChangeLambda: (() -> Boolean)? = null,
            cleanupFeatureLambda: (() -> Unit)? = null
        ): KeyCallback {
            return object : KeyCallback(
                linkedSetOf(KeyCombo(key.code)),
                invokeFeatureLambda,
                getUsageTextLambda,
                invokeModeChangeLambda,
                cleanupFeatureLambda
            ) {}
        }

        fun createKeyCallback(
            keys: Set<Char>,
            invokeFeatureLambda: () -> Unit,
            getUsageTextLambda: () -> String,
            invokeModeChangeLambda: (() -> Boolean)? = null,
            cleanupFeatureLambda: (() -> Unit)? = null
        ): KeyCallback {
            val keyCombos = keys.mapTo(LinkedHashSet()) { KeyCombo(keyCode = it.code) }
            return object : KeyCallback(
                keyCombos,
                invokeFeatureLambda,
                getUsageTextLambda,
                invokeModeChangeLambda,
                cleanupFeatureLambda
            ) {}
        }

        fun createKeyCallback(
            keyCombos: Collection<KeyCombo>,
            invokeFeatureLambda: () -> Unit,
            getUsageTextLambda: () -> String,
            invokeModeChangeLambda: (() -> Boolean)? = null,
            cleanupFeatureLambda: (() -> Unit)? = null
        ): KeyCallback {
            return object : KeyCallback(
                LinkedHashSet(keyCombos),
                invokeFeatureLambda,
                getUsageTextLambda,
                invokeModeChangeLambda,
                cleanupFeatureLambda
            ) {}
        }
    }
}