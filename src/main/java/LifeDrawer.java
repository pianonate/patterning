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


    // if size is BigDecimal due to the fact that cell_width is a float
    // todo: should our offsets also just be kept in decimal rather than bigint?
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


    void fill_square(float x, float y, float size) {
        float width = size - border_width;

        p.noStroke();
        p.fill(cell_color);
        // always drawing squares
        p.rect(x, y, width, width);
    }

    void setSize(int width, int height) {
        if (width != canvas_width || height != canvas_height) {
            canvas_width = width;
            canvas_height = height;
        }
    }

    void redraw(Node node) {
        border_width = (int) (border_width_ratio * cell_width);
        BigDecimal size = BigDecimal.valueOf(Math.pow(2, node.level - 1) * cell_width);
        draw_node(node, size.multiply(BigDecimal.valueOf(2)), size.negate().toBigInteger(), size.negate().toBigInteger());
    }

    void move(float dx, float dy) {
        BigInteger dxRounded = BigInteger.valueOf(Math.round(dx));
        BigInteger dyRounded = BigInteger.valueOf(Math.round(dy));

        canvas_offset_x = canvas_offset_x.add(dxRounded);
        canvas_offset_y = canvas_offset_y.add(dyRounded);
    }


    void zoom(float newCellWidth, float mouse_x, float mouse_y) {
        BigDecimal scaleFactor = BigDecimal.valueOf(newCellWidth / cell_width);

        BigDecimal mouseX = BigDecimal.valueOf(mouse_x);
        BigDecimal mouseY = BigDecimal.valueOf(mouse_y);

        BigInteger offsetXBeforeZoom = canvas_offset_x.multiply(scaleFactor.setScale(0, RoundingMode.HALF_UP).toBigInteger());
        BigInteger offsetYBeforeZoom = canvas_offset_y.multiply(scaleFactor.setScale(0, RoundingMode.HALF_UP).toBigInteger());

        BigDecimal zoomPointOffsetX = mouseX.subtract(BigDecimal.valueOf(canvas_offset_x.intValue()));
        BigDecimal zoomPointOffsetY = mouseY.subtract(BigDecimal.valueOf(canvas_offset_y.intValue()));

        BigDecimal zoomedOffsetX = zoomPointOffsetX.multiply(scaleFactor).subtract(zoomPointOffsetX).setScale(0, RoundingMode.HALF_UP);
        BigDecimal zoomedOffsetY = zoomPointOffsetY.multiply(scaleFactor).subtract(zoomPointOffsetY).setScale(0, RoundingMode.HALF_UP);

        canvas_offset_x = offsetXBeforeZoom.add(zoomedOffsetX.toBigInteger());
        canvas_offset_y = offsetYBeforeZoom.add(zoomedOffsetY.toBigInteger());

        cell_width = Math.round(newCellWidth);

        System.out.println("cell_width updated in zoom(): " + cell_width);
        if (cell_width == 0) {
            System.out.println("zero'd baby");
        }
    }

    void zoom(boolean out, float x, float y) {
        BigInteger xBigInt = BigInteger.valueOf(Math.round(x));
        BigInteger yBigInt = BigInteger.valueOf(Math.round(y));

        BigInteger offsetX = out ?
                canvas_offset_x.subtract(canvas_offset_x.subtract(xBigInt).divide(BigInteger.TWO)) :
                canvas_offset_x.add(canvas_offset_x.subtract(xBigInt));
        BigInteger offsetY = out ?
                canvas_offset_y.subtract(canvas_offset_y.subtract(yBigInt).divide(BigInteger.TWO)) :
                canvas_offset_y.add(canvas_offset_y.subtract(yBigInt));

        // Adjust cell width to align with grid
        if (out) {
            cell_width = cell_width / 2.0f;
        } else {
            cell_width = cell_width * 2.0f;
        }
        cell_width = Math.round(cell_width);

        canvas_offset_x = offsetX;
        canvas_offset_y = offsetY;
    }


    void zoom_at(boolean out, float mouse_x, float mouse_y) {
        zoom(out, mouse_x, mouse_y);
    }

    void zoom_centered(float newCellWidth) {
        float center_x = canvas_width / 2.0f;
        float center_y = canvas_height / 2.0f;

        zoom(newCellWidth, center_x, center_y);
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
        BigInteger width = bounds.right.subtract(bounds.left);
        BigInteger height = bounds.bottom.subtract(bounds.top);

        // Calculate the center of the bounding rectangle
        BigDecimal centerX = new BigDecimal(canvas_offset_x).add(new BigDecimal(bounds.left).add(new BigDecimal(width).divide(BigDecimal.valueOf(2),mc)).multiply(BigDecimal.valueOf(cell_width)));
        BigDecimal centerY = new BigDecimal(canvas_offset_y).add(new BigDecimal(bounds.top).add(new BigDecimal(height).divide(BigDecimal.valueOf(2), mc)).multiply(BigDecimal.valueOf(cell_width)));

        // Calculate the zoom factor
        BigDecimal zoomFactor = out ? BigDecimal.valueOf(1.01) : BigDecimal.valueOf(0.99);

        // Update the cell width
        BigDecimal newCellWidth = BigDecimal.valueOf(cell_width).multiply(zoomFactor);
        cell_width = newCellWidth.floatValue();

        // Update canvas offsets to keep the center of the bounding rectangle at the same position on the screen
        canvas_offset_x = new BigInteger(String.valueOf(centerX.subtract(new BigDecimal(width).divide(BigDecimal.valueOf(2), mc).multiply(newCellWidth)).intValueExact()));
        canvas_offset_y = new BigInteger(String.valueOf(centerY.subtract(new BigDecimal(height).divide(BigDecimal.valueOf(2), mc).multiply(newCellWidth)).intValueExact()));

        zoom_at(out, centerX.intValueExact(), centerY.intValueExact());
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

        BigDecimal widthRatio = canvasWidth.divide(width, mc);
        BigDecimal heightRatio = canvasHeight.divide(height, mc);

        BigDecimal newCellSize = widthRatio.min(heightRatio);

        cell_width = newCellSize.floatValue();

        BigDecimal drawingWidth = width.multiply(newCellSize);
        BigDecimal drawingHeight = height.multiply(newCellSize);

        BigDecimal offsetX = canvasWidth.subtract(drawingWidth).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));
        BigDecimal offsetY = canvasHeight.subtract(drawingHeight).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));

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


    boolean isFinite(float value) {
        return !Float.isInfinite(value) && !Float.isNaN(value);
    }
}