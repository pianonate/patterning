package ux.panel;

import actions.*;
import processing.core.PImage;
import processing.core.PVector;
import ux.DrawableManager;
import ux.UXThemeManager;
import ux.informer.DrawingInfoSupplier;

import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;


public class Control extends Panel implements KeyObserver, MouseEventReceiver {

    private static final UXThemeManager theme = UXThemeManager.getInstance();
    private final KeyCallback callback;
    private final int size;
    boolean isHighlightFromKeypress = false;
    protected PImage icon;
    private final String iconName;
    private TextPanel hoverTextPanel;
    private String hoverMessage;

    private int hoverTextWidth = UXThemeManager.getInstance().getHoverTextSize();

    protected Control(Builder builder) {
        super(builder);

        this.callback = builder.callback;
        this.size = builder.size;

        setFill(theme.getControlColor());
        callback.addObserver(this);
        MouseEventManager.getInstance().addReceiver(this);
        this.iconName = builder.iconName;

        this.icon = loadIcon(iconName); // panelBuffer.parent.loadImage(theme.getIconPath() + iconName);

        String keyCombos = callback.getValidKeyCombosForCurrentOS().stream()
                .map(KeyCombo::toString)
                .collect(Collectors.joining(", "));

        hoverMessage = callback.getUsageText() +theme.getShortcutParenStart() + keyCombos + theme.getShortcutParenEnd();

    }

    // at time of construction, the control doesn't have information about its parent
    // so instantiate the hover text on first use
    private void firstDrawSetup() {
        if (icon != null) {
            return;
        }

        PImage icon = loadIcon(iconName);
        icon.resize(width - 5, height - 5);
        this.icon = icon;

        String keyCombos = callback.getValidKeyCombosForCurrentOS().stream()
                .map(KeyCombo::toString)
                .collect(Collectors.joining(", "));

        hoverMessage = callback.getUsageText() + " (shortcut: " + keyCombos + ")";

    }

    protected PImage loadIcon(String iconName) {
        PImage icon = panelBuffer.parent.loadImage(theme.getIconPath() + iconName);
        icon.resize(width - 5, height - 5);
        return icon;
    }

    protected PImage getIcon() {
        return icon;
    }

    protected void panelSubclassDraw() {

        //firstDrawSetup();

        mouseHover(isMouseOverMe());
        drawHover();
        drawPressed();
        drawIcon();
    }

    private void drawHover() {

        if (isHovering) {
            drawControlHighlight(theme.getControlHighlightColor());
            if (null==hoverTextPanel) {
                hoverTextPanel = getHoverTextPanel();
                DrawableManager.getInstance().add(hoverTextPanel);
            }
        } else {
            if (null!=hoverTextPanel) {
                DrawableManager.getInstance().remove(hoverTextPanel);
                hoverTextPanel = null;
            }
        }
    }

    private TextPanel getHoverTextPanel() {
        int margin = theme.getHoverTextMargin();
        int hoverTextWidth = theme.getHoverTextWidth();

        int hoverX = 0, hoverY = 0;
        Transition.TransitionDirection transitionDirection = null;

        switch (parentPanel.hAlign) {
            case LEFT -> {
                hoverX = (int) parentPanel.position.x + size + margin;
                hoverY = (int) (parentPanel.position.y + position.y);
                transitionDirection = Transition.TransitionDirection.RIGHT;
            }
            case CENTER -> {
                hoverX = (int) (parentPanel.position.x + position.x);
                switch (parentPanel.vAlign) {
                    case TOP, CENTER -> {
                        hoverY = (int) parentPanel.position.y + size + margin;
                        transitionDirection = Transition.TransitionDirection.DOWN;
                    }
                    case BOTTOM -> {
                        hoverY = (int) parentPanel.position.y - margin;
                        transitionDirection = Transition.TransitionDirection.UP;
                    }
                }
            }
            case RIGHT -> {
                hoverX = (int) parentPanel.position.x - margin - hoverTextWidth;
                hoverY = (int) (parentPanel.position.y + position.y);
                transitionDirection = Transition.TransitionDirection.LEFT;
            }
        }

        // the Control parentPanel is a ContainerPanel that has a DrawingInfoSupplier
        // which has a PGraphicsSupplier of the current UXBuffer
        // we can't use the parent Control PGraphicsSupplier as it is provided by the ContainerPanel so that the
        // Control draws itself within the ContainerPanel
        //
        // instead we pass the hover text the parent ContainerPanel's DrawingInfoSupplier which comes from
        // PatternDrawer, i.e., and has a PGraphicsSupplier of the UXBuffer itself - otherwise the hover text
        // would try to draw itself within the control at a microscopic size
        return new TextPanel.Builder(parentPanel.drawingInformer, hoverMessage, new PVector(hoverX, hoverY), AlignHorizontal.LEFT, AlignVertical.TOP)
                .fill(theme.getControlHighlightColor())
                .textSize(theme.getHoverTextSize())
                .textWidth(hoverTextWidth)
                .wrap()
                .keepShortCutTogether()
                .transition(transitionDirection, Transition.TransitionType.SLIDE,  theme.getShortTransitionDuration())
                .radius(theme.getControlHighlightCornerRadius())
                .outline(false)
                .build();
    }


    void mouseHover(boolean isHovering) {

        if (isPressed && !isHovering) {
            // If pressed and not hovering, reset the pressed state
            this.isPressed = false;
        } else if (isHovering != isHoveringPrevious) {
            // Only update isHovering if there is a change in hover state
            this.isHovering = isHovering;
            isHoveringPrevious = isHovering;
        }
    }

    private void drawPressed() {
        if (isPressed || isHighlightFromKeypress) {
            drawControlHighlight(theme.getControlMousePressedColor());
        }
    }

    private void drawIcon() {
        PImage thisIcon = getIcon();

        float x = (float) (width - thisIcon.width) / 2;
        float y = (float) (height - thisIcon.height) / 2;
        panelBuffer.image(thisIcon, x, y);
    }

    private void drawControlHighlight(int color) {
        // highlight the control with a semi-transparent rect
        panelBuffer.fill(color); // Semi-transparent gray
        float roundedRectSize = size;
        // Rounded rectangle with radius
        panelBuffer.rect(0,0, roundedRectSize, roundedRectSize, theme.getControlHighlightCornerRadius());
    }

    @Override
    public void notifyKeyPress(KeyObservable o) {
        highlightFromKeyPress();
    }

    private void highlightFromKeyPress() {
        isHighlightFromKeypress = true;

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        isHighlightFromKeypress = false;
                    }
                },
                theme.getControlHighlightDuration()
        );
    }

    @Override
    public void onMousePressed() {
        super.onMousePressed();
    }

    @Override
    public void onMouseReleased() {
        super.onMouseReleased(); // Calls Panel's onMouseReleased
        if (isMouseOverMe()) {
            callback.invokeFeature();  // Specific to Control
        }
    }
    public static class Builder extends Panel.Builder<Builder> {
        private final KeyCallback callback;
        private final String iconName;
        private final int size;

        public Builder(DrawingInfoSupplier drawingInformer, KeyCallback callback, String iconName, int size) {
            super(drawingInformer, size, size);
            this.callback = callback;
            this.iconName = iconName;
            this.size = size;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Control build() {
            return new Control(this);
        }
    }

}
