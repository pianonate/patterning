package patterning.actions

/**
 * these are provided just for readability when instantiating SimpleKeyCallback
 * if you come up with different ways that you want to create KeyCombos than these, just add another extension
 * right now these are all invoked from KeyCallbackFactory
 */
fun Char.toKeyComboSet(): LinkedHashSet<KeyCombo> = linkedSetOf(KeyCombo(this.code))

fun CharRange.toKeyComboSet(): LinkedHashSet<KeyCombo> = this.map { KeyCombo(it.code) }.toCollection(LinkedHashSet())

fun KeyCombo.toKeyComboSet(): LinkedHashSet<KeyCombo> = linkedSetOf(this)

fun Pair<KeyCombo, KeyCombo>.toKeyComboSet(): LinkedHashSet<KeyCombo> = linkedSetOf(first, second)

fun Set<Int>.toKeyComboSet():  LinkedHashSet<KeyCombo> = this.mapTo(LinkedHashSet()) { KeyCombo(it) }