import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PGraphics;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


class LifeDrawer {

    boolean debugging = false;
    boolean useImageCache = false;

    PApplet p;
    BigInteger canvas_offset_x = BigInteger.ZERO;
    BigInteger canvas_offset_y = BigInteger.ZERO;

    MathContext mc = new MathContext(5);
    int canvas_width;
    int canvas_height;
    int border_width;
    int cell_color = 0;
    int background_color = 255;
    private float cell_width;
    float border_width_ratio = 0;


    private ImageCache imageCache;

    LifeDrawer(PApplet p, float cell_width) {
        this.p = p;
        this.setCellWidth(cell_width);
        this.canvas_width = p.width;
        this.canvas_height = p.height;
        this.imageCache = new ImageCache(p);
    }

    public void clearCache() {
        this.imageCache.clearCache();
    }

    public float getCellWidth() {
        return cell_width;
    }

    public void setCellWidth(float cell_width) {
        // nearest power of two so that integer math can more easily work
        //int power = Math.round((float) (Math.log(cell_width) / Math.log(2)));
        //this.cell_width =  (float) Math.pow(2, power);
        this.cell_width = cell_width;

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

        private class ImageCacheEntry {
            private PImage image;

            private boolean cached;

            private long retrievalCount;

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


    void fillSquare(PGraphics buffer, float x, float y, float size) {

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
        float previousCellWidth = getCellWidth();

        // Adjust cell width to align with grid
        if (in) {
            setCellWidth(getCellWidth() * 2f);
        } else {
            setCellWidth(getCellWidth() / 2f);
        }

        // Apply rounding conditionally based on a threshold
      /*  float threshold = 4.0f; // You can adjust this value based on your requirements
        if (getCell_width() >= threshold) {
            setCellWidth(Math.round(getCell_width()));
        } */

        // Calculate zoom factor
        float zoomFactor = (float) getCellWidth() / previousCellWidth;

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

    void zoomXY(boolean in, float mouse_x, float mouse_y) {
        zoom(in, mouse_x, mouse_y);
    }


    public void center(Bounds bounds, boolean fitBounds) {
        BigDecimal patternWidth = new BigDecimal(bounds.right.subtract(bounds.left));
        BigDecimal patternHeight = new BigDecimal(bounds.bottom.subtract(bounds.top));

        BigDecimal canvasWidth = new BigDecimal(canvas_width);
        BigDecimal canvasHeight = new BigDecimal(canvas_height);
    
        if (fitBounds) {
 
            BigDecimal widthRatio = (patternWidth.compareTo(BigDecimal.ZERO) > 0) ? canvasWidth.divide(patternWidth, mc) : BigDecimal.ONE;
            BigDecimal heightRatio = (patternHeight.compareTo(BigDecimal.ZERO) > 0) ? canvasHeight.divide(patternHeight, mc) : BigDecimal.ONE;
    
            BigDecimal newCellSize = (widthRatio.compareTo(heightRatio) < 0) ? widthRatio : heightRatio;
    
            setCellWidth(newCellSize.floatValue() *.9F);

        }
    
        BigDecimal drawingWidth = patternWidth.multiply(BigDecimal.valueOf(getCellWidth()));
        BigDecimal drawingHeight = patternHeight.multiply(BigDecimal.valueOf(getCellWidth()));

        BigDecimal offsetX;
        BigDecimal offsetY;

       /* f if (fitBounds) {

            // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
            offsetX = canvasWidth.subtract(drawingWidth).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(bounds.left).multiply(BigDecimal.valueOf(getCell_width())).negate());
            offsetY = canvasHeight.subtract(drawingHeight).divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP).add(new BigDecimal(bounds.top).multiply(BigDecimal.valueOf(getCell_width())).negate());
        
        } else { */
    
            BigDecimal halfCanvasWidth = new BigDecimal(canvas_width).divide(new BigDecimal(2), mc);
            BigDecimal halfCanvasHeight = new BigDecimal(canvas_height).divide(new BigDecimal(2), mc);
        
            BigDecimal halfDrawingWidth = drawingWidth.divide(new BigDecimal(2), mc);
            BigDecimal halfDrawingHeight = drawingHeight.divide(new BigDecimal(2), mc); 
        
            // Adjust offsetX and offsetY calculations to consider the bounds' topLeft corner
            offsetX = halfCanvasWidth.subtract(halfDrawingWidth).add(new BigDecimal(bounds.left).multiply(BigDecimal.valueOf(getCellWidth())).negate());
            offsetY = halfCanvasHeight.subtract(halfDrawingHeight).add(new BigDecimal(bounds.top).multiply(BigDecimal.valueOf(getCellWidth())).negate());
        //}
        canvas_offset_x = offsetX.setScale(0, RoundingMode.HALF_UP).toBigInteger();
        canvas_offset_y = offsetY.setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }
    
    
    
    

    void drawBounds(Bounds bounds, PGraphics offscreenBuffer) {

        Bounds screenBounds = bounds.getScreenBounds(getCellWidth(), canvas_offset_x, canvas_offset_y);

        offscreenBuffer.noFill();
        offscreenBuffer.stroke(200);
        offscreenBuffer.strokeWeight(1);
        offscreenBuffer.rect(screenBounds.leftToFloat(), screenBounds.topToFloat(), screenBounds.rightToFloat(), screenBounds.bottomToFloat());
    }

    // thi work - the cell width times 2 ^ level will give you the size of the whole universe
    // draw_node will draw whatever is visible of it that you want
    void redraw(Node node, PGraphics offscreenBuffer) {
        border_width = (int) (border_width_ratio * getCellWidth());

        BigDecimal size = new BigDecimal(LifeUniverse.pow2(node.level - 1), mc).multiply(BigDecimal.valueOf(getCellWidth()), mc);

        DrawNodeContext ctx = new DrawNodeContext(offscreenBuffer);
        drawNode(node, size.multiply(BigDecimal.valueOf(2), mc), size.negate(), size.negate(), ctx);

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

       //System.out.println("Node level: " + node.level + " Left: " + left + " Top: " + top + " Size: " + size);

        // if we've recursed down to a very small size and the population exists,
        // draw a unit square and be done
        if (size.compareTo(BigDecimal.ONE) <= 0) {
            if (node.population.compareTo(BigInteger.ZERO) > 0) {
                // System.out.println("node to small to fit - level: " + node.level + " left: " + left + ", top: " + top);

                ctx.buffer.fill(cell_color);
                fillSquare(ctx.buffer, Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()), 1);
            }
        } else if (node.level == 0) {
            if (node.population.equals(BigInteger.ONE)) {
               //  System.out.println("Leaf Node 0, left: " + left + ", top: " + top);

                ctx.buffer.fill(cell_color);
                fillSquare(ctx.buffer, Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()), getCellWidth());
            }
        } else {

            if (node.hasChanged()) {
                ctx.changedDrawCount++;
            } else {
                ctx.unchangedDrawCount++;
            }

            if (useImageCache && !node.hasChanged() && node.level <= 4 // not new nodes
                    && Math.pow(2, node.level) * getCellWidth() <= Math.min(canvas_width, canvas_height) // can fit on screen
                    && (getCellWidth() > Math.pow(2,-3)) // will work when cell_widths are small
                  // && (node.level < 8 )// todo: just as an insurance policy for now...
            )
            {

                ImageCache.ImageCacheEntry entry = imageCache.getImageCacheEntry(node);
                PImage cachedImage = entry.getImage();

                ctx.buffer.image(cachedImage,
                        Math.round(leftWithOffset.floatValue()),
                        Math.round(topWithOffset.floatValue()),
                        Math.round(cachedImage.width * getCellWidth()),
                        Math.round(cachedImage.width * getCellWidth()));



                if (debugging) {

                if ((getCellWidth() * Math.pow(2, node.level) ) > 16 ) {

                    p.fill(0, 125); // Black color with alpha value of 178

                    // Draw text with 70% opacity
                    p.textSize(16);
                    p.textAlign(PConstants.LEFT, PConstants.TOP);
                    String cacheString = (entry.isCached()) ? "cache" : "nocache";

                    p.text("id: " + node.id + " node.level:" + node.level + " - " + cacheString, Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue()));
                    p.text("retrievals: " + entry.getRetrievalCount(),Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue())+20);
                    p.text("nodes: " + entry.combinedNodeCount(), Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue())+40);
                    p.text("total: " + entry.getTotalRetrievalCount(), Math.round(leftWithOffset.floatValue()), Math.round(topWithOffset.floatValue())+60);

                }
                p.fill(0, 5); // Black color with alpha value of 25
                p.stroke(1);

                p.rect(Math.round(leftWithOffset.floatValue()),
                        Math.round(topWithOffset.floatValue()),
                        Math.round(cachedImage.width * getCellWidth()),
                        Math.round(cachedImage.width * getCellWidth())); 
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