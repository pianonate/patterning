package ux;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public class DrawableManager {
    private static DrawableManager instance;
    private final List<Drawable> drawables;
    private final List<Drawable> toBeAdded;

    private final List<Drawable> toBeRemoved;

    private DrawableManager() {
        drawables = new ArrayList<>();
        toBeAdded = new ArrayList<>();
        toBeRemoved = new ArrayList<>();
    }

    public static DrawableManager getInstance() {
        if (instance == null) {
            instance = new DrawableManager();
        }
        return instance;
    }

    public void add(Drawable drawable) {
        toBeAdded.add(drawable);
    }
    public void addAll(List<? extends Drawable> drawables) {
        this.toBeAdded.addAll(drawables);
    }


    public void drawAll(PGraphics buffer) {
        for (Drawable drawable : drawables) {
            // drawable.draw(buffer);
            drawable.draw();
        }
        // Add all drawables that need to be added
        drawables.addAll(toBeAdded);
        toBeAdded.clear();
        // clean up drawables that need to be removed
        drawables.removeAll(toBeRemoved);
        toBeRemoved.clear();
    }

    public boolean isManaging(Drawable drawable) {
        return drawables.contains(drawable);
    }

    public void remove(Drawable drawable) {
        toBeRemoved.add(drawable);
    }
}
