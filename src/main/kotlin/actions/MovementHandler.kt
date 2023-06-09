package actions

import processing.core.PApplet
import ux.PatternDrawer

// this class exists merely to encapsulate movement handling as it is a bit complex
// and i don't want to clutter the main class anymore than it already
class MovementHandler(private val drawer: PatternDrawer) {

    private var lastDirection = 0
    private val initialMoveAmount = 5f
    private var moveAmount = initialMoveAmount

    fun handleRequestedMovement() {
        for (key in pressedKeys) {
            when (key) {
                PApplet.LEFT, PApplet.RIGHT, PApplet.UP, PApplet.DOWN -> handleMovementKeys()
                else ->  // Ignore other keys and set lastDirection to 0
                    lastDirection = 0
            }
        }
    }

    private fun handleMovementKeys() {
        var moveX = 0f
        var moveY = 0f

        directions.forEach { direction ->
            if (pressedKeys.contains(direction[0])) {
                moveX += direction[2] * moveAmount / pressedKeys.size
                moveY += direction[1] * moveAmount / pressedKeys.size
            }
        }

        val currentDirection = directions.filter { pressedKeys.contains(it[0]) }.sumOf { it[0] }

        if (currentDirection != lastDirection) {
            moveAmount = initialMoveAmount
        }
        lastDirection = currentDirection

        drawer.move(moveX, moveY)
    }


    companion object {
        const val WEST = PApplet.LEFT
        const val EAST = PApplet.RIGHT
        const val NORTH = PApplet.UP
        const val SOUTH = PApplet.DOWN
        const val NORTHWEST = NORTH + WEST
        const val NORTHEAST = NORTH + EAST
        const val SOUTHWEST = SOUTH + WEST
        const val SOUTHEAST = SOUTH + EAST

        private val pressedKeys = KeyHandler.getPressedKeys()
        private val directions = arrayOf(
                intArrayOf(WEST, 0, -1),
                intArrayOf(NORTH, -1, 0),
                intArrayOf(EAST, 0, 1),
                intArrayOf(SOUTH, 1, 0),
                intArrayOf(NORTHWEST, -1, -1),
                intArrayOf(NORTHEAST, -1, 1),
                intArrayOf(SOUTHWEST, 1, -1),
                intArrayOf(SOUTHEAST, 1, 1)
        )
    }
}
