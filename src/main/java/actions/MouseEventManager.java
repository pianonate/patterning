package actions;

import ux.DrawRateManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MouseEventManager {
    private static MouseEventManager instance;

    private MouseEventManager() {}

    public static MouseEventManager getInstance() {
        if (instance == null) {
            instance = new MouseEventManager();
        }
        return instance;
    }

    private final List<MouseEventReceiver> mouseEventReceivers = new ArrayList<>();

    public void addReceiver(MouseEventReceiver receiver) {
        mouseEventReceivers.add(receiver);
    }

    public void addAll(Collection<? extends MouseEventReceiver> receivers) {
        mouseEventReceivers.addAll(receivers);
    }

    public void removeReceiver(MouseEventReceiver receiver) {
        mouseEventReceivers.remove(receiver);
    }

    private boolean mousePressedOverAny;
    private MouseEventReceiver pressedReceiver;

    public void onMousePressed(int mouseX, int mouseY) {
        mousePressedOverAny = false;
        pressedReceiver = null;

        for (MouseEventReceiver receiver : mouseEventReceivers) {
            if (!mousePressedOverAny && receiver.mousePressedOverMe()) {
                mousePressedOverAny = true;
                pressedReceiver = receiver;
            }

            receiver.onMousePressed();

        }
    }

    public void onMouseReleased() {
        if (pressedReceiver != null) {
            pressedReceiver.onMouseReleased();
            DrawRateManager.getInstance().drawImmediately();
            pressedReceiver = null;
        }
    }

    public boolean isMousePressedOverAnyReceiver() {
        return mousePressedOverAny;
    }

    public boolean isMouseDraggedOverReceiver() {
        return (pressedReceiver != null);
    }

}
