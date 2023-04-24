import processing.core.PApplet;

import java.math.BigInteger;

class LifeDrawer {
    PApplet p;
    int canvas_offset_x = 0;
    int canvas_offset_y = 0;
    int canvas_width;
    int canvas_height;
    int border_width;
    int cell_color = 0;
    int background_color = 255;
    float cell_width;
    float border_width_ratio = 0;



    LifeDrawer(PApplet p, float cell_width) {
        this.p = p;
        this.cell_width = cell_width;
        this.canvas_width = p.width;
        this.canvas_height = p.height;
    }


    void draw_node(Node node, float size, float left, float top) {
        if (node.population.equals(BigInteger.ZERO)) {
            return;
        }

        if (left + size + canvas_offset_x < 0 || top + size + canvas_offset_y < 0 || left + canvas_offset_x >= canvas_width || top + canvas_offset_y >= canvas_height) {
            return;
        }

        if (size <= 1) {
            if ((node.population.compareTo(BigInteger.ZERO) > 0)) {
                fill_square(left + canvas_offset_x, top + canvas_offset_y, 1);
            }
        } else if (node.level == 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                fill_square(left + canvas_offset_x, top + canvas_offset_y, cell_width);
            }
        } else {
            size /= 2;
            draw_node(node.nw, size, left, top);
            draw_node(node.ne, size, left + size, top);
            draw_node(node.sw, size, left, top + size);
            draw_node(node.se, size, left + size, top + size);
        }
    }

    void fill_square(float x, float y, float size) {
        float width = size - border_width;
        float height = width;

        p.noStroke();
        p.fill(cell_color);
        p.rect(x, y, width, height);
    }

    void setSize(int width, int height) {
        if (width != canvas_width || height != canvas_height) {
            canvas_width = width;
            canvas_height = height;
        }
    }

    void redraw(Node node) {
        border_width = (int) (border_width_ratio * cell_width);
        float size = (float) Math.pow(2, node.level - 1) * cell_width;
        draw_node(node, 2 * size, -size, -size);
    }

    void move(float dx, float dy) {
        canvas_offset_x += Math.round(dx);
        canvas_offset_y += Math.round(dy);
    }

    void zoom(boolean out, double center_x, double center_y) {
        if (out) {
            canvas_offset_x -= (canvas_offset_x - center_x) / 2;
            canvas_offset_y -= (canvas_offset_y - center_y) / 2;
            cell_width /= 2;
        } else {
            canvas_offset_x += (canvas_offset_x - center_x);
            canvas_offset_y += (canvas_offset_y - center_y);
            cell_width *= 2;
        }
    }

    void zoom_at(boolean out, int center_x, int center_y) {
        zoom(out, center_x, center_y);
    }

    void zoom_centered(boolean out) {
        zoom(out, canvas_width / 2, canvas_height / 2);
    }

    void zoom_to(float level) {
        while (cell_width > level) {
            zoom_centered(true);
        }

        while (cell_width * 2 < level) {
            zoom_centered(false);
        }
    }

    /*
    This implementation calculates the center of the bounding rectangle based on the
    passed Bounds and the current canvas_offset_x and canvas_offset_y values. It then
     determines the zoom factor (either 1.1 for zooming out or 0.9 for zooming in)
     and updates the cell_width accordingly. Finally, the canvas_offset_x and canvas_offset_y
      values are updated to keep the center of the bounding rectangle at the same
      position on the screen during the zoom operation.

    You can call this zoom_bounds method with the out parameter
    set to true or false and the Bounds instance you want to zoom in or out from.
     */
    public void zoom_bounds(boolean out, Bounds bounds) {
        float width = bounds.right - bounds.left;
        float height = bounds.bottom - bounds.top;

        // Calculate the center of the bounding rectangle
        float centerX = canvas_offset_x + (bounds.left + width / 2) * cell_width;
        float centerY = canvas_offset_y + (bounds.top + height / 2) * cell_width;

        // Calculate the zoom factor
        float zoomFactor = out ? 1.01f : 0.99f;

        // Update the cell width
        cell_width *= zoomFactor;

        // Update canvas offsets to keep the center of the bounding rectangle at the same position on the screen
        canvas_offset_x = (int) (centerX - (bounds.left + width / 2) * cell_width);
        canvas_offset_y = (int) (centerY - (bounds.top + height / 2) * cell_width);

        zoom_at(out, (int) centerX, (int) centerY);
    }


    void center_view() {
        canvas_offset_x = canvas_width / 2;
        canvas_offset_y = canvas_height / 2;
    }

    void fit_bounds(Bounds bounds) {
        float width = bounds.right - bounds.left;
        float height = bounds.bottom - bounds.top;
        float relative_size;
        float x, y;
        if (isFinite(width) && isFinite(height)) {
            relative_size = PApplet.min(16, // maximum cell size
                    canvas_width / width, // relative width
                    canvas_height / height // relative height
            );
            zoom_to(relative_size);

            x = canvas_width / 2 - (bounds.left + width / 2) * cell_width;
            y = canvas_height / 2 - (bounds.top + height / 2) * cell_width;
        } else {
            // can happen if the pattern is empty or very large
            zoom_to(16);

            x = canvas_width / 2;
            y = canvas_height / 2;
        }

        canvas_offset_x = Math.round(x);
        canvas_offset_y = Math.round(y);
    }

    void draw_bounds(Bounds bounds) {

        float width = bounds.right - bounds.left;
        float height = bounds.bottom - bounds.top;

        // Calculate the top-left corner coordinates of the bounding rectangle
        float x = canvas_offset_x + bounds.left * cell_width;
        float y = canvas_offset_y + bounds.top * cell_width;

        p.noFill();
        p.stroke(200);
        p.strokeWeight(1);
        p.rect(x, y, width * cell_width + cell_width, height * cell_width + cell_width);

    }

    void draw_cell(int x, int y, boolean set) {
        float cell_x = x * cell_width + canvas_offset_x;
        float cell_y = y * cell_width + canvas_offset_y;
        float width = (float) Math.ceil(cell_width) - (int) (cell_width * border_width_ratio);
        if (set) {
            p.fill(cell_color);
        } else {
            p.fill(background_color);
        }

        p.noStroke();
        p.rect(cell_x, cell_y, width, width);
    }


    boolean isFinite(float value) {
        return !Float.isInfinite(value) && !Float.isNaN(value);
    }
}