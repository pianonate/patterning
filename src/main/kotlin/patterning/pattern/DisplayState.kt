package patterning.pattern

class DisplayState {

    private val observers: MutableList<Observer> = mutableListOf()

    private data class OptionState(val option: Behavior, var isEnabled: Boolean)

    private val renderingOptions: Map<Behavior, OptionState> =
        Behavior.entries.associateWith { OptionState(it, false) }

    var boundaryMode: BoundaryMode = BoundaryMode.PatternOnly
        private set

    infix fun expects(mode: Behavior): Boolean = this.renderingOptions.getValue(mode).isEnabled

    interface Observer {
        fun onStateChanged(changedOption: Behavior)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun disable(option: Behavior) {
        renderingOptions.getValue(option).isEnabled = false
    }

    fun toggleState(option: Behavior) {
        val optionState = renderingOptions.getValue(option)
        optionState.isEnabled = !optionState.isEnabled

        if (option == Behavior.Boundary || option == Behavior.BoundaryOnly) {
            updateBoundaryMode()
        }

        notifyObservers(option)
    }

    private fun notifyObservers(option: Behavior) {
        observers.forEach { it.onStateChanged(option) }
    }

    private fun updateBoundaryMode() {
        val isBoundary = renderingOptions.getValue(Behavior.Boundary).isEnabled
        val isBoundaryOnly = renderingOptions.getValue(Behavior.BoundaryOnly).isEnabled

        val newMode = when {
            isBoundary && isBoundaryOnly -> BoundaryMode.BoundaryOnly
            isBoundary && !isBoundaryOnly -> BoundaryMode.PatternAndBoundary
            !isBoundary && isBoundaryOnly -> BoundaryMode.BoundaryOnly
            else -> BoundaryMode.PatternOnly
        }

        when (newMode) {
            BoundaryMode.PatternOnly -> {}
            BoundaryMode.PatternAndBoundary -> {}
            BoundaryMode.BoundaryOnly -> {}
        }

        boundaryMode = newMode
    }
}