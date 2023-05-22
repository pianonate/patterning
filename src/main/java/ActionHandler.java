public interface ActionHandler {
    void onInvoke();
    void onRelease();
    String getUsageText();
}
