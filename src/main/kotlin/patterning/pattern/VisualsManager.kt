package patterning.pattern

class VisualsManager {

    private val observers: MutableList<Observer> = mutableListOf()

    private data class OptionState(val option: Visual, var isEnabled: Boolean)

    private val renderingOptions: Map<Visual, OptionState> =
        Visual.entries.associateWith { OptionState(it, false) }

    var boundaryMode: BoundaryMode = BoundaryMode.PatternOnly
        private set

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

        if (option == Visual.Boundary || option == Visual.BoundaryOnly) {
            updateBoundaryMode()
        }

        notifyObservers(option)
    }

    private fun notifyObservers(option: Visual) {
        observers.forEach { it.onStateChanged(option) }
    }

    private fun updateBoundaryMode() {
        val isBoundary = renderingOptions.getValue(Visual.Boundary).isEnabled
        val isBoundaryOnly = renderingOptions.getValue(Visual.BoundaryOnly).isEnabled

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