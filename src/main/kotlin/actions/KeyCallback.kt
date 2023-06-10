package actions

import processing.event.KeyEvent

abstract class KeyCallback(private val keyCombos: LinkedHashSet<KeyCombo> = LinkedHashSet()) : KeyObservable {

    private val keyObservers: MutableSet<KeyObserver> = HashSet()

    constructor(key: Char) : this(linkedSetOf(KeyCombo(key.code)))

    constructor(keys: Set<Char>) : this(keys.mapTo(LinkedHashSet(), { KeyCombo(it.code) }))

    constructor(vararg keyCombos: KeyCombo) : this(keyCombos.toCollection(LinkedHashSet()))

    override fun addObserver(observer: KeyObserver) {
        keyObservers.add(observer)
    }

    override fun notifyKeyObservers() = keyObservers.forEach { it.notifyKeyPress(this) }

    override fun invokeModeChange(): Boolean {
        return false
    }

    abstract fun invokeFeature()

    open fun cleanupFeature() {
        // do nothing by default
    }

    abstract fun getUsageText(): String

    fun getKeyCombos(): Set<KeyCombo> = keyCombos

    val validKeyCombosForCurrentOS: Set<KeyCombo>
        get() = keyCombos.filter { it.isValidForCurrentOS }.toSet()

    fun matches(event: KeyEvent): Boolean = keyCombos.any { it.matches(event) }

    val isValidForCurrentOS: Boolean
        get() = keyCombos.any { it.isValidForCurrentOS }

    override fun toString(): String = validKeyCombosForCurrentOS.joinToString(", ") { "$it" }
}