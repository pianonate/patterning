package patterning

object Drawer {
    private val drawables: MutableList<Drawable> = ArrayList()
    private val toBeAdded: MutableList<Drawable> = ArrayList()
    private val toBeRemoved: MutableList<Drawable> = ArrayList()
    
    fun add(drawable: Drawable) {
        toBeAdded.add(drawable)
    }
    
    fun addAll(drawables: List<Drawable>?) {
        toBeAdded.addAll(drawables!!)
    }
    
    /**
     * countDownText specifically depends on the ordering of removing and adding
     * because when it is interrupted by a keypress it is removed - which is fine
     * but if the keypress is also one that invokes a new pattern then it is both removed
     * and also added - so the remove, add, draw ordering has to be preserved
     *
     * it's a little fragile but for now we'll live with it
     */
    fun drawAll() {
        
        // clean up drawables that need to be removed
        drawables.removeAll(toBeRemoved)
        toBeRemoved.clear()
        
        // Add all drawables that need to be added
        drawables.addAll(toBeAdded)
        toBeAdded.clear()
        
        for (drawable in drawables) {
            drawable.draw()
        }
        
    }
    
    fun isManaging(drawable: Drawable): Boolean {
        return drawables.contains(drawable)
    }
    
    fun remove(drawable: Drawable) {
        toBeRemoved.add(drawable)
    }
}