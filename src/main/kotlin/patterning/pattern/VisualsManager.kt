package patterning.pattern

class VisualsManager {

    private val observers: MutableList<Observer> = mutableListOf()

    private data class OptionState(val option: Visual, var isEnabled: Boolean)

    private val renderingOptions: Map<Visual, OptionState> =
        Visual.entries.associateWith { OptionState(it, false) }

    infix fun requires(mode: Visual): Boolean = this.renderingOptions.getValue(mode).isEnabled

    interface Observer {
        fun onStateChanged(changedOption: Visual)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun disable(option: Visual) {
        renderingOptions.getValue(option).isEnabled = false
    }

    fun toggleState(option: Visual) {
        val optionState = renderingOptions.getValue(option)
        optionState.isEnabled = !optionState.isEnabled

        notifyObservers(option)
    }

    private fun notifyObservers(option: Visual) {
        observers.forEach { it.onStateChanged(option) }
    }
}