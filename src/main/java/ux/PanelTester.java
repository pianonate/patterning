package ux;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PanelTester implements Drawable {
    private final Map<String, Panel> alignPanels;
    private final PApplet processing;

    public PanelTester(PApplet processing) {
        alignPanels = new HashMap<>();
        this.processing = processing;

        createAlignPanel(PApplet.LEFT, PApplet.TOP, 0xAA0000FF, Transition.TransitionDirection.RIGHT);
        createAlignPanel(PApplet.CENTER, PApplet.TOP, 0xAA0000FF, Transition.TransitionDirection.DOWN);
        createAlignPanel(PApplet.RIGHT, PApplet.TOP, 0xAA0000FF, Transition.TransitionDirection.LEFT);
        createAlignPanel(PApplet.LEFT, PApplet.CENTER, 0xAAFF0000, Transition.TransitionDirection.DOWN);
        createAlignPanel(PApplet.CENTER, PApplet.CENTER, 0xAAFF0000, Transition.TransitionDirection.LEFT);
        createAlignPanel(PApplet.RIGHT, PApplet.CENTER, 0xAAFF0000, Transition.TransitionDirection.DOWN);
        createAlignPanel(PApplet.LEFT, PApplet.BOTTOM, 0xAA00FF00,Transition.TransitionDirection.RIGHT);
        createAlignPanel(PApplet.CENTER, PApplet.BOTTOM, 0xAA00FF00, Transition.TransitionDirection.RIGHT);
        createAlignPanel(PApplet.RIGHT, PApplet.BOTTOM, 0xAA00FF00, Transition.TransitionDirection.LEFT);

    }

    private void createAlignPanel(int hAlign, int vAlign, int color, Transition.TransitionDirection direction) {
        Random randLeft = new Random();
        int left = randLeft.nextInt(processing.width);

        Random randTop = new Random();
        int top = randTop.nextInt(processing.height);

        String key = hAlign + "_" + vAlign + "_" + color;

        UXTheme theme = UXTheme.getInstance();

        Panel panel = new Panel(0,0, 150, 250);
        panel.setFill(color);
        panel.setAlignment(hAlign, vAlign);
        panel.setTransition(direction, Transition.TransitionType.SLIDE, theme.getLongTransitionDuration());

        alignPanels.put(key, panel);
    }

    @Override
    public void draw(PGraphics buffer) {
        for (Panel panel : alignPanels.values()) {
            panel.draw(buffer);
        }
    }
}
