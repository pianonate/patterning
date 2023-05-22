public interface KeyObservable {
    void addObserver(KeyObserver o);
    void deleteObserver(KeyObserver o);
    void notifyKeyObservers();
}
