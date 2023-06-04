package ux;

import actions.KeyFactory;
import actions.MouseEventManager;
import actions.MovementHandler;
import patterning.Bounds;
import patterning.LifeUniverse;
import patterning.Node;
import patterning.Patterning;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import ux.informer.DrawingInfoSupplier;
import ux.informer.DrawingInformer;
import ux.panel.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

public class PatternDrawer {

    // without this precision on the MathContext, small imprecision propagates at
    // large levels on the LifeUniverse - sometimes this will cause the image to jump around or completely
    // off the screen.  don't skimp on precision!
    private static final MathContext mc = new MathContext(100);
    private static final BigDecimal BigTWO = new BigDecimal(2);
    private static final Stack<CanvasState> previousStates = new Stack<>();
    private static final int DEFAULT_CELL_WIDTH = 4;
    final float cellBorderWidthRatio = .05F;
    private final DrawingInfoSupplier drawingInformer;
    // for all conversions, this needed to be a number larger than 5 for
    // there not to be rounding errors
    // which caused cells to appear outside the bounds
    private final PApplet processing;
    private final Patterning patterning;
    private final DrawRateManager drawRateManager;
    private final HUDStringBuilder hudInfo;
    private final MovementHandler movementHandler;
    // ain't no way to do drawing without a singleton drawables manager
    private final DrawableManager drawables = DrawableManager.getInstance();

    private final KeyFactory keyFactory;

    float cellBorderWidth = 0.0F;
    UXThemeManager theme = UXThemeManager.getInstance();


    // lifeFormPosition is used because we now separate the drawing speed from the framerate
    // we may not draw an image every frame
    // if we haven't drawn an image, we still want to be able to move and drag
    // the image around so this allows us to keep track of the current position
    // for the lifeFormBuffer and move that buffer around regardless of whether we've drawn an image
    // whenever an image is drawn, ths PVector is reset to 0,0 to match the current image state
    // it's a nifty way to handle things - just follow lifeFormPosition through the code
    // to see what i'm talking about
    PVector lifeFormPosition = new PVector(0, 0);
    boolean drawing = false;
    private CellWidth cellWidth;
    private TextPanel countdownText;
    private TextPanel hudText;
    private PGraphics lifeFormBuffer;
    private PGraphics UXBuffer;
    private boolean drawBounds;
    private BigDecimal canvasOffsetX = BigDecimal.ZERO;
    private BigDecimal canvasOffsetY = BigDecimal.ZERO;
    // if we're going to be operating in BigDecimal then we keep these that way so
    // that calculations can be done without conversions until necessary
    private BigDecimal canvasWidth;
    private BigDecimal canvasHeight;

    // surprisingly cacheing the result of the half size calculation provides
    // a remarkable speed boost
    private final Map<BigDecimal, BigDecimal> halfSizeMap = new HashMap<>();

    // used for resize detection
    private int prevWidth, prevHeight;


    public PatternDrawer(PApplet pApplet,
                         DrawRateManager drawRateManager) {

        this.processing = pApplet;
        this.patterning = (Patterning) pApplet;

        this.drawRateManager = drawRateManager;
        this.UXBuffer = getBuffer();
        this.lifeFormBuffer = getBuffer();

        drawingInformer = new DrawingInformer(this::getUXBuffer, this::isWindowResized, this::isDrawing);
        // resize trackers
        prevWidth = pApplet.width;
        prevHeight = pApplet.height;

        this.cellWidth = new CellWidth(DEFAULT_CELL_WIDTH);
        this.canvasWidth = BigDecimal.valueOf(pApplet.width);
        this.canvasHeight = BigDecimal.valueOf(pApplet.height);

        this.movementHandler = new MovementHandler(this);
        this.drawBounds = false;
        this.hudInfo = new HUDStringBuilder();

        TextPanel startupText = new TextPanel.Builder(drawingInformer, theme.getStartupText(), AlignHorizontal.RIGHT, AlignVertical.TOP)
                .textSize(theme.getStartupTextSize())
                .fadeInDuration(theme.getStartupTextFadeInDuration())
                .fadeOutDuration(theme.getStartupTextFadeOutDuration())
                .displayDuration(theme.getStartupTextDisplayDuration())
                .build();
        drawables.add(startupText);

        this.keyFactory = new KeyFactory(patterning, this);
        setupControls();
    }

    private PGraphics getBuffer() {
        return processing.createGraphics(processing.width, processing.height);
    }

    private PGraphics getUXBuffer() {
        return UXBuffer;
    }

    private boolean isWindowResized() {
        return prevWidth != processing.width || prevHeight != processing.height;
    }

    private boolean isDrawing() {
        return drawing;
    }

    private void setupControls() {

        // all callbacks have to invoke work - either on the Patterning or PatternDrawer
        // so give'em what they need
        keyFactory.setupKeyHandler();
        ControlPanel panelLeft, panelTop, panelRight;
        int transitionDuration = UXThemeManager.getInstance().getControlPanelTransitionDuration();
        panelLeft = new ControlPanel.Builder(drawingInformer, AlignHorizontal.LEFT, AlignVertical.CENTER)
                .transition(Transition.TransitionDirection.RIGHT, Transition.TransitionType.SLIDE, transitionDuration)
                .setOrientation(Orientation.VERTICAL)
                .addControl("zoomIn.png", keyFactory.callbackZoomInCenter)
                .addControl("zoomOut.png", keyFactory.callbackZoomOutCenter)
                .addControl("fitToScreen.png", keyFactory.callbackFitUniverseOnScreen)
                .addControl("center.png", keyFactory.callbackCenterView)
                .addControl("undo.png", keyFactory.callbackUndoMovement)
                .build();

        panelTop = new ControlPanel.Builder(drawingInformer, AlignHorizontal.CENTER, AlignVertical.TOP)
                .transition(Transition.TransitionDirection.DOWN, Transition.TransitionType.SLIDE, transitionDuration)
                .setOrientation(Orientation.HORIZONTAL)
                .addControl("random.png", keyFactory.callbackRandomLife)
                .addControl("stepSlower.png", keyFactory.callbackStepSlower)
                .addControl("drawSlower.png", keyFactory.callbackDrawSlower)
                .addToggleIconControl("pause.png", "play.png", keyFactory.callbackPause, keyFactory.callbackSingleStep)
                .addControl("drawFaster.png", keyFactory.callbackDrawFaster)
                .addControl("stepFaster.png", keyFactory.callbackStepFaster)
                .addControl("rewind.png", keyFactory.callbackRewind)
                .build();

        panelRight = new ControlPanel.Builder(drawingInformer, AlignHorizontal.RIGHT, AlignVertical.CENTER)
                .transition(Transition.TransitionDirection.LEFT, Transition.TransitionType.SLIDE, transitionDuration)
                .setOrientation(Orientation.VERTICAL)
                .addToggleHighlightControl("boundary.png", keyFactory.callbackDisplayBounds)
                .addToggleHighlightControl("darkmode.png", keyFactory.callbackThemeToggle)
                .addToggleHighlightControl("singleStep.png", keyFactory.callbackSingleStep)
                .build();

        List<ControlPanel> panels = Arrays.asList(panelLeft, panelTop, panelRight);

        MouseEventManager.getInstance().addAll(panels);

        drawables.addAll(panels);


    }

    public void toggleDrawBounds() {
        drawBounds = !drawBounds;
    }

    private BigDecimal calcCenterOnResize(BigDecimal dimension, BigDecimal offset) {
        return (dimension.divide(BigTWO, mc)).subtract(offset);
    }

    public void setupNewLife(LifeUniverse life) {

        Bounds bounds = life.getRootBounds();

        center(bounds, true, false);

        if (drawables.isManaging(countdownText)) {
            drawables.remove(countdownText);
        }

        // todo: on maximum volatility gun, not clearing the previousStates when doing a setStep seems to cause it to freak out - see if that's causal

        // clear image cache and previous states
        clearCache();

        countdownText = new TextPanel.Builder(drawingInformer, "counting down - press space to begin immediately", AlignHorizontal.CENTER, AlignVertical.CENTER)
                .runMethod(patterning::run)
                .fadeInDuration(2000)
                .countdownFrom(3)
                .textWidth(Optional.of(() -> canvasWidth.intValue() / 2))
                .wrap()
                .textSize(24)
                .build();
        drawables.add(countdownText);

        if (null != hudText) {
            drawables.remove(hudText);
            System.out.println(String.join("\n", Arrays.toString(Thread.currentThread().getStackTrace()).split(", ")));
        }

        hudText = new TextPanel.Builder(drawingInformer, getHUDMessage(life, bounds), AlignHorizontal.RIGHT, AlignVertical.BOTTOM)
                .textSize(24)
                .textWidth(Optional.of(() -> canvasWidth.intValue()))
                .build();
        drawables.add(hudText);
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

    private String getHUDMessage(LifeUniverse life, Bounds bounds) {
        Node root = life.root;

        hudInfo.addOrUpdate("fps", Math.round(processing.frameRate));
        hudInfo.addOrUpdate("dps", Math.round(drawRateManager.getCurrentDrawRate()));
        hudInfo.addOrUpdate("cell", getCellWidth());
        hudInfo.addOrUpdate("running", (patterning.isRunning()) ? "running" : "stopped");

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

    // called when moves are invoked as there is some trickery in the move handler to move
    // multiple times on key presses and even more so as they are held down
    // we just want to go back to the first one...
    public void saveUndoState() {
        previousStates.push(new CanvasState(cellWidth, canvasOffsetX, canvasOffsetY));
    }

    public float getCellWidth() {
        return cellWidth.get();
    }

    public void handlePause() {

        if (drawables.isManaging(countdownText)) {
            countdownText.interruptCountdown();
            keyFactory.callbackPause.notifyKeyObservers();
        } else {
            patterning.toggleRun();
        }
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

    private void fillSquare(float x, float y, float size) {


        float width = size - cellBorderWidth;

        UXBuffer.pushStyle();
        lifeFormBuffer.fill(theme.getCellColor());
        lifeFormBuffer.noStroke();
        lifeFormBuffer.rect(x, y, width, width);
        UXBuffer.popStyle();

/*        // todo, create a keyboard handler to display these values
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
        }*/

    }

    private void drawNode(Node node, BigDecimal size, BigDecimal left, BigDecimal top) {

        if (node.population.equals(BigInteger.ZERO)) {
            return;
        }

        BigDecimal leftWithOffset = left.add(canvasOffsetX);
        BigDecimal topWithOffset = top.add(canvasOffsetY);
        BigDecimal leftWithOffsetAndSize = leftWithOffset.add(size);
        BigDecimal topWithOffsetAndSize = topWithOffset.add(size);

        // no need to draw anything not visible on screen
        if (leftWithOffsetAndSize.compareTo(BigDecimal.ZERO) < 0
                || topWithOffsetAndSize.compareTo(BigDecimal.ZERO) < 0
                || leftWithOffset.compareTo(canvasWidth) >= 0
                || topWithOffset.compareTo(canvasHeight) >= 0) {
            return;
        }

        // if we have done a recursion down to a very small size and the population exists,
        // draw a unit square and be done
        if (size.compareTo(BigDecimal.ONE) <= 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                fillSquare(Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()),
                        1);
            }
        } else if (node.level == 0) {
            if (node.population.equals(BigInteger.ONE)) {
                fillSquare(Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()),
                        cellWidth.get());
            }
        } else {

            BigDecimal halfSize = getHalfSize(size);
            BigDecimal leftHalfSize = left.add(halfSize);
            BigDecimal topHalfSize = top.add(halfSize);

            drawNode(node.nw, halfSize, left, top);
            drawNode(node.ne, halfSize, leftHalfSize, top);
            drawNode(node.sw, halfSize, left, topHalfSize);
            drawNode(node.se, halfSize, leftHalfSize, topHalfSize);

        }
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

    public void draw(LifeUniverse life, boolean shouldDraw) {

        // lambdas are interested in this fact
        drawing = true;

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


        // make this threadsafe
        Node node = life.root;

        long start = System.nanoTime();
        Bounds bounds = life.getRootBounds();
        long end = System.nanoTime();

        movementHandler.handleRequestedMovement();

        float duration = (end - start) / 1000000f;

        cellBorderWidth = (cellBorderWidthRatio * cellWidth.get());


        // make this threadsafe
        String hudMessage = getHUDMessage(life, bounds);
        hudText.setMessage(hudMessage);

        drawables.drawAll(UXBuffer);

        UXBuffer.endDraw();

        if (shouldDraw) {
            lifeFormBuffer.beginDraw();
            lifeFormBuffer.clear();
            BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1), mc).multiply(cellWidth.getAsBigDecimal(), mc);
            drawNode(node, size.multiply(BigTWO, mc), size.negate(), size.negate());
            drawBounds(bounds);
            lifeFormBuffer.endDraw();
            // reset the position in case you've had mouse moves
            lifeFormPosition.set(0, 0);
        }

        processing.image(lifeFormBuffer, lifeFormPosition.x, lifeFormPosition.y);
        processing.image(UXBuffer, 0, 0);



        drawing = false;
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

    // re-using these really seems to make a difference
    private BigDecimal getHalfSize(BigDecimal size) {
        if (!halfSizeMap.containsKey(size)) {
            BigDecimal halfSize = size.divide(BigTWO, mc);
            halfSizeMap.put(size, halfSize);
        }
        return halfSizeMap.get(size);
    }
}