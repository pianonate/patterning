package actions

import processing.event.KeyEvent

abstract class KeyCallback(
    keyCombos: LinkedHashSet<KeyCombo> = LinkedHashSet()
) : KeyObservable {

    private val keyObservers: MutableSet<KeyObserver> = HashSet()
    private val _keyCombos = keyCombos

    constructor(key: Char) : this(linkedSetOf(KeyCombo(keyCode = key.code)))
    constructor(keys: Set<Char>) : this(keys.mapTo(LinkedHashSet()) { KeyCombo(keyCode = it.code) })
    constructor(vararg keyCombos: KeyCombo) : this(keyCombos.toCollection(LinkedHashSet()))

    // for the KeyObservable interface
    override fun addObserver(observer: KeyObserver) {
        keyObservers.add(observer)
    }

    // the interface of KeyCallback
    override fun notifyKeyObservers() = keyObservers.forEach { it.notifyKeyPress(this) }
    override fun invokeModeChange() = false
    abstract fun invokeFeature()
    open fun cleanupFeature() {
        // do nothing by default
    }

    abstract fun getUsageText(): String

    // properties
    val keyCombos: Set<KeyCombo> = _keyCombos.toSet()
    val validKeyCombosForCurrentOS: Set<KeyCombo> = _keyCombos.filter { it.isValidForCurrentOS }.toSet()
    val isValidForCurrentOS: Boolean = _keyCombos.any { it.isValidForCurrentOS }

    // methods
    fun matches(event: KeyEvent): Boolean = _keyCombos.any { it.matches(event) }

    override fun toString(): String = validKeyCombosForCurrentOS.joinToString(", ") { "$it" }
}