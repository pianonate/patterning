package ux;

import processing.core.PApplet;
import processing.core.PGraphics;

//todo: TRANSITION_DURATION is a UXConstant
public class Transition {

    private final long duration;
    private final PGraphics constructedBuffer;
    private final TransitionDirection direction;
    private final TransitionType type;
    private long transitionStartTime = -1;

    public Transition(PGraphics buffer, TransitionDirection direction, TransitionType type) {
        this(buffer, direction, type, UXTheme.getInstance().getShortTransitionDuration());
    }

    public Transition(PGraphics buffer, TransitionDirection direction, TransitionType type, long duration) {
        this.constructedBuffer = buffer;
        this.direction = direction;
        this.type = type;
        this.duration = duration;
    }


    public void reset() {
        transitionStartTime = -1;
    }

    public void transition(PGraphics buffer, float x, float y) {
        if (transitionStartTime == -1) {
            transitionStartTime = System.currentTimeMillis();
        }

        long elapsed = System.currentTimeMillis() - transitionStartTime;
        float transitionProgress = PApplet.constrain((float) elapsed / duration, 0, 1);

        switch (type) {
            case EXPANDO -> drawExpandoTransition(buffer, transitionProgress, x, y);
            case SLIDE -> drawSlideTransition(buffer, transitionProgress, x, y);
            case DIAGONAL -> drawDiagonalTransition(buffer, transitionProgress, x, y);
        }
    }

    private void drawExpandoTransition(PGraphics buffer, float animationProgress, float x, float y) {
        switch (direction) {
            case LEFT ->
                    buffer.image(this.constructedBuffer, x - this.constructedBuffer.width * animationProgress, y, (int) (this.constructedBuffer.width * animationProgress), this.constructedBuffer.height);
            case RIGHT ->
                    buffer.image(this.constructedBuffer, x, y, (int) (this.constructedBuffer.width * animationProgress), this.constructedBuffer.height);
            case UP ->
                    buffer.image(this.constructedBuffer, x, y - this.constructedBuffer.height * animationProgress, this.constructedBuffer.width, (int) (this.constructedBuffer.height * animationProgress));
            case DOWN ->
                    buffer.image(this.constructedBuffer, x, y, this.constructedBuffer.width, (int) (this.constructedBuffer.height * animationProgress));
        }
    }

    private void drawSlideTransition(PGraphics buffer, float animationProgress, float x, float y) {

        switch (direction) {
            case LEFT ->
                    buffer.image(this.constructedBuffer.get(0, 0, (int) (this.constructedBuffer.width * animationProgress), this.constructedBuffer.height), x - this.constructedBuffer.width * animationProgress, y);
            case RIGHT ->
                    buffer.image(this.constructedBuffer.get((int) (this.constructedBuffer.width * (1 - animationProgress)), 0, (int) (this.constructedBuffer.width * animationProgress), this.constructedBuffer.height), x, y);
            case UP ->
                    buffer.image(this.constructedBuffer.get(0, 0, this.constructedBuffer.width, (int) (this.constructedBuffer.height * animationProgress)), x, y - this.constructedBuffer.height * animationProgress);
            case DOWN ->
                    buffer.image(this.constructedBuffer.get(0, (int) (this.constructedBuffer.height * (1 - animationProgress)), this.constructedBuffer.width, (int) (this.constructedBuffer.height * animationProgress)), x, y);
        }
    }

    private void drawDiagonalTransition(PGraphics buffer, float animationProgress, float x, float y) {
        switch (direction) {
            case LEFT ->
                    buffer.image(this.constructedBuffer.get(0, (int) (this.constructedBuffer.height * (1 - animationProgress)), (int) (this.constructedBuffer.width * animationProgress), (int) (this.constructedBuffer.height * animationProgress)), x - this.constructedBuffer.width * animationProgress, y);
            case RIGHT ->
                    buffer.image(this.constructedBuffer.get((int) (this.constructedBuffer.width - this.constructedBuffer.width * animationProgress), (int) (this.constructedBuffer.height - this.constructedBuffer.height * animationProgress), (int) (this.constructedBuffer.width * animationProgress), (int) (this.constructedBuffer.height * animationProgress)), x, y);
            case UP ->
                    buffer.image(this.constructedBuffer.get(0, 0, (int) (this.constructedBuffer.width * animationProgress), (int) (this.constructedBuffer.height * animationProgress)), x, y - this.constructedBuffer.height * animationProgress);
            case DOWN ->
                    buffer.image(this.constructedBuffer.get(0, (int) (this.constructedBuffer.height * (1 - animationProgress)), (int) (this.constructedBuffer.width * animationProgress), (int) (this.constructedBuffer.height * animationProgress)), x, y);
        }
    }

    public enum TransitionDirection {
        LEFT, RIGHT, UP, DOWN
    }


    public enum TransitionType {
        EXPANDO, SLIDE, DIAGONAL
    }


}

