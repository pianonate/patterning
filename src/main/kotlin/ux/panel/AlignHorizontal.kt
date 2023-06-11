package ux.panel;
import processing.core.PApplet;

public enum AlignHorizontal {
    LEFT, CENTER, RIGHT;

    public int toPApplet() {
        return switch (this) {
            case LEFT -> PApplet.LEFT;
            case CENTER -> PApplet.CENTER;
            case RIGHT -> PApplet.RIGHT;
        };
    }

}
