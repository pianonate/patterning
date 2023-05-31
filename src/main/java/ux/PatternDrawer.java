package ux;

import actions.MovementHandler;
import patterning.Bounds;
import patterning.LifeUniverse;
import patterning.Node;
import patterning.Patterning;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PVector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

public class PatternDrawer {

    private static final MathContext mc = new MathContext(10);
    private static final BigDecimal BigTWO = new BigDecimal(2);
    private static final Stack<CanvasState> previousStates = new Stack<>();
    private CellWidth cellWidth;
    final float cellBorderWidthRatio = .05F;

    private static final int DEFAULT_CELL_WIDTH = 4;
    public static PFont font;
    // for all conversions, this needed to be a number larger than 5 for
    // there not to be rounding errors
    // which caused cells to appear outside the bounds
    private final PApplet processing;
    private final DrawRateManager drawRateManager;
    private final HUDStringBuilder hudInfo;
    private final MovementHandler movementHandler;
    //private final List<Drawable> drawables = new ArrayList<>();
    private final DrawableManager drawables = DrawableManager.getInstance();
    private TextPanel countdownText;
    private final TextPanel hudText;
    UXThemeManager theme = UXThemeManager.getInstance();
    float cellBorderWidth = 0.0F;

    // this is used because we now separate the drawing speed from the framerate
    // we may not draw an image every frame
    // if we haven't drawn an image, we still want to be able to move and drag
    // the image around so this allows us to keep track of the current position
    // for the lifeFormBuffer and move that buffer around regardless of whether we've drawn an image
    // whenever an image is drawn, ths PVector is reset to 0,0 to match the current image state
    // it's a nifty way to handle things - just follow lifeFormPosition through the code
    // to see what i'm talking about
    PVector lifeFormPosition = new PVector(0, 0);
    private PGraphics lifeFormBuffer;
    private PGraphics UXBuffer;
    private boolean drawBounds;
    private BigDecimal canvasOffsetX = BigDecimal.ZERO;
    private BigDecimal canvasOffsetY = BigDecimal.ZERO;
    // if we're going to be operating in BigDecimal then we keep these that way so
    // that calculations can be done without conversions until necessary
    private BigDecimal canvasWidth;
    private BigDecimal canvasHeight;

    // used for resize detection
    private int prevWidth, prevHeight;

    private List<OldControlPanel> panels;

    public PatternDrawer(PApplet pApplet,
                         DrawRateManager drawRateManager,
                         List<OldControlPanel> panels) {

        this.processing = pApplet;
        this.drawRateManager = drawRateManager;
        this.panels = panels;

        PanelTester panelTester = new PanelTester(this::getUXBuffer);

        Patterning patterning = (Patterning) processing;

        Panel testControlPanel = patterning.getTestControl(this::getUXBuffer);
        drawables.add(testControlPanel);

        font = processing.createFont("Verdana", 24);

        // initial height in case we resize
        prevWidth = pApplet.width;
        prevHeight = pApplet.height;

        this.cellWidth = new CellWidth(DEFAULT_CELL_WIDTH);
        this.canvasWidth = BigDecimal.valueOf(pApplet.width);
        this.canvasHeight = BigDecimal.valueOf(pApplet.height);

        this.UXBuffer = getBuffer();
        this.lifeFormBuffer = getBuffer();

        this.movementHandler = new MovementHandler(this);
        this.drawBounds = false;
        this.hudInfo = new HUDStringBuilder();

        TextPanel startupText = new TextPanel.Builder(this::getUXBuffer, "patterning".toUpperCase(), AlignHorizontal.RIGHT, AlignVertical.TOP)
                .textSize(50)
                .fadeInDuration(2000)
                .fadeOutDuration(2000)
                .displayDuration(4000)
                .build();

        hudText = new TextPanel.Builder(this::getUXBuffer, "HUD", AlignHorizontal.RIGHT, AlignVertical.BOTTOM)
                .textSize(24)
                .build();

        drawables.add(startupText);
        drawables.add(hudText);

    }

    private PGraphics getUXBuffer() {
        return UXBuffer;
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

        if (drawables.isManaging(countdownText)) {
            drawables.remove(countdownText);
        }

        // todo: on maximum volatility gun, not clearing the previousStates when doing a setStep seems to cause it to freak out
        // see if that's causal
        // clear image cache and previous states
        clearCache();

        countdownText = new TextPanel.Builder(this::getUXBuffer, "counting down - press space to begin immediately", AlignHorizontal.CENTER, AlignVertical.CENTER)
                .runMethod(Patterning::run)
                .fadeInDuration(2000)
                .countdownFrom(3)
                .wordWrapWidth(Optional.of(() -> canvasWidth.intValue() / 2))
                .textSize(24)
                .build();
        drawables.add(countdownText);
    }

    public void center(Bounds bounds, boolean fitBounds, boolean saveState) {


        if (saveState) {
            saveUndoState();
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

    public void clearCache() {
        previousStates.clear();
    }

    // called when moves are invoked as there is some trickery in the move handler to move
    // multiple times on key presses and even more so as they are held down
    // we just want to go back to the first one...
    public void saveUndoState() {
        previousStates.push(new CanvasState(cellWidth, canvasOffsetX, canvasOffsetY));
    }

    public void handlePause() {

        if (drawables.isManaging(countdownText)) {
            countdownText.interruptCountdown();
        } else {
            Patterning.toggleRun();
        }
    }

    private boolean isWindowResized() {
       return prevWidth != processing.width || prevHeight != processing.height;
    }

    private void updateWindowResized() {

        BigDecimal bigWidth = BigDecimal.valueOf(processing.width);
        BigDecimal bigHEight = BigDecimal.valueOf(processing.height);

        // create new buffers
        UXBuffer = getBuffer();
        lifeFormBuffer = getBuffer();

        // Calculate the center of the visible portion before resizing
        BigDecimal centerXBefore = calcCenterOnResize(canvasWidth, canvasOffsetX);
        BigDecimal centerYBefore = calcCenterOnResize(canvasHeight, canvasOffsetY);

        // Update the canvas size
        canvasWidth = bigWidth;
        canvasHeight = bigHEight;

        // Calculate the center of the visible portion after resizing
        BigDecimal centerXAfter = calcCenterOnResize(bigWidth, canvasOffsetX);
        BigDecimal centerYAfter = calcCenterOnResize(bigHEight, canvasOffsetY);

        // Calculate the difference in the visible portion's center
        BigDecimal offsetX = centerXAfter.subtract(centerXBefore);
        BigDecimal offsetY = centerYAfter.subtract(centerYBefore);

        updateCanvasOffsets(offsetX, offsetY);

    }

    public float getCellWidth() {
        return cellWidth.get();
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
                lifeFormBuffer.fill(theme.getCellColor());
                fillSquare(Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()),
                        1);
            }
        } else if (node.level == 0) {
            if (node.population.equals(BigInteger.ONE)) {
                lifeFormBuffer.fill(theme.getCellColor());
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

    public void move(float dx, float dy) {
        saveUndoState();
        updateCanvasOffsets(BigDecimal.valueOf(dx), BigDecimal.valueOf(dy));
        lifeFormPosition.add(dx, dy);
        DrawRateManager.getInstance().drawImmediately();
    }

    private void updateCanvasOffsets(BigDecimal offsetX, BigDecimal offsetY) {
        canvasOffsetX = canvasOffsetX.add(offsetX);
        canvasOffsetY = canvasOffsetY.add(offsetY);
    }

    public void zoomXY(boolean in, float x, float y) {
        zoom(in, x, y);
    }

    public void zoom(boolean zoomIn, float x, float y) {
        saveUndoState();

        float previousCellWidth = cellWidth.get();

        // Adjust cell width to align with grid
        if (zoomIn) {
            cellWidth.set(previousCellWidth * 1.25f);
        } else {
            cellWidth.set(previousCellWidth / 1.25f);
        }

        // Calculate zoom factor
        float zoomFactor = cellWidth.get() / previousCellWidth;

        // Calculate the difference in canvas offset-s before and after zoom
        float offsetX = (1 - zoomFactor) * (x - canvasOffsetX.floatValue());
        float offsetY = (1 - zoomFactor) * (y - canvasOffsetY.floatValue());

        // Update canvas offsets
        updateCanvasOffsets(BigDecimal.valueOf(offsetX), BigDecimal.valueOf(offsetY));

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

        boolean resized = isWindowResized();

        prevWidth = processing.width;
        prevHeight = processing.height;
        processing.background(theme.getBackGroundColor());



        if (resized) {
            processing.image(lifeFormBuffer, lifeFormPosition.x, lifeFormPosition.y);
            processing.image(UXBuffer, 0, 0);
            updateWindowResized();
            return;
        }


        UXBuffer.beginDraw();
        UXBuffer.clear();
        UXBuffer.textFont(font);

        LifeUniverse life = Patterning.getLifeUniverse();
        // make this threadsafe
        Node node = life.root;
        Bounds bounds = life.getRootBounds();

        movementHandler.handleRequestedMovement();
        // (int)
        cellBorderWidth = (cellBorderWidthRatio * cellWidth.get());

        // make this threadsafe
        String hudMessage = getHUDMessage(life, bounds);
        hudText.setMessage(hudMessage);

        drawables.drawAll(UXBuffer);
        for (OldControlPanel panel : panels) {
            panel.draw(UXBuffer);
        }

        UXBuffer.endDraw();

        if (shouldDraw) {
            lifeFormBuffer.beginDraw();
            lifeFormBuffer.clear();
            BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1), mc).multiply(cellWidth.getAsBigDecimal(), mc);
            DrawNodeContext ctx = new DrawNodeContext();
            drawNode(node, size.multiply(BigTWO, mc), size.negate(), size.negate(), ctx);
            drawBounds(bounds);
            lifeFormBuffer.endDraw();
            // reset the position in case you've had mouse moves
            lifeFormPosition.set(0, 0);
        }


        processing.image(lifeFormBuffer, lifeFormPosition.x, lifeFormPosition.y);
        processing.image(UXBuffer, 0, 0);



    }

    private String getHUDMessage(LifeUniverse life, Bounds bounds) {
        Node root = life.root;

        hudInfo.addOrUpdate("fps", Math.round(processing.frameRate));
        hudInfo.addOrUpdate("dps", Math.round(drawRateManager.getCurrentDrawRate()));
        hudInfo.addOrUpdate("cell", getCellWidth());
        hudInfo.addOrUpdate("running", (Patterning.isRunning()) ? "running" : "stopped");

        hudInfo.addOrUpdate("level ", root.level);
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

        public float get() {
            return cellWidth;
        }

        // private impl created to log before/after
        // without needing to log on class construction
        public void set(float cellWidth) {
            setImpl(cellWidth);
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