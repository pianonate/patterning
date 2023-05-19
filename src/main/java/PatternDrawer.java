import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

class PatternDrawer {

    private static final BigDecimal BigTWO = new BigDecimal(2);
    private static final Stack<CanvasState> previousStates = new Stack<>();
    private static final int DEFAULT_CELL_WIDTH = 4;

    private static class CellWidth {
        private static final float ONE_PIXEL_THRESHOLD = 1.0f;
        private static final float ONE_DECIMAL_PLACE = 10.0F;
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
            if (cellWidth >= ONE_PIXEL_THRESHOLD) {
                cellWidth = Math.round(cellWidth * ONE_DECIMAL_PLACE) / ONE_DECIMAL_PLACE;
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

    private final PApplet processing;
    private final ImageCache imageCache;

    private final HUDStringBuilder hudInfo;
    private final MovementHandler movementHandler;

    private final List<TextDisplay> textDisplays = new ArrayList<>();
    private final TextDisplay countdownText;
    private final TextDisplay hudText;


    // for all conversions, this needed to be a number larger than 5 for
    // there not to be rounding errors
    // which caused cells to appear outside the bounds
    private static final MathContext mc = new MathContext(10);

    private PGraphics newBuffer;

    private boolean drawBounds;

    // this class operates on BigDecimals because the bounds of a drawing can be arbitrarily large

    private BigDecimal canvasOffsetX = BigDecimal.ZERO;
    private BigDecimal canvasOffsetY = BigDecimal.ZERO;

    // if we're going to be operating in BigDecimal then we keep these that way so
    // that calculations can be done without conversions until necessary
    private BigDecimal canvasWidth;
    private BigDecimal canvasHeight;

    // todo: implement an actual border width so you can see space between cells
    int cellBorderWidth;
    final int cellColor = 255;
    private CellWidth cellWidth;
    final float cellBorderWidthRatio = 0;


    PatternDrawer(PApplet pApplet) {
        this.processing = pApplet;
        this.cellWidth = new CellWidth(DEFAULT_CELL_WIDTH);
        this.canvasWidth = BigDecimal.valueOf(pApplet.width);
        this.canvasHeight = BigDecimal.valueOf(pApplet.height);
        this.imageCache = new ImageCache(pApplet);
        this.newBuffer = pApplet.createGraphics(pApplet.width, pApplet.height);
        this.movementHandler = new MovementHandler(this);
        this.drawBounds = false;
        this.hudInfo = new HUDStringBuilder();

        countdownText = new TextDisplay.Builder("counting down - press space to begin immediately", TextDisplay.Position.CENTER)
                .runMethod(Patterning::run)
                .countdownFrom(3)
                .build();

        TextDisplay startupText = new TextDisplay.Builder("Welcome to Patterning", TextDisplay.Position.TOP_LEFT)
                .textSize(60)
                .fadeInDuration(1000)
                .duration(5000)
                .build();

        hudText = new TextDisplay.Builder("HUD", TextDisplay.Position.BOTTOM_RIGHT)
                .textSize(24)
                .build();

        textDisplays.add(countdownText);
        textDisplays.add(startupText);
        textDisplays.add(hudText);

        startupText.startDisplay();
        hudText.startDisplay();

    }

    public void toggleDrawBounds() {
        drawBounds = !drawBounds;
    }

    private BigDecimal calcCenterOnResize(BigDecimal dimension, BigDecimal offset) {
        return (dimension.divide(BigTWO, mc)).subtract(offset);
    }

    public void displayBufferedImage() {
        processing.image(newBuffer, 0, 0);
    }

    public void setupNewLife(Bounds bounds) {

        center(bounds, true, false);

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

    // formerly known as setSize()
    // make sure that when you resize it tries to show you the contents of the last
    // screen size
    // without updating the cell size - by getting the center before and then
    // centering
    // around the center after
    @SuppressWarnings("SuspiciousNameCombination")
    public void surfaceResized(int newWidth, int newHeight) {

        BigDecimal bigWidth = BigDecimal.valueOf(newWidth);
        BigDecimal bigHeight = BigDecimal.valueOf(newHeight);

        if (bigHeight.equals(canvasHeight) && bigWidth.equals(canvasWidth) ) {
            return;
        }

        newBuffer = processing.createGraphics(newWidth, newHeight);

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

    public float getCellWidth() {
        return cellWidth.get();
    }

    // you probably don't need this nonsense after all...
    public void clearCache() {
        imageCache.clearCache();
        previousStates.clear();
    }

    public void zoom(boolean in, float x, float y) {

        previousStates.push(new CanvasState(cellWidth, canvasOffsetX, canvasOffsetY));

        float previousCellWidth = cellWidth.get();

        // Adjust cell width to align with grid
        if (in) {
            cellWidth.set(cellWidth.get() * 2f);
        } else {
            cellWidth.set(cellWidth.get() / 2f);
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
        newBuffer.noStroke();
        newBuffer.rect(x, y, width, width);

        // todo, create a keyboard handler to display these values
        //       or a mouse mode - it can be useful when debugging positioning information
        // you'll be glad you did
        if( false) {
            newBuffer.fill(0xff000000);
            newBuffer.textSize(11);
            newBuffer.textAlign(processing.LEFT, processing.TOP);
            newBuffer.text((int) x + ",  " + (int) y, x, y, width, width);
            newBuffer.text(canvasWidth.toString() + ",  " + canvasHeight.toString(), x, y + 12, width, width);

            newBuffer.text("size: " + size, x, y + 24, width, width);

            int nextPos = (int) x + (int) size;
            newBuffer.text("next: " + nextPos, x, y + 36, width, width);
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
                newBuffer.fill(cellColor);
                fillSquare(Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()),
                        1);
            }
        } else if (node.level == 0) {
            if (node.population.equals(BigInteger.ONE)) {
                newBuffer.fill(cellColor);
                fillSquare(Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()),
                        cellWidth.get());
            }
        } else {

            BigDecimal powResult = BigDecimal.valueOf(2).pow(node.level).multiply(cellWidth.getAsBigDecimal());
            BigDecimal minCanvasDimension = canvasWidth.min(canvasHeight);

            boolean fitsOnScreen = (powResult.compareTo(minCanvasDimension) <= 0);
            boolean useImageCache = false;
            boolean bypassRecursion = useImageCache
                    && !node.hasChanged()
                    && (node.level <= DEFAULT_CELL_WIDTH) // not new nodes
                    && fitsOnScreen
                    && (cellWidth.get() > Math.pow(2, -3)); // will work when cell_widths are small

            if (bypassRecursion) {

                ImageCache.ImageCacheEntry entry = imageCache.getImageCacheEntry(node);
                PImage cachedImage = entry.getImage();

                newBuffer.image(cachedImage,
                        Math.round(leftWithOffset.floatValue()),
                        Math.round(topWithOffset.floatValue()),
                        Math.round(cachedImage.width * cellWidth.get()),
                        Math.round(cachedImage.width * cellWidth.get()));

                boolean debugging = false;
                if (debugging) {

                    if ((cellWidth.get() * Math.pow(2, node.level)) > 16) {

                        newBuffer.fill(0, 125); // Black color with alpha value of 178

                        // Draw text with 70% opacity
                        newBuffer.textSize(16);
                        newBuffer.textAlign(PConstants.LEFT, PConstants.TOP);
                        String cacheString = (entry.isCached()) ? "cache" : "nocache";

                        newBuffer.text("id: " + node.id + " node.level:" + node.level + " - " + cacheString,
                                Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()));
                        newBuffer.text("retrievals: " + entry.getRetrievalCount(), Math.round(leftWithOffset.floatValue()),
                                Math.round(topWithOffset.floatValue()) + 20);
                        newBuffer.text("nodes: " + entry.combinedNodeCount(), Math.round(leftWithOffset.floatValue()),
                                Math.round(topWithOffset.floatValue()) + 40);
                        newBuffer.text("total: " + entry.getTotalRetrievalCount(), Math.round(leftWithOffset.floatValue()),
                                Math.round(topWithOffset.floatValue()) + 60);

                    }

                    newBuffer.fill(0, 5); // Black color with alpha value of 25
                    newBuffer.stroke(1);

                    newBuffer.rect(Math.round(leftWithOffset.floatValue()),
                            Math.round(topWithOffset.floatValue()),
                            Math.round(cachedImage.width * cellWidth.get()),
                            Math.round(cachedImage.width * cellWidth.get()));
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
    }

    // called when moves are invoked as there is some trickery in the move handler to move
    // multiple times on key presses and even more so as they are held down
    // we just want to go back to the first one...
    public void saveUndoState() {
        previousStates.push(new CanvasState(cellWidth, canvasOffsetX, canvasOffsetY));
    }

    public void move(float dx, float dy) {
        updateCanvasOffsets(BigDecimal.valueOf(dx), BigDecimal.valueOf(dy));
    }

    private record CanvasState(CellWidth cellWidth, BigDecimal canvasOffsetX, BigDecimal canvasOffsetY) {
            private CanvasState(CellWidth cellWidth, BigDecimal canvasOffsetX, BigDecimal canvasOffsetY) {
                this.cellWidth = new CellWidth(cellWidth.get());
                this.canvasOffsetX = canvasOffsetX;
                this.canvasOffsetY = canvasOffsetY;
            }
        }

    private void updateCanvasOffsets(BigDecimal offsetX, BigDecimal offsetY) {
        canvasOffsetX = canvasOffsetX.add(offsetX);
        canvasOffsetY = canvasOffsetY.add(offsetY);
    }

    void zoomXY(boolean in, float mouse_x, float mouse_y) {
        zoom(in, mouse_x, mouse_y);
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

        newBuffer.noFill();
        newBuffer.stroke(200);
        newBuffer.strokeWeight(1);
        newBuffer.rect(screenBounds.leftToFloat(), screenBounds.topToFloat(), screenBounds.rightToFloat(),
                screenBounds.bottomToFloat());
    }

    // the cell width times 2 ^ level will give you the size of the whole universe
    // draws the screen size viewport on the universe

    public void draw(LifeUniverse life) {

        newBuffer.beginDraw();
        newBuffer.background(0);

        Node node = life.root;
        Bounds bounds = life.getRootBounds();

        movementHandler.handleRequestedMovement();

        cellBorderWidth = (int) (cellBorderWidthRatio * cellWidth.get());

        BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1), mc).multiply(cellWidth.getAsBigDecimal(), mc);

        DrawNodeContext ctx = new DrawNodeContext();
        drawNode(node, size.multiply(BigTWO, mc), size.negate(), size.negate(), ctx);

        drawBounds(bounds);


        String hudMessage = getHUDMessage(life, bounds);
        hudText.setMessage(hudMessage);

        //drawHUD(life, bounds);

        for (TextDisplay display : textDisplays) {
            display.draw(newBuffer);
        }

        newBuffer.endDraw();

        displayBufferedImage();


    }

    private String getHUDMessage(LifeUniverse life, Bounds bounds) {
        Node root = life.root;

        hudInfo.addOrUpdate("fps", Math.round(processing.frameRate));
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

        newBuffer.textAlign(PApplet.RIGHT, PApplet.BOTTOM);
        // use the default delimiter
        return hudInfo.getFormattedString(processing.frameCount, 12);
    }

    private void drawHUD(LifeUniverse life, Bounds bounds) {

        String hud = getHUDMessage(life, bounds);

        newBuffer.fill(255);
        float hudTextSize = 24;
        newBuffer.textSize(hudTextSize);

        float hudMargin = 10;

        while ((newBuffer.textWidth(hud) + (2 * hudMargin) > newBuffer.width)
                || ((newBuffer.textAscent() + newBuffer.textDescent()) + (2 * hudMargin) > newBuffer.height)) {
            hudTextSize--;
            hudTextSize =  Math.max(hudTextSize, 1); // Prevent the textSize from going below 1
            newBuffer.textSize(hudTextSize);
        }

        newBuffer.text(hud, newBuffer.width - hudMargin, newBuffer.height - 5);

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

    class ImageCache {
        private final PApplet pApplet;
        private final int cacheSize = 8000; // Set your desired cache size
        private final Map<Node, ImageCacheEntry> cache = new LRUCache<>(cacheSize);
        private boolean removeEldestMode = false;

        public ImageCache(PApplet pApplet) {
            this.pApplet = pApplet;

        }

        public void clearCache() {
            cache.clear();
            removeEldestMode = false;
        }

        private PImage createBinaryBitArrayImage(boolean[][] binaryBitArray) {
            int rows = binaryBitArray.length;

            PGraphics img = pApplet.createGraphics(rows, rows);

            img.beginDraw();
            img.background(255, 255, 255, 0); // Transparent background
            img.noStroke();
            img.fill(cellColor);

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < rows; x++) {
                    if (binaryBitArray[y][x]) {
                        img.rect(x, y, 1, 1);
                    }
                }
            }

            img.endDraw();
            return img;
        }

        public ImageCacheEntry getImageCacheEntry(Node node) {

            if (cache.containsKey(node)) {
                ImageCacheEntry entry = cache.get(node);
                entry.incrementRetrievalCount();
                return entry;
            }

            if (node.level > 0) {
                // Retrieve or generate ImageCacheEntries for child nodes
                ImageCacheEntry nwEntry = getImageCacheEntry(node.nw);
                ImageCacheEntry neEntry = getImageCacheEntry(node.ne);
                ImageCacheEntry swEntry = getImageCacheEntry(node.sw);
                ImageCacheEntry seEntry = getImageCacheEntry(node.se);

                // Combine child images into a single image for the current node
                PImage combinedImage = combineChildImages(
                        nwEntry.getImage(), neEntry.getImage(),
                        swEntry.getImage(), seEntry.getImage());

                // Create a new ImageCacheEntry for the combined image
                ImageCacheEntry combinedEntry = new ImageCacheEntry(combinedImage, nwEntry, neEntry, swEntry, seEntry);
                cache.put(node, combinedEntry);
                return combinedEntry;

            } else { // leaf node entry - should only have to do this a few times
                boolean[][] binaryBitArray = node.getBinaryBitArray();
                PImage img = createBinaryBitArrayImage(binaryBitArray);
                ImageCacheEntry entry = new ImageCacheEntry(img);
                cache.put(node, entry);
                return entry;
            }
        }

        private PImage combineChildImages(PImage nwImage, PImage neImage, PImage swImage, PImage seImage) {
            int childSize = nwImage.width;
            int combinedSize = childSize * 2;

            PGraphics combinedImage = pApplet.createGraphics(combinedSize, combinedSize);
            combinedImage.beginDraw();

            combinedImage.image(nwImage, 0, 0);
            combinedImage.image(neImage, childSize, 0);
            combinedImage.image(swImage, 0, childSize);
            combinedImage.image(seImage, childSize, childSize);

            combinedImage.endDraw();
            return combinedImage;
        }

        private class ImageCacheEntry {
            private final PImage image;
            private final ImageCacheEntry nw;
            private final ImageCacheEntry ne;
            private final ImageCacheEntry sw;
            private final ImageCacheEntry se;
            private boolean cached;
            private long retrievalCount;

            public ImageCacheEntry(PImage image) {
                this(image, null, null, null, null);
            }

            public ImageCacheEntry(PImage image, ImageCacheEntry nw, ImageCacheEntry ne, ImageCacheEntry sw,
                                   ImageCacheEntry se) {
                this.image = image;
                this.retrievalCount = 0;
                this.cached = false;
                this.nw = nw;
                this.ne = ne;
                this.sw = sw;
                this.se = se;
            }

            public PImage getImage() {
                return image;
            }

            public boolean isCached() {
                return cached;
            }

            public void incrementRetrievalCount() {
                retrievalCount++;
                cached = true;
            }

            public long getRetrievalCount() {
                return retrievalCount;
            }

            public long getTotalRetrievalCount() {
                long total = retrievalCount;
                if (nw != null)
                    total += nw.getTotalRetrievalCount();
                if (ne != null)
                    total += ne.getTotalRetrievalCount();
                if (sw != null)
                    total += sw.getTotalRetrievalCount();
                if (se != null)
                    total += se.getTotalRetrievalCount();
                return total;
            }

            public int combinedNodeCount() {
                int count = 1; // Counting itself

                // Counting child nodes, if they exist
                if (nw != null) {
                    count += nw.combinedNodeCount();
                }
                if (ne != null) {
                    count += ne.combinedNodeCount();
                }
                if (sw != null) {
                    count += sw.combinedNodeCount();
                }
                if (se != null) {
                    count += se.combinedNodeCount();
                }

                return count;
            }

        }

        // todo: return from the cache an object that
        // if it was cached and
        // how many times this node representation- was retrieved from the cache
        //
        // LRUCache class
        private class LRUCache<K> extends LinkedHashMap<K, ImageCacheEntry> {
            private final int cacheSize;

            public LRUCache(int cacheSize) {
                super(cacheSize + 1, 1.0f, true); // Set accessOrder to true for LRU behavior
                this.cacheSize = cacheSize;
            }

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, ImageCacheEntry> eldest) {
                boolean removeEldest = size() > cacheSize;

                if (removeEldest) {
                    if (!removeEldestMode) {
                        System.out.println("Remove eldest mode at size(): " + size());
                        removeEldestMode = true;
                    }

                    ImageCacheEntry entry = eldest.getValue();
                    PImage imageToDispose = entry.getImage();
                    ((PGraphics) imageToDispose).dispose();
                }

                return removeEldest;
            }

        }
    }
}