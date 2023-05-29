package ux;

import actions.MouseEventReceiver;
import patterning.Patterning;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.awt.*;


public abstract class Panel implements Drawable, MouseEventReceiver {

    public enum HAlign {
        LEFT, CENTER, RIGHT;

        public int toPApplet() {
            return switch (this) {
                case LEFT -> PApplet.LEFT;
                case CENTER -> PApplet.CENTER;
                case RIGHT -> PApplet.RIGHT;
            };
        }
    }

    public enum VAlign {
        TOP, CENTER, BOTTOM;

        public int toPApplet() {
            return switch (this) {
                case TOP -> PApplet.TOP;
                case CENTER -> PApplet.CENTER;
                case BOTTOM -> PApplet.BOTTOM;
            };
        }
    }

    protected Panel parentPanel;


    // size & positioning
    protected PVector position;
    protected int width, height;

    private int fill;

    boolean resized = false;
    private int lastUXWidth;
    private int lastUXHeight;

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

    // alignment
    private boolean alignAble;
    protected HAlign hAlign;
    protected VAlign vAlign;

    // transition
    private boolean transitionAble;
    private Transition transition;
    private Transition.TransitionDirection transitionDirection;
    private Transition.TransitionType transitionType;
    private long transitionDuration;

    // image buffers and callbacks
    protected PGraphics panelBuffer;

   protected Panel(Builder<?> builder) {

       setPosition(builder.x,builder.y);
       this.width = builder.width;
       this.height = builder.height;
       this.hAlign = builder.hAlign;
       this.vAlign = builder.vAlign;
       this.alignAble = (hAlign != null && vAlign != null);
       this.fill = builder.fill;
       this.transitionDirection = builder.transitionDirection;
       this.transitionType = builder.transitionType;
       this.transitionDuration = builder.transitionDuration;
       this.transitionAble = (transitionDirection != null && transitionType != null);
   }

    protected void setPosition(int x, int y) {
        if (position==null){
            position = new PVector();
        }
        position.x = x;
        position.y = y;
    }

/*    public void setAlignment(HAlign hAlign, VAlign vAlign) {
        this.hAlign = hAlign;
        this.vAlign = vAlign;
        this.alignAble = true;
    }*/

    public void setFill(int fill) {
        this.fill = fill;
    }

    public void setTransition(Transition.TransitionDirection direction, Transition.TransitionType type, long duration) {
        this.transitionDirection = direction;
        this.transitionType = type;
        this.transitionDuration = duration;
        this.transitionAble = true;
    }

    protected PGraphics getPanelBuffer(PGraphics parentBuffer) {
        return parentBuffer.parent.createGraphics(this.width, this.height);
    }

    protected boolean shouldGetPanelBuffer(PGraphics parentBuffer) {
        return (this.panelBuffer == null);
    }

    private void updateResized(PGraphics parentBuffer) {
        if (parentBuffer.width != this.lastUXWidth || parentBuffer.height != this.lastUXHeight) {
            this.lastUXWidth = parentBuffer.width;
            this.lastUXHeight = parentBuffer.height;
            this.resized = true;
        } else {
            this.resized = false;
        }
    }

    @Override
    public void draw(PGraphics parentBuffer) {

        parentBuffer.pushStyle();

        if (shouldGetPanelBuffer(parentBuffer)) {
            panelBuffer = getPanelBuffer(parentBuffer);
            lastUXWidth = parentBuffer.width;
            lastUXHeight = parentBuffer.height;
            if (transitionAble) {
                transition = new Transition(panelBuffer, transitionDirection, transitionType, transitionDuration);
            }
        }

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
        panelBuffer.rect(0, 0, width, height);

        // subclass of Panels (such as a Control) can provide an implementation to be called at this point
        panelSubclassDraw();

        panelBuffer.popStyle();

        panelBuffer.endDraw();

        if (transitionAble) {
            transition.transition(parentBuffer, position.x, position.y);

        } else {
            parentBuffer.image(panelBuffer, position.x, position.y);
        }

        parentBuffer.popStyle();
    }

    protected void panelSubclassDraw() {

    }

    private void updateAlignment(PGraphics buffer) {
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

    private PVector getEffectivePosition() {
        // used when a Panel contains other Panels
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
            Patterning patterning = (Patterning) processing ;
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
        private int x;
        private int y;
        private int width;
        private int height;
        private HAlign hAlign;
        private VAlign vAlign;
        private int fill = UXThemeManager.getInstance().getDefaultPanelColor();
        private Transition.TransitionDirection transitionDirection;
        private Transition.TransitionType transitionType;
        private long transitionDuration;

        // Constructor for explicitly positioned Panel
        public Builder(int x, int y, int width, int height) {
            setPosition(x, y);
            setWidth(width);
            setHeight(height);
        }

        // Constructor for aligned Panel with given width and height
        public Builder(HAlign hAlign, VAlign vAlign, int width, int height) {
            setPosition(0, 0);
            setAlignment(hAlign, vAlign);
            setWidth(width);
            setHeight(height);
        }

        // Constructor for aligned Panel with default dimensions (0, 0)
        public Builder(HAlign hAlign, VAlign vAlign) {
            setPosition(0, 0);
            setAlignment(hAlign, vAlign);
            setWidth(0);
            setHeight(0);
        }

        private T setPosition(int x, int y) {
            this.x = x;
            this.y = y;
            return self();
        }

        private T setWidth(int width) {
            this.width = width;
            return self();
        }

        private T setHeight(int height) {
            this.height = height;
            return self();
        }

        private T setAlignment(HAlign hAlign, VAlign vAlign) {
            this.hAlign = hAlign;
            this.vAlign = vAlign;
            return self();
        }

        public T setFill(int fill) {
            this.fill = fill;
            return self();
        }

        public T setTransition(Transition.TransitionDirection direction, Transition.TransitionType type, long duration) {
            this.transitionDirection = direction;
            this.transitionType = type;
            this.transitionDuration = duration;
            return self();
        }

        // Method to allow subclass builders to return "this" correctly
        protected T self() {
            return (T) this;
        }

        public abstract Panel build();
    }

}

