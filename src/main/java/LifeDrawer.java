import processing.core.PApplet;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;


class LifeDrawer {
    PApplet p;
    BigInteger canvas_offset_x = BigInteger.ZERO;
    BigInteger canvas_offset_y = BigInteger.ZERO;

    MathContext mc = new MathContext(400);
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

        System.out.println("resize");
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

    void move(float dx, float dy) {
        updateCanvasOffsets(dx, dy);
    }

    void zoom(boolean in, float x, float y) {
        float previousCellWidth = cell_width;

        // Adjust cell width to align with grid
        if (in) {
            cell_width = cell_width * 2.0f;
        } else {
            cell_width = cell_width / 2.0f;
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

    void zoom_at(boolean in, float mouse_x, float mouse_y) {
        zoom(in, mouse_x, mouse_y);
    }

    void center_view(Bounds bounds) {

        BigDecimal patternWidth = new BigDecimal(bounds.right.subtract(bounds.left));
        BigDecimal patternHeight = new BigDecimal(bounds.bottom.subtract(bounds.top));

        BigDecimal drawingWidth = patternWidth.multiply(BigDecimal.valueOf(cell_width));
        BigDecimal drawingHeight = patternHeight.multiply(BigDecimal.valueOf(cell_width));

        // Assuming canvas_width and canvas_height are int values representing the visible portion of the drawing
        BigDecimal halfCanvasWidth = new BigDecimal(canvas_width).divide(new BigDecimal(2), mc);
        BigDecimal halfCanvasHeight = new BigDecimal(canvas_height).divide(new BigDecimal(2), mc);

        BigDecimal halfDrawingWidth = drawingWidth.divide(new BigDecimal(2), mc);
        BigDecimal halfDrawingHeight = drawingHeight.divide(new BigDecimal(2), mc);

        BigDecimal offsetX = halfCanvasWidth.subtract(halfDrawingWidth);
        BigDecimal offsetY = halfCanvasHeight.subtract(halfDrawingHeight);

        canvas_offset_x = new BigInteger(String.valueOf(offsetX.intValue()));
        canvas_offset_y = new BigInteger(String.valueOf(offsetY.intValue()));
    }

    public void fit_bounds(Bounds bounds) {

        BigDecimal patternWidth = new BigDecimal(bounds.right.subtract(bounds.left));
        BigDecimal patternHeight = new BigDecimal(bounds.bottom.subtract(bounds.top));

        BigDecimal canvasWidthWithBorder = new BigDecimal(canvas_width - (lifeDrawingBorder));
        BigDecimal canvasHeightWithBorder = new BigDecimal(canvas_height - (lifeDrawingBorder));

        BigDecimal widthRatio = (patternWidth.compareTo(BigDecimal.ZERO) > 0) ? canvasWidthWithBorder.divide(patternWidth, mc) : BigDecimal.ONE;
        BigDecimal heightRatio = (patternHeight.compareTo(BigDecimal.ZERO) > 0) ? canvasHeightWithBorder.divide(patternHeight, mc) : BigDecimal.ONE;

        BigDecimal newCellSize = widthRatio.min(heightRatio).multiply(BigDecimal.valueOf(.8F));

        cell_width = newCellSize.floatValue();

        BigDecimal drawingWidth = patternWidth.multiply(newCellSize);
        BigDecimal drawingHeight = patternHeight.multiply(newCellSize);

        BigDecimal offsetX = canvasWidthWithBorder.subtract(drawingWidth).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));
        BigDecimal offsetY = canvasHeightWithBorder.subtract(drawingHeight).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));


        // i think given the offs can be calculated against a bounding box that is larger than a float
        // you can't use the current updateCanvasOffsets (although you could make a version to pass in these BigDecimals)
        // first make sure you have one.
        canvas_offset_x = offsetX.setScale(0, RoundingMode.HALF_UP).toBigInteger();
        canvas_offset_y = offsetY.setScale(0, RoundingMode.HALF_UP).toBigInteger();

        center_view(bounds);

        //zoom(true,(canvas_width -(float) lifeDrawingBorder) / 2, (canvas_height -(float) lifeDrawingBorder) /2 );


    }

    void draw_bounds(Bounds bounds) {

        Bounds screenBounds = bounds.getScreenBounds(cell_width, canvas_offset_x, canvas_offset_y);

        p.noFill();
        p.stroke(200);
        p.strokeWeight(1);
        p.rect(screenBounds.leftToFloat(), screenBounds.topToFloat(), screenBounds.rightToFloat(), screenBounds.bottomToFloat());
    }

    // thi work - the cell width times 2 ^ level will give you the size of the whole universe
    // draw_node will draw whatever is visible of it that you want
   void redraw(Node node) {
        border_width = (int) (border_width_ratio * cell_width);
        BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1), mc).multiply(BigDecimal.valueOf(cell_width), mc);
        draw_node(node, size.multiply(BigDecimal.valueOf(2), mc), size.negate(), size.negate(), new DrawNodeContext());
    }

    // this won't work - see your onenote and come back to regruop

    void redraw(Node node, Bounds bounds) {
        border_width = (int) (border_width_ratio * cell_width);

        draw_node(node, bounds.getScreenBounds(cell_width, canvas_offset_x, canvas_offset_y), new DrawNodeContext());
    }



    // this won't work - see your onenote and come back to regruop
    void draw_node(Node node, Bounds bounds, DrawNodeContext ctx) {
        // this is the info that showed that when it recursed - the "size" was the same size as the cell_width
        // and that was precisely when population = 1
        // eureka
        System.out.println("screen: " + canvas_width + " " + canvas_height + " - " + bounds.toString() + " population: " + node.population);

        if (node.population.equals(BigInteger.ZERO)) {
            return;
        }

        BigDecimal size = bounds.rightToBigDecimal().subtract(bounds.leftToBigDecimal());
        BigDecimal left = bounds.leftToBigDecimal();
        BigDecimal top = bounds.topToBigDecimal();


        // no need to draw anything not visible on screen
        if (bounds.right.compareTo(BigInteger.ZERO) < 0
                || bounds.bottom.compareTo(BigInteger.ZERO) < 0
                || bounds.left.compareTo(BigInteger.valueOf(canvas_width)) >= 0
                || bounds.top.compareTo(BigInteger.valueOf(canvas_height)) >= 0) {
            return;
        }

        // if we've recurse down to a very small size - fractional, and the population exist then just draw a unit square and be done
        if (size.compareTo(new BigDecimal(ctx.one)) <= 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                fill_square(left.add(ctx.canvasOffsetXDecimal).floatValue(), top.add(ctx.canvasOffsetYDecimal).floatValue(), 1);
            }
        } else if (node.level == 0) {
            // leaf node with a population of 1
            if (node.population.equals(ctx.one)) {
                // and this showed that the cell_size matches the "size" at the leaf nodes. pretty groovy
                // System.out.println("fill_square left: " + bounds.leftToFloat() + ", top: " + bounds.topToFloat() + ", cellWidth: " + cell_width);
                fill_square(left.add(ctx.canvasOffsetXDecimal).floatValue(), top.add(ctx.canvasOffsetYDecimal).floatValue(), cell_width);
            }
        } else {
            // cached halfSize for each level - speeds up the drawing performance quite a bit
            BigDecimal halfSize = ctx.getHalfSize(size);

            draw_node(node.nw, new Bounds(top, left, top.add(halfSize),left.add(halfSize)), ctx);
            draw_node(node.ne, new Bounds(top, left.add(halfSize), top.add(halfSize), bounds.rightToBigDecimal()), ctx);
            draw_node(node.sw, new Bounds(top.add(halfSize), left, bounds.bottomToBigDecimal(), left.add(halfSize)), ctx);
            draw_node(node.se, new Bounds(top.add(halfSize), left.add(halfSize), bounds.bottomToBigDecimal(), bounds.rightToBigDecimal()), ctx);

        }
    }


    class DrawNodeContext {

        public final BigDecimal canvasWidthDecimal;
        public final BigDecimal canvasHeightDecimal;
        public final BigDecimal canvasOffsetXDecimal;
        public final BigDecimal canvasOffsetYDecimal;

        public final BigInteger one;

        private final Map<BigDecimal, BigDecimal> halfSizeMap = new HashMap<>();

        public DrawNodeContext() {
            this.canvasWidthDecimal = BigDecimal.valueOf(canvas_width);
            this.canvasHeightDecimal = BigDecimal.valueOf(canvas_height);
            this.canvasOffsetXDecimal = new BigDecimal(canvas_offset_x);
            this.canvasOffsetYDecimal = new BigDecimal(canvas_offset_y);
            this.one = BigInteger.ONE;
        }

        public BigDecimal getHalfSize(BigDecimal size) {
            if (!halfSizeMap.containsKey(size)) {
                BigDecimal halfSize = size.divide(BigDecimal.valueOf(2), mc);
                halfSizeMap.put(size, halfSize);
            }
            return halfSizeMap.get(size);
        }

    }

    void draw_node(Node node, BigDecimal size, BigDecimal left, BigDecimal top, DrawNodeContext ctx) {

        // this is the info that showed that when it recursed - the "size" was the same size as the cell_width
        // and that was precisely when population = 1
        // eureka
        // System.out.println("screen: " + canvas_width + " " + canvas_height + " - top: " + top + " left: " + left + " size: " + size + " population: " + node.population);


        if (node.population.equals(BigInteger.ZERO)) {
            return;
        }

        // no need to draw anything not visible on screen
        // this protects the call fill_square below
        if (left.add(size).add(ctx.canvasOffsetXDecimal).compareTo(BigDecimal.ZERO) < 0
                || top.add(size).add(ctx.canvasOffsetYDecimal).compareTo(BigDecimal.ZERO) < 0
                || left.add(ctx.canvasOffsetXDecimal).compareTo(ctx.canvasWidthDecimal) >= 0
                || top.add(ctx.canvasOffsetYDecimal).compareTo(ctx.canvasHeightDecimal) >= 0) {
            return;
        }

        // if we've recursed down to a very small size - fractional, and the population exist then just draw a unit square and be done
        if (size.compareTo(new BigDecimal(ctx.one)) <= 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                fill_square(left.add(ctx.canvasOffsetXDecimal).floatValue(), top.add(ctx.canvasOffsetYDecimal).floatValue(), 1);
            }
        } else if (node.level == 0) {
            // leaf node with a population of 1
            // if (node.population.compareTo(BigInteger.ZERO) > 0) {
            if (node.population.equals(ctx.one)) {
                // and this showed that the cell_size matches the "size" at the leaf nodes. pretty groovy
                // System.out.println("fill_square left: " + left.add(ctx.canvasOffsetXDecimal).floatValue() + ", top: " +  top.add(ctx.canvasOffsetYDecimal).floatValue() + ", cellWidth: " + cell_width);

                fill_square(left.add(ctx.canvasOffsetXDecimal).floatValue(), top.add(ctx.canvasOffsetYDecimal).floatValue(), cell_width);
            }
        } else {

            // cached halfSize for each level - speeds up the drawing performance quite a bit
            BigDecimal halfSize = ctx.getHalfSize(size);
            draw_node(node.nw, halfSize, left, top, ctx);
            draw_node(node.ne, halfSize, left.add(halfSize), top, ctx);
            draw_node(node.sw, halfSize, left, top.add(halfSize), ctx);
            draw_node(node.se, halfSize, left.add(halfSize), top.add(halfSize), ctx);
        }
    }


    void draw_cell(int x, int y, boolean set) {
        // todo: something is happening when you get to a step size at 1024
        //       you can go that large and because you are using a math context to do the division
        //       everything seems to work but at step size of 1024, the drawing starts to go wonky
        //       so can you... maybe keep everything in BigDecimal until you convert it somehow?
        //       the initial size passed into draw_cell is the largest possible size of the drawing
        //       based on the level - but that's so large it can't possibly matter.  is there a way
        //       to just keep track of the part of the drawing that is on screen and ask
        //       the lifeUniverse to only give you that much of it without having to use all this recursion?
        //       seems inefficient
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