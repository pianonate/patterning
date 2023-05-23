import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PVector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

class PatternDrawer {

    public static PFont font;
    private static final BigDecimal BigTWO = new BigDecimal(2);
    private static final Stack<CanvasState> previousStates = new Stack<>();
    private static final int DEFAULT_CELL_WIDTH = 4;
    // for all conversions, this needed to be a number larger than 5 for
    // there not to be rounding errors
    // which caused cells to appear outside the bounds
    private static final MathContext mc = new MathContext(10);
    final int cellColor = 255;
    final float cellBorderWidthRatio = .05F;
    private final PApplet processing;

    private final DrawRateController drawRateController;

    private final HUDStringBuilder hudInfo;
    private final MovementHandler movementHandler;
    private final List<Drawable> drawables = new ArrayList<>();
    private final TextDisplay countdownText;
    private final TextDisplay hudText;
    float cellBorderWidth = 0.0F;

    private PGraphics backgroundBuffer;
    private PGraphics lifeFormBuffer;
    private PGraphics UXBuffer;


    // this is used because we now separate the drawing speed from the framerate
    // so we may not draw an image every frame
    // if we haven't drawn an image, we still want to be able to move and drag
    // the image around so this allows us to keep track of the current position
    // for the lifeFormBuffer and move that buffer around regardless of whether we've drawn an image
    // whenever an image is drawn, ths PVector is reset to 0,0 to match the current image state
    // it's a nifty way to handle things - just follow lifeFormPosition through the code
    // to see what i'm talking about
    PVector lifeFormPosition = new PVector(0,0);


    private boolean drawBounds;
    private BigDecimal canvasOffsetX = BigDecimal.ZERO;
    private BigDecimal canvasOffsetY = BigDecimal.ZERO;
    // if we're going to be operating in BigDecimal then we keep these that way so
    // that calculations can be done without conversions until necessary
    private BigDecimal canvasWidth;
    private BigDecimal canvasHeight;

    // used for resize detection
    private int prevWidth, prevHeight;



    private CellWidth cellWidth;
    PatternDrawer(PApplet pApplet, List<ControlPanel> panels, DrawRateController drawRateController) {
        this.processing = pApplet;

        font = processing.createFont("Verdana",24);

        // initial height in case we resize
        prevWidth = pApplet.width;
        prevHeight = pApplet.height;

        this.cellWidth = new CellWidth(DEFAULT_CELL_WIDTH);
        this.canvasWidth = BigDecimal.valueOf(pApplet.width);
        this.canvasHeight = BigDecimal.valueOf(pApplet.height);

        this.UXBuffer = getBuffer();
        this.lifeFormBuffer= getBuffer();
        this.backgroundBuffer = getBuffer();
        
        this.movementHandler = new MovementHandler(this);
        this.drawBounds = false;
        this.hudInfo = new HUDStringBuilder();

        countdownText = new TextDisplay.Builder("counting down - press space to begin immediately", PApplet.CENTER, PApplet.CENTER)
                .runMethod(Patterning::run)
                .fadeInDuration(2000)
                .countdownFrom(3)
                .build();

        TextDisplay startupText = new TextDisplay.Builder("Welcome to Patterning", PApplet.LEFT, PApplet.TOP)
                .textSize(60)
                .fadeInDuration(2000)
                .fadeOutDuration(2000)
                .duration(4000)
                .build();

        hudText = new TextDisplay.Builder("HUD", PApplet.RIGHT, PApplet.BOTTOM)
                .textSize(24)
                .build();

        drawables.addAll(panels);

        drawables.add(countdownText);
        drawables.add(startupText);
        drawables.add(hudText);

        startupText.startDisplay();
        hudText.startDisplay();

        this.drawRateController = drawRateController;

    }

    private PGraphics getBuffer() {
        return processing.createGraphics(processing.width, processing.height);
    }

    public void toggleDrawBounds() {
        drawBounds = !drawBounds;
    }

    private BigDecimal calcCenterOnResize(BigDecimal dimension, BigDecimal offset) {
        return (dimension.divide(BigTWO, mc)).subtract(offset);
    }

    public void setupNewLife(Bounds bounds) {

        center(bounds, true, false);

        // todo: on maximum volatility gun, not clearing the previousStates when doing a setStep seems to cause it to freak out
        // see if that's causal
        // clear image cache and previous states
        clearCache();

        countdownText.startCountdown();

    }

    public void handlePause() {
        if (countdownText.isDisplaying) {
            countdownText.interruptCountdown();
        } else {
            Patterning.toggleRun();
        }
    }

    private void surfaceResized() {

        BigDecimal bigWidth = BigDecimal.valueOf(processing.width);
        BigDecimal bigHeight = BigDecimal.valueOf(processing.height);

        if (bigHeight.equals(canvasHeight) && bigWidth.equals(canvasWidth)) {
            return;
        }

        UXBuffer = getBuffer();
        lifeFormBuffer = getBuffer();
        backgroundBuffer = getBuffer();

        // Calculate the center of the visible portion before resizing
        BigDecimal centerXBefore = calcCenterOnResize(canvasWidth, canvasOffsetX);
        BigDecimal centerYBefore = calcCenterOnResize(canvasHeight, canvasOffsetY);

        // Update the canvas size
        canvasWidth = bigWidth;
        canvasHeight = bigHeight;

        // Calculate the center of the visible portion after resizing
        BigDecimal centerXAfter = calcCenterOnResize(bigWidth, canvasOffsetX);
        BigDecimal centerYAfter = calcCenterOnResize(bigHeight, canvasOffsetY);

        // Calculate the difference in the visible portion's center
        BigDecimal offsetX = centerXAfter.subtract(centerXBefore);
        BigDecimal offsetY = centerYAfter.subtract(centerYBefore);

        updateCanvasOffsets(offsetX, offsetY);

    }

    public void clearCache() {
        previousStates.clear();
    }

    public float getCellWidth() {
        return cellWidth.get();
    }

    public void zoom(boolean in, float x, float y) {

        previousStates.push(new CanvasState(cellWidth, canvasOffsetX, canvasOffsetY));

        float previousCellWidth = cellWidth.get();

        // Adjust cell width to align with grid
        if (in) {
            cellWidth.set(previousCellWidth * 2f);
        } else {
            cellWidth.set(previousCellWidth / 2f);
        }

        // Calculate zoom factor
        float zoomFactor = cellWidth.get() / previousCellWidth;

        // Calculate the difference in canvas offset-s before and after zoom
        float offsetX = (1 - zoomFactor) * (x - canvasOffsetX.floatValue());
        float offsetY = (1 - zoomFactor) * (y - canvasOffsetY.floatValue());

        // Update canvas offsets
        updateCanvasOffsets(BigDecimal.valueOf(offsetX), BigDecimal.valueOf(offsetY));

    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void fillSquare(float x, float y, float size) {

        float width = size - cellBorderWidth;
        lifeFormBuffer.noStroke();
        lifeFormBuffer.rect(x, y, width, width);

        // todo, create a keyboard handler to display these values
        //       or a mouse mode - it can be useful when debugging positioning information
        // you'll be glad you did
        if (false) {
            lifeFormBuffer.fill(0xff000000);
            lifeFormBuffer.textSize(11);
            lifeFormBuffer.textAlign(processing.LEFT, processing.TOP);
            lifeFormBuffer.text((int) x + ",  " + (int) y, x, y, width, width);
            lifeFormBuffer.text(canvasWidth.toString() + ",  " + canvasHeight.toString(), x, y + 12, width, width);

            lifeFormBuffer.text("size: " + size, x, y + 24, width, width);

            int nextPos = (int) x + (int) size;
            lifeFormBuffer.text("next: " + nextPos, x, y + 36, width, width);
        }

    }

    private void drawNode(Node node, BigDecimal size, BigDecimal left, BigDecimal top, DrawNodeContext ctx) {

        if (node.population.equals(BigInteger.ZERO)) {
            return;
        }

        BigDecimal leftWithOffset = left.add(ctx.canvasOffsetXDecimal);
        BigDecimal topWithOffset = top.add(ctx.canvasOffsetYDecimal);
        BigDecimal leftWithOffsetAndSize = leftWithOffset.add(size);
        BigDecimal topWithOffsetAndSize = topWithOffset.add(size);

        // no need to draw anything not visible on screen
        if (leftWithOffsetAndSize.compareTo(BigDecimal.ZERO) < 0
                || topWithOffsetAndSize.compareTo(BigDecimal.ZERO) < 0
                || leftWithOffset.compareTo(ctx.canvasWidthDecimal) >= 0
                || topWithOffset.compareTo(ctx.canvasHeightDecimal) >= 0) {
            return;
        }

        // if we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size.compareTo(BigDecimal.ONE) <= 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                lifeFormBuffer.fill(cellColor);
                fillSquare(Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()),
                        1);
            }
        } else if (node.level == 0) {
            if (node.population.equals(BigInteger.ONE)) {
                lifeFormBuffer.fill(cellColor);
                fillSquare(Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()),
                        cellWidth.get());
            }
        } else {

            BigDecimal halfSize = ctx.getHalfSize(size);
            BigDecimal leftHalfSize = left.add(halfSize);
            BigDecimal topHalfSize = top.add(halfSize);

            drawNode(node.nw, halfSize, left, top, ctx);
            drawNode(node.ne, halfSize, leftHalfSize, top, ctx);
            drawNode(node.sw, halfSize, left, topHalfSize, ctx);
            drawNode(node.se, halfSize, leftHalfSize, topHalfSize, ctx);

        }
    }

    // called when moves are invoked as there is some trickery in the move handler to move
    // multiple times on key presses and even more so as they are held down
    // we just want to go back to the first one...
    public void saveUndoState() {
        previousStates.push(new CanvasState(cellWidth, canvasOffsetX, canvasOffsetY));
    }

    public void move(float dx, float dy) {
        saveUndoState();
        updateCanvasOffsets(BigDecimal.valueOf(dx), BigDecimal.valueOf(dy));
        lifeFormPosition.add(dx, dy);
    }

    private void updateCanvasOffsets(BigDecimal offsetX, BigDecimal offsetY) {
        canvasOffsetX = canvasOffsetX.add(offsetX);
        canvasOffsetY = canvasOffsetY.add(offsetY);
    }

    void zoomXY(boolean in, float x, float y) {
        zoom(in, x, y);
    }

    /*
        this will only undo one step. If you want to be able to undo multiple steps,
         you would need to store all previous states, not just the last one.
         You could use a Stack<CanvasState> for this purpose.
     */
    public void undoMovement() {
        if (!previousStates.empty()) {
            CanvasState previous = previousStates.pop();

            cellWidth = previous.cellWidth();
            canvasOffsetX = previous.canvasOffsetX();
            canvasOffsetY = previous.canvasOffsetY();
        }

        // if you want to float something on screen that says nothing more to pop you can
        // or just make a noise
        // or show some sparkles or something
    }

    public void center(Bounds bounds, boolean fitBounds, boolean saveState) {


        if (saveState) {
            previousStates.push(new CanvasState(cellWidth, canvasOffsetX, canvasOffsetY));
        }

        // remember, bounds are inclusive - if you want the count of discrete items, then you need to add one back to it
        BigDecimal patternWidth = new BigDecimal(bounds.right.subtract(bounds.left).add(BigInteger.ONE));
        BigDecimal patternHeight = new BigDecimal(bounds.bottom.subtract(bounds.top).add(BigInteger.ONE));

        if (fitBounds) {

            BigDecimal widthRatio = (patternWidth.compareTo(BigDecimal.ZERO) > 0) ? canvasWidth.divide(patternWidth, mc)
                    : BigDecimal.ONE;
            BigDecimal heightRatio = (patternHeight.compareTo(BigDecimal.ZERO) > 0)
                    ? canvasHeight.divide(patternHeight, mc)
                    : BigDecimal.ONE;

            BigDecimal newCellSize = (widthRatio.compareTo(heightRatio) < 0) ? widthRatio : heightRatio;

            cellWidth.set(newCellSize.floatValue() * .9F);

        }

        BigDecimal bigCell = cellWidth.getAsBigDecimal();

        BigDecimal drawingWidth = patternWidth.multiply(bigCell);
        BigDecimal drawingHeight = patternHeight.multiply(bigCell);

        BigDecimal halfCanvasWidth = canvasWidth.divide(BigTWO, mc);
        BigDecimal halfCanvasHeight = canvasHeight.divide(BigTWO, mc);

        BigDecimal halfDrawingWidth = drawingWidth.divide(BigTWO, mc);
        BigDecimal halfDrawingHeight = drawingHeight.divide(BigTWO, mc);

        // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
        BigDecimal offsetX = halfCanvasWidth.subtract(halfDrawingWidth).add(bounds.leftToBigDecimal().multiply(bigCell).negate());
        BigDecimal offsetY = halfCanvasHeight.subtract(halfDrawingHeight).add(bounds.topToBigDecimal().multiply(bigCell).negate());

        canvasOffsetX = offsetX;
        canvasOffsetY = offsetY;
    }

    public void drawBounds(Bounds bounds) {

        if (!drawBounds) return;

        // use the bounds of the "living" section of the universe to determine
        // a visible boundary based on the current canvas offsets and cell size
        Bounds screenBounds = bounds.getScreenBounds(cellWidth.get(), canvasOffsetX, canvasOffsetY);

        lifeFormBuffer.pushStyle();
        lifeFormBuffer.noFill();
        lifeFormBuffer.stroke(200);
        lifeFormBuffer.strokeWeight(1);
        lifeFormBuffer.rect(screenBounds.leftToFloat(), screenBounds.topToFloat(), screenBounds.rightToFloat(),
                screenBounds.bottomToFloat());
        lifeFormBuffer.popStyle();
    }

    public void draw(boolean shouldDraw) {

        if (prevWidth != processing.width || prevHeight != processing.height) {
            // moral equivalent of a resize
            surfaceResized();
        }

        prevWidth = processing.width;
        prevHeight = processing.height;

        // if we're not in the middle of updating, then
        // draw the newly updated universe

        backgroundBuffer.beginDraw();
        backgroundBuffer.background(0);
        backgroundBuffer.endDraw();;

        UXBuffer.beginDraw();
        UXBuffer.clear();
        UXBuffer.textFont(font);


        LifeUniverse life = Patterning.getLifeUniverse();
        // make this threadsafe
        Node node = life.root;
        Bounds bounds = life.getRootBounds();

        // put the actual drawing onto its own buffer that you image and then stamp the rest of the displayables on top of that
        // re-use that lifeBuffer when it's doing a long running operation
        // use the frame rate manager to gat the speed of drawing the lifeform itself
        movementHandler.handleRequestedMovement();
        // (int)
        cellBorderWidth = (cellBorderWidthRatio * cellWidth.get());

        if (shouldDraw) {
            lifeFormBuffer.beginDraw();
            lifeFormBuffer.clear();
            BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1), mc).multiply(cellWidth.getAsBigDecimal(), mc);
            DrawNodeContext ctx = new DrawNodeContext();
            drawNode(node, size.multiply(BigTWO, mc), size.negate(), size.negate(), ctx);
            drawBounds(bounds);
            lifeFormBuffer.endDraw();
            lifeFormPosition.set(0, 0);
        }

        // make this threadsafe
        String hudMessage = getHUDMessage(life, bounds);
        hudText.setMessage(hudMessage);

        for (Drawable drawable : drawables) {
            drawable.draw(UXBuffer);
        }

        UXBuffer.endDraw();

        processing.image(backgroundBuffer, 0,0);
        processing.image(lifeFormBuffer, lifeFormPosition.x, lifeFormPosition.y);
        //processing.image(lifeFormBuffer,0,0);
        processing.image(UXBuffer, 0, 0);

    }

    private String getHUDMessage(LifeUniverse life, Bounds bounds) {
        Node root = life.root;

        hudInfo.addOrUpdate("fps", Math.round(processing.frameRate));
        hudInfo.addOrUpdate("dps", drawRateController.getCurrentDrawRate());
        hudInfo.addOrUpdate("cell", getCellWidth());
        hudInfo.addOrUpdate("running", (Patterning.isRunning()) ? "running" : "stopped");

        hudInfo.addOrUpdate("level: ", root.level);
        hudInfo.addOrUpdate("step", LifeUniverse.pow2(life.step));
        hudInfo.addOrUpdate("generation", life.generation);
        hudInfo.addOrUpdate("population", root.population);
        hudInfo.addOrUpdate("maxLoad", life.maxLoad);
        hudInfo.addOrUpdate("lastID", life.lastId);

        hudInfo.addOrUpdate("width", bounds.right.subtract(bounds.left));
        hudInfo.addOrUpdate("height", bounds.bottom.subtract(bounds.top));

        UXBuffer.textAlign(PApplet.RIGHT, PApplet.BOTTOM);
        // use the default delimiter
        return hudInfo.getFormattedString(processing.frameCount, 12);
    }

    // the cell width times 2 ^ level will give you the size of the whole universe
    // draws the screen size viewport on the universe

    private static class CellWidth {
        private static final float CELL_WIDTH_ROUNDING_THRESHOLD = 1.6f;
        private static final float CELL_WIDTH_ROUNDING_FACTOR = 1.0F;
        private float cellWidth;
        private BigDecimal cellWidthBigDecimal;

        public CellWidth(float cellWidth) {
            setImpl(cellWidth);
        }

        public float get() {
            return cellWidth;
        }

        // private impl created to log before/after
        // without needing to log on class construction
        public void set(float cellWidth) {
            setImpl(cellWidth);
        }

        private void setImpl(float cellWidth) {

            // Apply rounding conditionally based on a threshold
            // if it's larger than a pixel then we may see unintended gaps between cells
            // so round them if they're over the 1 pixel threshold
            if (cellWidth > CELL_WIDTH_ROUNDING_THRESHOLD) {
                cellWidth = Math.round(cellWidth * CELL_WIDTH_ROUNDING_FACTOR) / CELL_WIDTH_ROUNDING_FACTOR;
            }

            this.cellWidth = cellWidth;
            this.cellWidthBigDecimal = BigDecimal.valueOf(cellWidth);
        }

        public BigDecimal getAsBigDecimal() {
            return cellWidthBigDecimal;
        }

        public String toString() {
            return "CellWidth{" + cellWidth + "}";
        }

    }

    private record CanvasState(CellWidth cellWidth, BigDecimal canvasOffsetX, BigDecimal canvasOffsetY) {
        private CanvasState(CellWidth cellWidth, BigDecimal canvasOffsetX, BigDecimal canvasOffsetY) {
            this.cellWidth = new CellWidth(cellWidth.get());
            this.canvasOffsetX = canvasOffsetX;
            this.canvasOffsetY = canvasOffsetY;
        }
    }

    private class DrawNodeContext {

        public final BigDecimal canvasWidthDecimal;
        public final BigDecimal canvasHeightDecimal;
        public final BigDecimal canvasOffsetXDecimal;
        public final BigDecimal canvasOffsetYDecimal;

        private final Map<BigDecimal, BigDecimal> halfSizeMap = new HashMap<>();

        public DrawNodeContext() {
            this.canvasWidthDecimal = canvasWidth;
            this.canvasHeightDecimal = canvasHeight;
            this.canvasOffsetXDecimal = canvasOffsetX; // new BigDecimal(canvasOffsetX);
            this.canvasOffsetYDecimal = canvasOffsetY; // new BigDecimal(canvasOffsetY);
        }

        public BigDecimal getHalfSize(BigDecimal size) {
            if (!halfSizeMap.containsKey(size)) {
                BigDecimal halfSize = size.divide(BigTWO, mc);
                halfSizeMap.put(size, halfSize);
            }
            return halfSizeMap.get(size);
        }
    }
}