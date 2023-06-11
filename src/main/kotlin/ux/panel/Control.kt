package ux.panel;

import actions.*;
import org.jetbrains.annotations.NotNull;
import processing.core.PImage;
import processing.core.PVector;
import ux.DrawableManager;
import ux.UXThemeManager;
import ux.informer.DrawingInfoSupplier;

import java.util.Timer;
import java.util.TimerTask;


public class Control extends Panel implements KeyObserver, MouseEventReceiver {

    private static final UXThemeManager theme = UXThemeManager.getInstance();
    private final KeyCallback callback;
    private final int size;
    boolean isHighlightFromKeypress = false;
    protected PImage icon;
    private TextPanel hoverTextPanel;
    private final String hoverMessage;

    protected Control(Builder builder) {
        super(builder);

        this.callback = builder.callback;
        this.size = builder.size;

        setFill(theme.getControlColor());
        callback.addObserver(this);
        MouseEventManager.getInstance().addReceiver(this);


        this.icon = loadIcon(builder.iconName);

        String keyCombos = callback.toString();

        hoverMessage = callback.getUsageText() +theme.getShortcutParenStart() + keyCombos + theme.getShortcutParenEnd();

    }

    protected PImage loadIcon(String iconName) {
        PImage icon = panelBuffer.parent.loadImage(theme.getIconPath() + iconName);
        icon.resize(width - theme.getIconMargin(), height - theme.getIconMargin());
        return icon;
    }

    protected PImage getIcon() {
        return icon;
    }

    protected void panelSubclassDraw() {

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

        int hoverX = (int) parentPanel.position.x;
        int hoverY = (int) parentPanel.position.y;

        Transition.TransitionDirection transitionDirection = null;

        Orientation orientation = ((ControlPanel)parentPanel).orientation;

        switch (orientation) {
            case VERTICAL -> {
                switch(parentPanel.hAlign) {

                    case LEFT, CENTER -> {
                        hoverX += size + margin;
                        hoverY += position.y;
                        transitionDirection = Transition.TransitionDirection.RIGHT;
                    }
                    case RIGHT -> {
                        hoverX = hoverX - margin - hoverTextWidth;
                        hoverY += position.y;
                        transitionDirection = Transition.TransitionDirection.LEFT;
                    }
                }
            }
            case HORIZONTAL -> {
                hoverX += position.x;

                switch(parentPanel.vAlign) {
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
        }

        // the Control parentPanel is a ContainerPanel that has a DrawingInfoSupplier
        // which has a PGraphicsSupplier of the current UXBuffer
        // we can't use the parent Control PGraphicsSupplier as it is provided by the ContainerPanel so that the
        // Control draws itself within the ContainerPanel
        //
        // instead we pass the hover text the parent ContainerPanel's DrawingInfoSupplier which comes from
        // PatternDrawer, i.e., and has a PGraphicsSupplier of the UXBuffer itself - otherwise the hover text
        // would try to draw itself within the control at a microscopic size
        TextPanel hoverText = new TextPanel.Builder(parentPanel.drawingInformer, hoverMessage, new PVector(hoverX, hoverY), AlignHorizontal.LEFT, AlignVertical.TOP)
                .fill(theme.getControlHighlightColor())
                .radius(theme.getControlHighlightCornerRadius())
                .textSize(theme.getHoverTextSize())
                .textWidth(hoverTextWidth)
                .wrap()
                .keepShortCutTogether() // keeps the last two words on the same line when text wrapping
                .transition(transitionDirection, Transition.TransitionType.SLIDE,  theme.getShortTransitionDuration())
                .outline(false)
                .build();

        // hover text is word wrapped and sized to fit
        // we pass in the max and set up the position to display
        // for RIGHT aligned VERTICAL control panels, we need to change the x position to make it appear
        // next to the control.  Not a problem for TOP, LEFT, BOTTOM controls
        // we could put this logic into TextPanel so that it adjusts
        // its own x position based on the alignment of this control but that would clutter TextPanel
        //
        // maybe a generic capability of aligning controls to each other
        // could be added in the future if it becomes a common need -for now, we just do it here
        if (orientation==Orientation.VERTICAL && parentPanel.hAlign == AlignHorizontal.RIGHT) {
            hoverText.position.x += (hoverTextWidth - hoverText.width);
        }

        // similar treatment for HORIZONTAL aligned BOTTOM control panels
        if (orientation==Orientation.HORIZONTAL && parentPanel.vAlign==AlignVertical.BOTTOM) {
            hoverText.position.y -= (hoverText.height);
        }

        // if the text won't display, make it possible to display
        int screenWidth = parentPanel.drawingInformer.getPGraphics().width;
        if (hoverText.position.x + hoverText.width > screenWidth) {
            hoverText.position.x = screenWidth - hoverText.width;
        }

        return hoverText;
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
    public void notifyKeyPress(@NotNull KeyObservable observer) {
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
