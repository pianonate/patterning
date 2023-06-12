package patterning.actions

interface KeyObservable {
    fun addObserver(observer: KeyObserver)
    fun notifyKeyObservers()
    fun invokeModeChange(): Boolean
}