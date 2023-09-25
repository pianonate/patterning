package patterning.pattern

class DisplayState {

    private val observers: MutableList<Observer> = mutableListOf()

    private data class OptionState(val option: RenderingOption, var isEnabled: Boolean)

    private val renderingOptions: Map<RenderingOption, OptionState> =
        RenderingOption.entries.associateWith { OptionState(it, false) }

    // right now these are just
    var boundaryMode: BoundaryMode = BoundaryMode.PatternOnly
        private set

    interface Observer {
        fun onStateChanged(changedOption: RenderingOption)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun toggleState(option: RenderingOption) {
        val optionState = renderingOptions.getValue(option)
        optionState.isEnabled = !optionState.isEnabled

        if (option == RenderingOption.Boundary || option == RenderingOption.BoundaryOnly) {
            updateBoundaryMode()
        }

        notifyObservers(option)
    }

    private fun notifyObservers(option: RenderingOption) {
        observers.forEach { it.onStateChanged(option) }
    }

    private fun updateBoundaryMode() {
        val isBoundary = renderingOptions.getValue(RenderingOption.Boundary).isEnabled
        val isBoundaryOnly = renderingOptions.getValue(RenderingOption.BoundaryOnly).isEnabled

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