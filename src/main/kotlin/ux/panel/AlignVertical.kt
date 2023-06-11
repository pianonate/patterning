package ux.panel;

import processing.core.PApplet;

public enum AlignVertical {
    TOP, CENTER, BOTTOM;

    public int toPApplet() {
        return switch (this) {
            case TOP -> PApplet.TOP;
            case CENTER -> PApplet.CENTER;
            case BOTTOM -> PApplet.BOTTOM;
        };
    }
}
