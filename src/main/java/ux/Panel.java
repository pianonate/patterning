package ux;

import actions.MouseEventReceiver;
import patterning.Patterning;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class Panel implements Drawable, MouseEventReceiver {

    protected Panel parent;


    // size & positioning
    protected PVector position;
    protected int width, height;

    private int fill;

    // mouse stuff
    boolean isPressed = false;
    boolean isHovering = false;
    boolean isHoveringPrevious = false;

    @Override
    public void onMousePressed() {
        this.isPressed = isMouseOverMe();

        if (isPressed) {
            isHovering = false;
            isHoveringPrevious = true;
        }
    }

    @Override
    public void onMouseReleased() {
        if (isMouseOverMe()) {
            this.isPressed = false;
        }
    }


    @Override
    public boolean mousePressedOverMe() {
        return isMouseOverMe();
    }

    // child panels
    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    private final List<Panel> childPanels;
    private Orientation orientation;

    // alignment
    private boolean alignAble = false;
    private int hAlign;
    private int vAlign;

    // transition
    private boolean transitionAble = false;
    private Transition transition;
    private Transition.TransitionDirection transitionDirection;
    private Transition.TransitionType transitionType;
    private long transitionDuration;

    // image buffers and callbacks
    private PGraphics panelBuffer;
    private Consumer<PGraphics> subclassDraw;

    public Panel(int x, int y, int width, int height) {
        setPosition(x, y);
        this.width = width;
        this.height = height;

        // default to fully transparent black
        this.fill = UXTheme.getInstance().getDefaultPanelColor();

        this.transitionDirection = null;
        this.transitionType = null;
        this.transition = null;
        this.panelBuffer = null;

        this.childPanels = new ArrayList<>();
        this.orientation = Orientation.HORIZONTAL; // default value
    }

    public Panel(int hAlign, int vAlign) {
        this(0, 0, 0, 0);
        setAlignment(hAlign, vAlign);
    }

    private void setPosition(int x, int y) {
        position = new PVector(x, y);
    }

    protected PVector getPosition() {
        return position;
    }

    protected void setSubclassDraw(Consumer<PGraphics> subclassDraw) {
        this.subclassDraw = subclassDraw;
    }

    public void setAlignment(int hAlign, int vAlign) {
        this.hAlign = hAlign;
        this.vAlign = vAlign;
        this.alignAble = true;
    }

    public void setFill(int fill) {
        this.fill = fill;
    }

    public void setTransition(Transition.TransitionDirection direction, Transition.TransitionType type, long duration) {
        this.transitionDirection = direction;
        this.transitionType = type;
        this.transitionDuration = duration;
        this.transitionAble = true;
    }

    @Override
    public void draw(PGraphics buffer) {

        if (this.panelBuffer == null) {
            this.panelBuffer = buffer.parent.createGraphics(this.width, this.height);
            if (transitionAble) {
                transition = new Transition(this.panelBuffer, this.transitionDirection, this.transitionType, this.transitionDuration);
            }
        }

        this.panelBuffer.beginDraw();
        this.panelBuffer.pushStyle();

        this.panelBuffer.fill(fill);
        this.panelBuffer.noStroke();

        this.panelBuffer.clear();

        this.panelBuffer.rect(0, 0, this.width, this.height);

        // subclass of Panels (such as a Control) can provide an implementation to be called at this point
        if (null != subclassDraw) {
            subclassDraw.accept(panelBuffer);
        }

        // Draw child panels
        for (Panel child : childPanels) {

            child.draw(panelBuffer);
        }

        this.panelBuffer.popStyle();

        this.panelBuffer.endDraw();




        // handle alignment if requested
        if (alignAble) {
            updateAlignment(buffer);
        }

        if (transitionAble) {
            transition.transition(buffer, position.x, position.y);
        } else {
            buffer.image(this.panelBuffer, position.x, position.y);
        }
    }

    private void updateAlignment(PGraphics buffer) {
        int posX = 0, posY = 0;
        switch (hAlign) {
            // case PApplet.LEFT -> posX = 0;
            case PApplet.CENTER -> posX = (buffer.width - width) / 2;
            case PApplet.RIGHT -> posX = buffer.width - width;
        }

        switch (vAlign) {
            // case PApplet.TOP -> posY = 0;
            case PApplet.CENTER -> posY = (buffer.height - height) / 2;
            case PApplet.BOTTOM -> posY = buffer.height - height;
        }

        setPosition(posX, posY);
    }

    public void addChildPanel(Panel child) {
        this.childPanels.add(child);
        child.parent = this;
        updatePanelSizes();
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        updatePanelSizes();
    }

    public void updatePanelSizes() {
        int totalWidth = 0, totalHeight = 0;
        for (Panel child : childPanels) {
            if (this.orientation == Orientation.HORIZONTAL) {
                child.setPosition(totalWidth, 0);
                totalWidth += child.width;
                totalHeight = Math.max(totalHeight, child.height);
            } else { // Orientation.VERTICAL
                child.setPosition(0, totalHeight);
                totalHeight += child.height;
                totalWidth = Math.max(totalWidth, child.width);
            }
        }

        // Update parent size
        this.width = totalWidth;
        this.height = totalHeight;
    }

    private PVector getEffectivePosition() {
        if (parent != null) {
            return new PVector(position.x + parent.getEffectivePosition().x,
                    position.y + parent.getEffectivePosition().y);
        } else {
            return position;
        }
    }

    protected boolean isMouseOverMe() {
        try {
            PApplet processing = Patterning.getInstance();

            Patterning patterning = (Patterning) Patterning.getInstance();
            if (patterning.draggingDrawing) {
                return false;
            }

            Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            Point windowPosition = ((java.awt.Component) Patterning.getInstance().getSurface().getNative()).getLocationOnScreen();

            int mouseX = mousePosition.x - windowPosition.x;
            int mouseY = mousePosition.y - windowPosition.y;

            if (mouseX < 0 || mouseX > processing.width || mouseY < 0 || mouseY > processing.height) {
                return false;
            }

            PVector effectivePosition = getEffectivePosition();

            return mouseX >= effectivePosition.x && mouseX <= effectivePosition.x + width &&
                    mouseY >= effectivePosition.y && mouseY <= effectivePosition.y + height;

        } catch (Exception e) {
            return false;
        }
    }

}

