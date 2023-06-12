package patterning.actions

abstract class ExtendedKeyCallback(
    keyCombos: LinkedHashSet<KeyCombo> = LinkedHashSet(),
    private val invokeFeatureLambda: () -> Unit,
    private val getUsageTextLambda: () -> String,
    private val invokeModeChangeLambda: (() -> Boolean)? = null,
    private val cleanupFeatureLambda: (() -> Unit)? = null
) : KeyCallback(keyCombos) {

    override fun invokeFeature() = invokeFeatureLambda()
    override fun getUsageText() = getUsageTextLambda()
    override fun invokeModeChange(): Boolean = invokeModeChangeLambda?.invoke() ?: false
    override fun cleanupFeature() = cleanupFeatureLambda?.invoke() ?: Unit
}