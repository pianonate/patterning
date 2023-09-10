package patterning.actions

import patterning.pattern.Pattern
import processing.core.PApplet

// this class exists merely to encapsulate movement handling as it is a bit complex
// and i don't want to clutter the main class anymore than it already
class MovementHandler(private val pattern: Pattern) {

    data class Direction(val key: Int, val moveY: Float, val moveX: Float)

    fun handleRequestedMovement() {
        val movementKeys = KeyHandler.pressedKeys.intersect(setOf(WEST, EAST, NORTH, SOUTH))

        if (movementKeys.isNotEmpty()) {
            handleMovementKeys(movementKeys)
        }
    }

    private fun handleMovementKeys(movementKeys: Set<Int>) {
        var moveX = 0f
        var moveY = 0f

        directions.forEach { direction ->
            if (movementKeys.contains(direction.key)) {
                moveX += direction.moveX * MOVE_AMOUNT / movementKeys.size
                moveY += direction.moveY * MOVE_AMOUNT / movementKeys.size
            }
        }

        pattern.move(moveX, moveY)
    }

    companion object {
        const val WEST = PApplet.LEFT
        const val EAST = PApplet.RIGHT
        const val NORTH = PApplet.UP
        const val SOUTH = PApplet.DOWN

        private const val MOVE_AMOUNT = 5f

        private val directions = arrayOf(
            Direction(WEST, 0f, -1f),
            Direction(NORTH, -1f, 0f),
            Direction(EAST, 0f, 1f),
            Direction(SOUTH, 1f, 0f),
            Direction(NORTH + WEST, -1f, -1f),
            Direction(NORTH + EAST, -1f, 1f),
            Direction(SOUTH + WEST, 1f, -1f),
            Direction(SOUTH + EAST, 1f, 1f)
        )
    }
}