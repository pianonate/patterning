package ux;

import actions.MouseEventReceiver;
import patterning.Patterning;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.awt.*;
import java.util.OptionalInt;


public abstract class Panel implements Drawable, MouseEventReceiver {

    // alignment
    protected final boolean alignAble;
    protected final OptionalInt radius;
    protected Panel parentPanel;
    protected PGraphicsSupplier graphicsSupplier;
    // size & positioning
    protected PVector position;
    protected int width, height;
    protected int fill;
    protected AlignHorizontal hAlign;
    protected AlignVertical vAlign;
    // transition
    protected boolean transitionAble;
    protected Transition transition;
    // image buffers and callbacks
    protected PGraphics panelBuffer;
    boolean resized = false;
    // mouse stuff
    boolean isPressed = false;
    boolean isHovering = false;
    boolean isHoveringPrevious = false;
    private int lastUXWidth;
    private int lastUXHeight;
    private Transition.TransitionDirection transitionDirection;
    private Transition.TransitionType transitionType;
    private long transitionDuration;
    protected Panel(Builder<?> builder) {

        setPosition(builder.x, builder.y);
        this.graphicsSupplier = builder.graphicsSupplier;
        this.width = builder.width;
        this.height = builder.height;
        this.radius = builder.radius;
        this.hAlign = builder.alignHorizontal;
        this.vAlign = builder.alignVertical;
        this.alignAble = builder.alignable;
        this.fill = builder.fill;
        this.transitionDirection = builder.transitionDirection;
        this.transitionType = builder.transitionType;
        this.transitionDuration = builder.transitionDuration;
        this.transitionAble = (transitionDirection != null && transitionType != null);

        PGraphics parentBuffer = graphicsSupplier.get();
        panelBuffer = getPanelBuffer(parentBuffer);
        updateParentBufferSize(graphicsSupplier.get());

        if (transitionAble) {
            transition = new Transition(graphicsSupplier, transitionDirection, transitionType, transitionDuration);
        }
    }

    protected void setPosition(int x, int y) {
        if (position == null) {
            position = new PVector();
        }
        position.x = x;
        position.y = y;
    }

    protected PGraphics getPanelBuffer(PGraphics parentBuffer) {
        return parentBuffer.parent.createGraphics(this.width, this.height);
    }

    private void updateParentBufferSize(PGraphics parentBuffer) {
        lastUXWidth = parentBuffer.width;
        lastUXHeight = parentBuffer.height;
    }

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

    public void setFill(int fill) {
        this.fill = fill;
    }

    @SuppressWarnings("unused")
    public void setTransition(Transition.TransitionDirection direction, Transition.TransitionType type, long duration) {
        this.transitionDirection = direction;
        this.transitionType = type;
        this.transitionDuration = duration;
        this.transitionAble = true;
    }

    @Override
    //public void draw(PGraphics parentBuffer) {
    public void draw() {

        PGraphics parentBuffer = graphicsSupplier.get();

        parentBuffer.pushStyle();

        updateResized(parentBuffer);

        panelBuffer.beginDraw();
        panelBuffer.pushStyle();

        panelBuffer.fill(fill);
        // panelBuffer.fill(0xFFFF0000); // debugging ghost panel
        panelBuffer.noStroke();

        panelBuffer.clear();

        // handle alignment if requested
        if (alignAble) {
            updateAlignment(parentBuffer);
        }

        // output the background Rect for this panel
        if (radius.isPresent()) {
            panelBuffer.rect(0, 0, width, height, radius.getAsInt());
        } else {
            panelBuffer.rect(0, 0, width, height);
        }

        // subclass of Panels (such as a Control) can provide an implementation to be called at this point
        panelSubclassDraw();

        panelBuffer.endDraw();

        parentBuffer.popStyle();

        if (transitionAble) {
            transition.image(panelBuffer, position.x, position.y);
        } else {
            parentBuffer.image(panelBuffer, position.x, position.y);
        }
    }

    protected void updateResized(PGraphics parentBuffer) {
        if (parentBuffer.width != this.lastUXWidth || parentBuffer.height != this.lastUXHeight) {
            updateParentBufferSize(parentBuffer);
            this.resized = true;
        } else {
            this.resized = false;
        }
    }

    protected void updateAlignment(PGraphics buffer) {
        int posX = 0, posY = 0;
        switch (hAlign) {
            // case PApplet.LEFT -> posX = 0;
            case CENTER -> posX = (buffer.width - width) / 2;
            case RIGHT -> posX = buffer.width - width;
        }

        switch (vAlign) {
            // case PApplet.TOP -> posY = 0;
            case CENTER -> posY = (buffer.height - height) / 2;
            case BOTTOM -> posY = buffer.height - height;
        }

        setPosition(posX, posY);
    }

    protected abstract void panelSubclassDraw();

    private PVector getEffectivePosition() {
        // used in isMouseOverMe when a Panel contains other Panels
        // can walk up the hierarchy if you have nested panels
        if (parentPanel != null) {
            return new PVector(position.x + parentPanel.getEffectivePosition().x,
                    position.y + parentPanel.getEffectivePosition().y);
        } else {
            return position;
        }
    }

    protected boolean isMouseOverMe() {
        try {
            // the parent is a Panel, which has a PGraphics panelBuffer which has its PApplet
            PApplet processing = parentPanel.panelBuffer.parent;


            // our Patterning class extends Processing so we can use it here also
            Patterning patterning = (Patterning) processing;
            if (patterning.draggingDrawing) {
                return false;
            }

            Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            Point windowPosition = ((java.awt.Component) processing.getSurface().getNative()).getLocationOnScreen();

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

    public static abstract class Builder<T extends Builder<T>> {

        protected final PGraphicsSupplier graphicsSupplier;
        private int x;
        private int y;
        private int width;
        private int height;

        private boolean alignable;
        private AlignHorizontal alignHorizontal;
        private AlignVertical alignVertical;
        private int fill = UXThemeManager.getInstance().getDefaultPanelColor();
        private Transition.TransitionDirection transitionDirection;
        private Transition.TransitionType transitionType;
        private long transitionDuration;

        private OptionalInt radius = OptionalInt.empty();


        // used by Control
        public Builder(PGraphicsSupplier graphicsSupplier, int width, int height) {
            setRect(0, 0, width, height); // parent positioned
            this.graphicsSupplier = graphicsSupplier;
        }

        private void setRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        // used by TextPanel for explicitly positioned text
        public Builder(PGraphicsSupplier graphicsSupplier, PVector position, AlignHorizontal hAlign, AlignVertical vAlign) {
            setRect((int) position.x, (int) position.y, 0, 0); // parent positioned
            setAlignment(hAlign, vAlign, false);
            this.graphicsSupplier = graphicsSupplier;
        }

        private void setAlignment(AlignHorizontal alignHorizontal, AlignVertical vAlign, boolean alignAble) {
            this.alignable = alignAble;
            this.alignHorizontal = alignHorizontal;
            this.alignVertical = vAlign;
        }

        // used by BasicPanel for demonstration purposes
        public Builder(PGraphicsSupplier graphicsSupplier, AlignHorizontal alignHorizontal, AlignVertical alignVertical, int width, int height) {
            setRect(0, 0, width, height); // we're only using BasicPanel to show that panels are useful...
            setAlignment(alignHorizontal, alignVertical, true);
            this.graphicsSupplier = graphicsSupplier;

        }

        //  ContainerPanel(s) and TextPanel are often alignHorizontal / vAlign able
        public Builder(PGraphicsSupplier graphicsSupplier, AlignHorizontal alignHorizontal, AlignVertical alignVertical) {
            setRect(0, 0, 0, 0); // Containers and text, so far, only need to be aligned around the screen
            setAlignment(alignHorizontal, alignVertical, true);
            this.graphicsSupplier = graphicsSupplier;
        }

        public T fill(int fill) {
            this.fill = fill;
            return self();
        }

        // Method to allow subclass builders to return "this" correctly
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T transition(Transition.TransitionDirection direction, Transition.TransitionType type, long duration) {
            this.transitionDirection = direction;
            this.transitionType = type;
            this.transitionDuration = duration;
            return self();
        }

        public T radius(int radius) {
            this.radius = OptionalInt.of(radius);
            return self();
        }

        public abstract Panel build();
    }

}

