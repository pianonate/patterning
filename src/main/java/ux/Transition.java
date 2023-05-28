package ux;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class Transition {

    private final long duration;
    private final PGraphics panelBuffer;
    private final TransitionDirection direction;
    private final TransitionType type;
    private long transitionStartTime = -1;

    public Transition(PGraphics buffer, TransitionDirection direction, TransitionType type) {
        this(buffer, direction, type, UXThemeManager.getInstance().getShortTransitionDuration());
    }

    public Transition(PGraphics panelBuffer, TransitionDirection direction, TransitionType type, long duration) {
        this.panelBuffer = panelBuffer;
        this.direction = direction;
        this.type = type;
        this.duration = duration;
    }


    public void reset() {
        transitionStartTime = -1;
    }

    public void transition(PGraphics UXBuffer, float x, float y) {
        if (transitionStartTime == -1) {
            transitionStartTime = System.currentTimeMillis();
        }

        long elapsed = System.currentTimeMillis() - transitionStartTime;
        float transitionProgress = PApplet.constrain((float) elapsed / duration, 0, 1);

        switch (type) {
            case EXPANDO -> drawExpandoTransition(UXBuffer, transitionProgress, x, y);
            case SLIDE -> drawSlideTransition(UXBuffer, transitionProgress, x, y);
            case DIAGONAL -> drawDiagonalTransition(UXBuffer, transitionProgress, x, y);
        }
    }

    private void drawExpandoTransition(PGraphics UXBuffer, float animationProgress, float x, float y) {
        switch (direction) {
            case LEFT -> {
                int visibleWidth = (int) (this.panelBuffer.width * animationProgress);
                int revealPointX = (int) (x + this.panelBuffer.width - visibleWidth);

                UXBuffer.image(this.panelBuffer, revealPointX, y, visibleWidth, this.panelBuffer.height);
            }
            case RIGHT ->
                    UXBuffer.image(this.panelBuffer, x, y, (int) (this.panelBuffer.width * animationProgress), this.panelBuffer.height);
            case UP -> {
                int visibleHeight = (int) (this.panelBuffer.height * animationProgress);
                int revealPointY = (int) (y + this.panelBuffer.height - visibleHeight);

                UXBuffer.image(this.panelBuffer, x, revealPointY, this.panelBuffer.width, visibleHeight);
            }case DOWN ->
                    UXBuffer.image(this.panelBuffer, x, y, this.panelBuffer.width, (int) (this.panelBuffer.height * animationProgress));
        }
    }

    private void drawSlideTransition(PGraphics UXBuffer, float animationProgress, float x, float y) {

        switch (direction) {
            case LEFT -> {
                int visibleWidth = (int) (this.panelBuffer.width * animationProgress);
                int revealPointX = (int) (x + (this.panelBuffer.width - visibleWidth));

                PImage visiblePart = this.panelBuffer.get(0, 0, visibleWidth, this.panelBuffer.height);
                UXBuffer.image(visiblePart, revealPointX, y);
            }
            case RIGHT -> {
                int visibleWidth = (int) (this.panelBuffer.width * animationProgress);
                int revealPointX = (int) (x);

                PImage visiblePart = this.panelBuffer.get(this.panelBuffer.width - visibleWidth, 0, visibleWidth, this.panelBuffer.height);
                UXBuffer.image(visiblePart, revealPointX, y);
            }
            case UP -> {
                int visibleHeight = (int) (this.panelBuffer.height * animationProgress);
                int revealPointY = (int) (y + (this.panelBuffer.height - visibleHeight));

                PImage visiblePart = this.panelBuffer.get(0, 0, this.panelBuffer.width, visibleHeight);
                UXBuffer.image(visiblePart, x, revealPointY);
            }
            case DOWN -> {
                int visibleHeight = (int) (this.panelBuffer.height * animationProgress);
                int revealPointY = (int) (y);

                PImage visiblePart = this.panelBuffer.get(0, this.panelBuffer.height - visibleHeight, this.panelBuffer.width, visibleHeight);
                UXBuffer.image(visiblePart, x, revealPointY);
            }


        }
    }


    private void drawDiagonalTransition(PGraphics UXBuffer, float animationProgress, float x, float y) {
        switch (direction) {
            case LEFT -> {
                int visibleWidth = (int) (this.panelBuffer.width * animationProgress);
                int revealPointX = (int) (x + (this.panelBuffer.width - visibleWidth));

                UXBuffer.image(this.panelBuffer.get(this.panelBuffer.width - visibleWidth, (int) (this.panelBuffer.height * (1 - animationProgress)), visibleWidth, (int) (this.panelBuffer.height * animationProgress)), revealPointX, y);
            }
            case RIGHT ->
                    UXBuffer.image(this.panelBuffer.get((int) (this.panelBuffer.width - this.panelBuffer.width * animationProgress), (int) (this.panelBuffer.height - this.panelBuffer.height * animationProgress), (int) (this.panelBuffer.width * animationProgress), (int) (this.panelBuffer.height * animationProgress)), x, y);
            case UP ->
                UXBuffer.image(this.panelBuffer.get(0, 0, (int) (this.panelBuffer.width * animationProgress), (int) (this.panelBuffer.height * animationProgress)), x + (this.panelBuffer.width * (1 - animationProgress)), y + (this.panelBuffer.height * (1 - animationProgress)));
            case DOWN ->
                    UXBuffer.image(this.panelBuffer.get(0, (int) (this.panelBuffer.height * (1 - animationProgress)), (int) (this.panelBuffer.width * animationProgress), (int) (this.panelBuffer.height * animationProgress)), x, y);
        }
    }

    public enum TransitionDirection {
        LEFT, RIGHT, UP, DOWN
    }

    public enum TransitionType {
        EXPANDO, SLIDE, DIAGONAL
    }


}

