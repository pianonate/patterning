import processing.core.PApplet;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;


class LifeDrawer {
    PApplet p;
    BigInteger canvas_offset_x = BigInteger.ZERO;
    BigInteger canvas_offset_y = BigInteger.ZERO;

    MathContext mc = new MathContext(300);
    int canvas_width;
    int canvas_height;
    int border_width;
    int cell_color = 0;
    int background_color = 255;
    float cell_width;
    float border_width_ratio = 0;

    private final int lifeDrawingBorder;


    LifeDrawer(PApplet p, float cell_width) {
        this.p = p;
        this.cell_width = cell_width;
        this.canvas_width = p.width;
        this.canvas_height = p.height;
        this.lifeDrawingBorder = 50;
    }


    void fill_square(float x, float y, float size) {
        float width = size - border_width;

        p.noStroke();
        p.fill(cell_color);
        // always drawing squares
        p.rect(x, y, width, width);
    }

    // formerly known as setSize()
    // make sure that when you resize it tries to show you the contents of the last screen size
    // without updating the cellsize - by getting the center before and then centering
    // around the center after
    void surfaceResized(int width, int height) {

        if (width != canvas_width || height != canvas_height) {

            // Calculate the center of the visible portion before resizing
            float centerXBefore = (canvas_width / 2.0f) - canvas_offset_x.floatValue();
            float centerYBefore = (canvas_height / 2.0f) - canvas_offset_y.floatValue();

            // Update the canvas size
            canvas_width = width;
            canvas_height = height;

            // Calculate the center of the visible portion after resizing
            float centerXAfter = (width / 2.0f) - canvas_offset_x.floatValue();
            float centerYAfter = (height / 2.0f) - canvas_offset_y.floatValue();

            // Calculate the difference in the visible portion's center
            float offsetX = centerXAfter - centerXBefore;
            float offsetY = centerYAfter - centerYBefore;

            updateCanvasOffsets(offsetX, offsetY);
        }

    }

    void redraw(Node node) {
        border_width = (int) (border_width_ratio * cell_width);
        BigDecimal size = BigDecimal.valueOf(Math.pow(2, node.level - 1) * cell_width);
        //BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1)).multiply(BigDecimal.valueOf(cell_width));
        draw_node(node, size.multiply(BigDecimal.valueOf(2)), size.negate().toBigInteger(), size.negate().toBigInteger());
    }

    void move(float dx, float dy) {
        updateCanvasOffsets(dx, dy);
    }

    void zoom(boolean out, float x, float y) {
        float previousCellWidth = cell_width;

        // Adjust cell width to align with grid
        if (out) {
            cell_width = cell_width / 2.0f;
        } else {
            cell_width = cell_width * 2.0f;
        }

        // Apply rounding conditionally based on a threshold
        float threshold = 4.0f; // You can adjust this value based on your requirements
        if (cell_width >= threshold) {
            cell_width = Math.round(cell_width);
        }

        // Calculate zoom factor
        float zoomFactor = cell_width / previousCellWidth;

        // Calculate the difference in canvas offsets before and after zoom
        float offsetX = (1 - zoomFactor) * (x - canvas_offset_x.floatValue());
        float offsetY = (1 - zoomFactor) * (y - canvas_offset_y.floatValue());

        // Update canvas offsets
        updateCanvasOffsets(offsetX, offsetY);

    }

    void updateCanvasOffsets(float offsetX, float offsetY) {
        canvas_offset_x = canvas_offset_x.add(BigInteger.valueOf(Math.round(offsetX)));
        canvas_offset_y = canvas_offset_y.add(BigInteger.valueOf(Math.round(offsetY)));
    }

    void zoom_at(boolean out, float mouse_x, float mouse_y) {
        zoom(out, mouse_x, mouse_y);
    }

    void center_view(Bounds bounds) {

        BigInteger drawingWidth = new BigInteger(String.valueOf(bounds.right)).subtract(new BigInteger(String.valueOf(bounds.left)));
        BigInteger drawingHeight = new BigInteger(String.valueOf(bounds.bottom)).subtract(new BigInteger(String.valueOf(bounds.top)));

        // Assuming canvas_width and canvas_height are int values representing the visible portion of the drawing
        BigDecimal halfCanvasWidth = new BigDecimal(canvas_width).divide(new BigDecimal(2), mc);
        BigDecimal halfCanvasHeight = new BigDecimal(canvas_height).divide(new BigDecimal(2), mc);

        BigDecimal halfDrawingWidth = new BigDecimal(drawingWidth).divide(new BigDecimal(2), mc);
        BigDecimal halfDrawingHeight = new BigDecimal(drawingHeight).divide(new BigDecimal(2), mc);

        BigDecimal offsetX = halfCanvasWidth.subtract(halfDrawingWidth);
        BigDecimal offsetY = halfCanvasHeight.subtract(halfDrawingHeight);

        canvas_offset_x = new BigInteger(String.valueOf(offsetX.intValue()));
        canvas_offset_y = new BigInteger(String.valueOf(offsetY.intValue()));
    }

    public void fit_bounds(Bounds bounds) {

        BigDecimal width = new BigDecimal(bounds.right.subtract(bounds.left));
        BigDecimal height = new BigDecimal(bounds.bottom.subtract(bounds.top));

        BigDecimal canvasWidth = new BigDecimal(canvas_width - 2 * lifeDrawingBorder);
        BigDecimal canvasHeight = new BigDecimal(canvas_height - 2 * lifeDrawingBorder);

        BigDecimal widthRatio = (width.compareTo(BigDecimal.ZERO) > 0) ?  canvasWidth.divide(width, mc) : BigDecimal.ONE;
        BigDecimal heightRatio = (height.compareTo(BigDecimal.ZERO) > 0) ? canvasHeight.divide(height, mc) : BigDecimal.ONE;

        BigDecimal newCellSize = widthRatio.min(heightRatio);

        cell_width = newCellSize.floatValue();

        BigDecimal drawingWidth = width.multiply(newCellSize);
        BigDecimal drawingHeight = height.multiply(newCellSize);

        BigDecimal offsetX = canvasWidth.subtract(drawingWidth).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));
        BigDecimal offsetY = canvasHeight.subtract(drawingHeight).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));

        // just for debugging
        BigDecimal maxFloat = BigDecimal.valueOf(Float.MAX_VALUE);

        if (offsetX.compareTo(maxFloat) > 0) {
            System.out.println("the offsetX is larger than the max float value");
        }

        // i think given the offs can be calculated against a bounding box that is larger than a float
        // you can't use the current updateCanvasOffsets (although you could make a version to pass in these BigDecimals)
        // first make sure you have one.
        canvas_offset_x = offsetX.setScale(0, RoundingMode.HALF_UP).toBigInteger();
        canvas_offset_y = offsetY.setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }


   void draw_bounds(Bounds bounds) {
        BigDecimal cellWidth = BigDecimal.valueOf(cell_width);
        BigDecimal left = new BigDecimal(bounds.left).multiply(cellWidth).add(new BigDecimal(canvas_offset_x));
        BigDecimal top = new BigDecimal(bounds.top).multiply(cellWidth).add(new BigDecimal(canvas_offset_y));
        BigDecimal width = new BigDecimal(bounds.right).subtract(new BigDecimal(bounds.left))
                .multiply(cellWidth)
                .add(cellWidth);
        BigDecimal height = new BigDecimal(bounds.bottom).subtract(new BigDecimal(bounds.top))
                .multiply(cellWidth)
                .add(cellWidth);

        float maxFloat = Float.MAX_VALUE;
        float leftFloat = (left.compareTo(BigDecimal.valueOf(maxFloat)) > 0) ? maxFloat : left.floatValue();
        float topFloat = (top.compareTo(BigDecimal.valueOf(maxFloat)) > 0) ? maxFloat : top.floatValue();
        float widthFloat = (width.compareTo(BigDecimal.valueOf(maxFloat)) > 0) ? maxFloat : width.floatValue();
        float heightFloat = (height.compareTo(BigDecimal.valueOf(maxFloat)) > 0) ? maxFloat : height.floatValue();

        p.noFill();
        p.stroke(200);
        p.strokeWeight(1);
        p.rect(leftFloat, topFloat, widthFloat, heightFloat);
    }

    // if size is BigDecimal due to the fact that cell_width is a float
    // todo: probably not - the offsets (i think) get converted to human scale
    void draw_node(Node node, BigDecimal size, BigInteger left, BigInteger top) {
        if (node.population.equals(BigInteger.ZERO)) {
            return;
        }

        BigInteger canvasWidthBigInt = BigInteger.valueOf(canvas_width);
        BigInteger canvasHeightBigInt = BigInteger.valueOf(canvas_height);

        // no need to draw anything not visible on screen
        // this protects the call fill_square below
        if (left.add(size.toBigInteger()).add(canvas_offset_x).compareTo(BigInteger.ZERO) < 0
                || top.add(size.toBigInteger()).add(canvas_offset_y).compareTo(BigInteger.ZERO) < 0
                || left.add(canvas_offset_x).compareTo(canvasWidthBigInt) >= 0
                || top.add(canvas_offset_y).compareTo(canvasHeightBigInt) >= 0) {
            return;
        }

        BigInteger one = BigInteger.ONE;

        if (size.compareTo(new BigDecimal(one)) <= 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                fill_square(left.add(canvas_offset_x).floatValue(), top.add(canvas_offset_y).floatValue(), 1);
            }
        } else if (node.level == 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                fill_square(left.add(canvas_offset_x).floatValue(), top.add(canvas_offset_y).floatValue(), cell_width);
            }
        } else {
            BigDecimal halfSize = size.divide(BigDecimal.valueOf(2), mc);
            draw_node(node.nw, halfSize, left, top);
            draw_node(node.ne, halfSize, left.add(halfSize.toBigInteger()), top);
            draw_node(node.sw, halfSize, left, top.add(halfSize.toBigInteger()));
            draw_node(node.se, halfSize, left.add(halfSize.toBigInteger()), top.add(halfSize.toBigInteger()));
        }
    }

    void draw_cell(int x, int y, boolean set) {
        BigDecimal biCellWidth = new BigDecimal(cell_width);
        BigDecimal biX = new BigDecimal(x).multiply(biCellWidth).add(new BigDecimal(canvas_offset_x));
        BigDecimal biY = new BigDecimal(y).multiply(biCellWidth).add(new BigDecimal(canvas_offset_y));
        float width = (float) Math.ceil(cell_width) - (int) (cell_width * border_width_ratio);
        if (set) {
            p.fill(cell_color);
        } else {
            p.fill(background_color);
        }

        p.noStroke();
        p.rect(biX.floatValue(), biY.floatValue(), width, width);
    }
}