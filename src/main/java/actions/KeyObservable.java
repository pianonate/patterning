package actions;

public interface KeyObservable {
    void addObserver(KeyObserver o);
    void notifyKeyObservers();

    boolean invokeModeChange();
}
