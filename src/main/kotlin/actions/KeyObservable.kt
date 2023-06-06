package actions

interface KeyObservable {
    fun addObserver(o: KeyObserver?)
    fun notifyKeyObservers()
    fun invokeModeChange(): Boolean
}
