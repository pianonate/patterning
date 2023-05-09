import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;

import java.awt.*;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private float cell_width;
    float border_width_ratio = 0;

    private final int lifeDrawingBorder;

    private long drawNodeRecursions;
    private long lastDrawNodeRecursion;

    private ImageCache imageCache;

    LifeDrawer(PApplet p, float cell_width) {
        this.p = p;
        this.setCell_width(cell_width);
        this.canvas_width = p.width;
        this.canvas_height = p.height;
        this.lifeDrawingBorder = 50;
        drawNodeRecursions = 0;
        this.imageCache = new ImageCache(p);
    }

    public void clearCache() {
        this.imageCache.clearCache();
    }

    public float getCell_width() {
        return cell_width;
    }

    public void setCell_width(float cell_width) {
        // nearest power of two so that integer math can more easily work
        int power = Math.round((float) (Math.log(cell_width) / Math.log(2)));
        this.cell_width =  (float) Math.pow(2, power);
    }


    class ImageCache {
        private final PApplet pApplet;
        private long hits;
        private long misses;
        private final int cacheSize = 8000; // Set your desired cache size
        private final Map<Node, ImageCacheEntry> cache = new LRUCache<>(cacheSize);
        private boolean removeEldestMode = false;

        public ImageCache(PApplet pApplet) {
            this.pApplet = pApplet;
            this.hits = 0;
            this.misses = 0;
        }

        public void clearCache() {
            cache.clear();
            removeEldestMode = false;
        }

        private class ImageCacheEntry {
            private PImage image;

            private boolean cached;

            private long retrievalCount;

            private int combinedNodeCount;

            private ImageCacheEntry nw, ne, sw, se;

            public ImageCacheEntry(PImage image) {
                this(image, null, null, null, null);
            }

            public ImageCacheEntry(PImage image, ImageCacheEntry nw, ImageCacheEntry ne, ImageCacheEntry sw, ImageCacheEntry se) {
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
                if (nw != null) total += nw.getTotalRetrievalCount();
                if (ne != null) total += ne.getTotalRetrievalCount();
                if (sw != null) total += sw.getTotalRetrievalCount();
                if (se != null) total += se.getTotalRetrievalCount();
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

        //todo: return from the cache an object that
        //      if it was cached and
        //      how many times this node representation- was retrieved from the cache
        //
        // LRUCache class
        private class LRUCache<K, V> extends LinkedHashMap<K, ImageCacheEntry> {
            private final int cacheSize;

            public LRUCache(int cacheSize) {
                super(cacheSize + 1, 1.0f, true); // Set accessOrder to true for LRU behavior
                this.cacheSize = cacheSize;
            }

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, ImageCacheEntry> eldest) {
                boolean removeEldest = size() > cacheSize;

                if (removeEldest ) {
                    if (!removeEldestMode) {
                        System.out.println("Remove eldest mode at size(): " + size());
                        removeEldestMode = true;
                    }

                    ImageCacheEntry entry = (ImageCacheEntry) eldest.getValue();
                    PImage imageToDispose = entry.getImage();
                     ((PGraphics) imageToDispose).dispose();
                }

                return removeEldest;
            }

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
                        swEntry.getImage(), seEntry.getImage()
                );

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


        private PImage createBinaryBitArrayImage(boolean[][] binaryBitArray) {
            int rows = binaryBitArray.length;
            int cols = rows; // it is squares here...

            PGraphics img = pApplet.createGraphics(rows, cols);

            img.beginDraw();
            img.background(255, 255, 255, 0); // Transparent background
            img.noStroke();
            img.fill(cell_color);

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    if (binaryBitArray[y][x]) {
                        img.rect(x, y, 1, 1);
                    }
                }
            }

            img.endDraw();
            return img;
        }
    }


    void fill_square(PGraphics buffer, float x, float y, float size) {

        float width = size - border_width;
        buffer.noStroke();
        // p.fill(cell_color);
        buffer.rect(x, y, width, width);

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
        float previousCellWidth = getCell_width();

        // Adjust cell width to align with grid
        if (in) {
            setCell_width(getCell_width() * 2f);
        } else {
            setCell_width(getCell_width() / 2f);
        }

        // Apply rounding conditionally based on a threshold
        float threshold = 4.0f; // You can adjust this value based on your requirements
        if (getCell_width() >= threshold) {
            setCell_width(Math.round(getCell_width()));
        }

        // Calculate zoom factor
        float zoomFactor = (float) getCell_width() / previousCellWidth;

        // Calculate the difference in canvas offset-s before and after zoom
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

        BigDecimal drawingWidth = patternWidth.multiply(BigDecimal.valueOf(getCell_width()));
        BigDecimal drawingHeight = patternHeight.multiply(BigDecimal.valueOf(getCell_width()));

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

        // setCell_width(newCellSize.floatValue());
        setCell_width(newCellSize.floatValue());

        BigDecimal drawingWidth = patternWidth.multiply(newCellSize);
        BigDecimal drawingHeight = patternHeight.multiply(newCellSize);

        BigDecimal offsetX = canvasWidthWithBorder.subtract(drawingWidth).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));
        BigDecimal offsetY = canvasHeightWithBorder.subtract(drawingHeight).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(lifeDrawingBorder));


        // i think given the offs can be calculated against a bounding box that is larger than a float
        // you can'tx use the current updateCanvasOffsets (although you could make a version to pass in these BigDecimals)
        // first make sure you have one.
        canvas_offset_x = offsetX.setScale(0, RoundingMode.HALF_UP).toBigInteger();
        canvas_offset_y = offsetY.setScale(0, RoundingMode.HALF_UP).toBigInteger();

        center_view(bounds);



    }

    void draw_bounds(Bounds bounds, PGraphics offscreenBuffer) {

        Bounds screenBounds = bounds.getScreenBounds(getCell_width(), canvas_offset_x, canvas_offset_y);

        offscreenBuffer.noFill();
        offscreenBuffer.stroke(200);
        offscreenBuffer.strokeWeight(1);
        offscreenBuffer.rect(screenBounds.leftToFloat(), screenBounds.topToFloat(), screenBounds.rightToFloat(), screenBounds.bottomToFloat());
    }

    // thi work - the cell width times 2 ^ level will give you the size of the whole universe
    // draw_node will draw whatever is visible of it that you want
    void redraw(Node node, PGraphics offscreenBuffer) {
        border_width = (int) (border_width_ratio * getCell_width());
        drawNodeRecursions = 0;

        BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1), mc).multiply(BigDecimal.valueOf(getCell_width()), mc);

        DrawNodeContext ctx = new DrawNodeContext(offscreenBuffer);
        draw_node(node, size.multiply(BigDecimal.valueOf(2), mc), size.negate(), size.negate(), ctx);

       //  draw_node(node, size, size.negate(), size.negate(), ctx);
        // draw_node(node, ctx);

       /* if (lastDrawNodeRecursion != drawNodeRecursions) {
            System.out.println("drawNode Recursions: " + NumberFormat.getInstance().format(drawNodeRecursions)
                    + " changed: " + ctx.changedDrawCount
                    + " unchanged: " + ctx.unchangedDrawCount
            );
        }
        */
        lastDrawNodeRecursion = drawNodeRecursions;

    }

    class DrawNodeContext {

        public final BigDecimal canvasWidthDecimal;
        public final BigDecimal canvasHeightDecimal;
        public final BigDecimal canvasOffsetXDecimal;
        public final BigDecimal canvasOffsetYDecimal;

        private final Map<BigDecimal, BigDecimal> halfSizeMap = new HashMap<>();

        public int changedDrawCount;
        public int unchangedDrawCount;

        PGraphics buffer;


        public DrawNodeContext(PGraphics offscreenBuffer) {
            this.canvasWidthDecimal = BigDecimal.valueOf(canvas_width);
            this.canvasHeightDecimal = BigDecimal.valueOf(canvas_height);
            this.canvasOffsetXDecimal = new BigDecimal(canvas_offset_x);
            this.canvasOffsetYDecimal = new BigDecimal(canvas_offset_y);
            this.buffer = offscreenBuffer;
        }

        public BigDecimal getHalfSize(BigDecimal size) {
            if (!halfSizeMap.containsKey(size)) {
                BigDecimal halfSize = size.divide(BigDecimal.valueOf(2), mc);
                halfSizeMap.put(size, halfSize);
            }
            return halfSizeMap.get(size);
        }

    }



   private void draw_node(Node node, BigDecimal size, BigDecimal left, BigDecimal top, DrawNodeContext ctx) {

        drawNodeRecursions++;

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

       //System.out.println("Node level: " + node.level + " Left: " + left + " Top: " + top + " Size: " + size);

        // if we've recursed down to a very small size and the population exists,
        // draw a unit square and be done
        if (size.compareTo(BigDecimal.ONE) <= 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                // System.out.println("node to small to fit - level: " + node.level + " left: " + left + ", top: " + top);

                ctx.buffer.fill(cell_color);
                fill_square(ctx.buffer, Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()), 1);
            }
        } else if (node.level == 0) {
            if (node.population.equals(BigInteger.ONE)) {
               //  System.out.println("Leaf Node 0, left: " + left + ", top: " + top);

                ctx.buffer.fill(cell_color);
                fill_square(ctx.buffer, Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()), getCell_width());
            }
        } else {

            if (node.hasChanged()) {
                ctx.changedDrawCount++;
            } else {
                ctx.unchangedDrawCount++;
            }

            if (!node.hasChanged() && node.level <= 4 // not new nodes
                    && Math.pow(2, node.level) * getCell_width() <= Math.min(canvas_width, canvas_height) // can fit on screen
                    && (getCell_width() > Math.pow(2,-3)) // will work when cell_widths are small
                  // && (node.level < 8 )// todo: just as an insurance policy for now...
            )
            {

                ImageCache.ImageCacheEntry entry = imageCache.getImageCacheEntry(node);
                PImage cachedImage = entry.getImage();

                ctx.buffer.image(cachedImage,
                        Math.round(leftWithOffset.floatValue()),
                        Math.round(topWithOffset.floatValue()),
                        Math.round(cachedImage.width * getCell_width()),
                        Math.round(cachedImage.width * getCell_width()));

                /* p.fill(node.level * 20, 255 - node.level * 20, 0);
                p.rect(Math.round(leftWithOffset.floatValue()),
                        Math.round(topWithOffset.floatValue()),
                        Math.round(size.floatValue() * getCell_width()),
                        Math.round(size.floatValue() * getCell_width()));*/

                /*
                if (( getCell_width() * Math.pow(2, node.level) ) > 16 ) {

                    p.fill(0, 125); // Black color with alpha value of 178

                    // Draw text with 70% opacity
                    p.textSize(16);
                    p.textAlign(p.LEFT, p.TOP);
                    String cacheString = (entry.cached) ? "cache" : "nocache";

                    p.text("id: " + node.id + " node.level:" + node.level + " - " + cacheString, Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()));
                    p.text("retrievals: " + entry.getRetrievalCount(),Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue())+20);
                    p.text("nodes: " + entry.combinedNodeCount(), Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue())+40);
                    p.text("total: " + entry.getTotalRetrievalCount(), Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue())+60);

                }
                p.fill(0, 5); // Black color with alpha value of 25
                p.stroke(1);

                p.rect(Math.round(leftWithOffset.floatValue()),
                        Math.round(topWithOffset.floatValue()),
                        Math.round(cachedImage.width * getCell_width()),
                        Math.round(cachedImage.width * getCell_width())); */


            } else {

                BigDecimal halfSize = ctx.getHalfSize(size);
                BigDecimal leftHalfSize = left.add(halfSize);
                BigDecimal topHalfSize = top.add(halfSize);

                draw_node(node.nw, halfSize, left, top, ctx);
                draw_node(node.ne, halfSize, leftHalfSize, top, ctx);
                draw_node(node.sw, halfSize, left, topHalfSize, ctx);
                draw_node(node.se, halfSize, leftHalfSize, topHalfSize, ctx);
            }
        }
    }

    /*void draw_cell(int x, int y, boolean set) {
        // todo: something is happening when you get to a step size at 1024
        //       you can go that large and because you are using a math context to do the division
        //       everything seems to work but at step size of 1024, the drawing starts to go wonky
        //       so can you... maybe keep everything in BigDecimal until you convert it somehow?
        //       the initial size passed into draw_cell is the largest possible size of the drawing
        //       based on the level - but that's so large it can't possibly matter.  is there a way
        //       to just keep track of the part of the drawing that is on screen and ask
        //       the lifeUniverse to only give you that much of it without having to use all this recursion?
        //       seems inefficient
        BigDecimal biCellWidth = new BigDecimal(getCell_width());
        BigDecimal biX = new BigDecimal(x).multiply(biCellWidth).add(new BigDecimal(canvas_offset_x));
        BigDecimal biY = new BigDecimal(y).multiply(biCellWidth).add(new BigDecimal(canvas_offset_y));
        float width = (float) Math.ceil(getCell_width()) - (int) (getCell_width() * border_width_ratio);

        // todo: don't forget to use offscreenBuffer
        if (set) {
            p.fill(cell_color);
        } else {
            p.fill(background_color);
        }

        p.noStroke();
        p.rect(biX.floatValue(), biY.floatValue(), width, width);
    } */
}