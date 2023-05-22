public interface MouseEventReceiver {
    void onMousePressed();
    void onMouseReleased();

    boolean mousePressedOverMe();
}