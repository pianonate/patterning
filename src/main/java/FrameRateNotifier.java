import java.util.List;
import java.util.ArrayList;

public class FrameRateNotifier {

    private final List<FrameRateListener> listeners;

    public FrameRateNotifier() {
        listeners = new ArrayList<>();
    }

    public void addListener(FrameRateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FrameRateListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(float frameRate) {
        for (FrameRateListener listener : listeners) {
            listener.onFrameRateUpdate(frameRate);
        }
    }
}

