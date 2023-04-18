import processing.core.PApplet;

public class GameOfLife extends PApplet {

    public static void main(String[] args) {
        GameOfLife sketch = new GameOfLife();
        PApplet.runSketch(new String[]{sketch.getClass().getSimpleName()}, sketch);
    }

    public void settings() {
        size(800, 600);
    }

    public void setup() {
        background(255);
    }

    public void draw() {
        ellipse(mouseX, mouseY, 50, 50);
    }

}
