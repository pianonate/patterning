package patterning.ux

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

    fun drawAll() {
        for (drawable in drawables) {
            drawable.draw()
        }
        // Add all drawables that need to be added
        drawables.addAll(toBeAdded)
        toBeAdded.clear()
        // clean up drawables that need to be removed
        drawables.removeAll(toBeRemoved)
        toBeRemoved.clear()
    }

    fun isManaging(drawable: Drawable): Boolean {
        return drawables.contains(drawable)
    }

    fun remove(drawable: Drawable) {
        toBeRemoved.add(drawable)
    }
}