package patterning.pattern

class DisplayState {

    private val observers: MutableList<Observer> = mutableListOf()

    private data class OptionState(val option: DisplayMode, var isEnabled: Boolean)

    private val renderingOptions: Map<DisplayMode, OptionState> =
        DisplayMode.entries.associateWith { OptionState(it, false) }

    var boundaryMode: BoundaryMode = BoundaryMode.PatternOnly
        private set

    infix fun expects(mode: DisplayMode): Boolean = this.renderingOptions.getValue(mode).isEnabled

    interface Observer {
        fun onStateChanged(changedOption: DisplayMode)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun disable(option: DisplayMode) {
        renderingOptions.getValue(option).isEnabled = false
    }

    fun toggleState(option: DisplayMode) {
        val optionState = renderingOptions.getValue(option)
        optionState.isEnabled = !optionState.isEnabled

        if (option == DisplayMode.Boundary || option == DisplayMode.BoundaryOnly) {
            updateBoundaryMode()
        }

        notifyObservers(option)
    }

    private fun notifyObservers(option: DisplayMode) {
        observers.forEach { it.onStateChanged(option) }
    }

    private fun updateBoundaryMode() {
        val isBoundary = renderingOptions.getValue(DisplayMode.Boundary).isEnabled
        val isBoundaryOnly = renderingOptions.getValue(DisplayMode.BoundaryOnly).isEnabled

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