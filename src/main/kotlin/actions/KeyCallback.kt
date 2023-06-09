package actions

import processing.event.KeyEvent

abstract class KeyCallback(private val keyCombos: LinkedHashSet<KeyCombo> = LinkedHashSet()) : KeyObservable {

    private val keyObservers: MutableSet<KeyObserver> = HashSet()

    constructor(key: Char) : this(LinkedHashSet(listOf(KeyCombo(key.code))))

    constructor(keys: Set<Char>) : this(keys.map { KeyCombo(it.code) }.let { LinkedHashSet(it) })

    constructor(vararg keyCombos: KeyCombo) : this(LinkedHashSet(keyCombos.toList()))

    override fun addObserver(observer: KeyObserver) {
        keyObservers.add(observer)
    }

    override fun notifyKeyObservers() {
        for (keyObserver in keyObservers) {
            keyObserver.notifyKeyPress(this)
        }
    }

    override fun invokeModeChange(): Boolean {
        return false
    }

    abstract fun invokeFeature()

    open fun cleanupFeature() {
        // do nothing by default
    }

    abstract fun getUsageText(): String

    fun getKeyCombos(): Set<KeyCombo> {
        return keyCombos
    }

    val validKeyCombosForCurrentOS: Set<KeyCombo>
        get() = keyCombos.filter { it.isValidForCurrentOS }.toSet()

    fun matches(event: KeyEvent): Boolean {
        return keyCombos.any { it.matches(event) }
    }

    val isValidForCurrentOS: Boolean
        get() = keyCombos.any { it.isValidForCurrentOS }

    override fun toString(): String {
        return validKeyCombosForCurrentOS.joinToString(", ") { it.toString() }
    }
}
