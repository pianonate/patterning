package ux

class DrawableManager private constructor() {
    private val drawables: MutableList<Drawable>
    private val toBeAdded: MutableList<Drawable>
    private val toBeRemoved: MutableList<Drawable>

    init {
        drawables = ArrayList()
        toBeAdded = ArrayList()
        toBeRemoved = ArrayList()
    }

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

    companion object {
        @JvmStatic
        var instance: DrawableManager? = null
            get() {
                if (field == null) {
                    field = DrawableManager()
                }
                return field
            }
            private set
    }
}