package patterning.life

// the cell width times 2 ^ level will give you the size of the whole universe
// you'll need it to draw the viewport on screen
class UniverseSize {

    fun getSize(universeLevel: Int, zoomLevel: Float): Float {
        return universeSizeImpl(universeLevel, zoomLevel)
    }

    fun getHalf(universeLevel: Int, zoomLevel: Float): Float {
        return universeSizeImpl(universeLevel - 1, zoomLevel)
    }

    private fun universeSizeImpl(universeLevel: Int, zoomLevel: Float): Float {
        return if (universeLevel < 0) 0f
        else zoomLevel * LifeUniverse.pow2(universeLevel)
    }
}