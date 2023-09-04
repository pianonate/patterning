package patterning.actions

import patterning.pattern.Pattern
import processing.core.PApplet

// this class exists merely to encapsulate movement handling as it is a bit complex
// and i don't want to clutter the main class anymore than it already
class MovementHandler(private val pattern: Pattern) {

    data class Direction(val key: Int, val moveY: Float, val moveX: Float)

    private var lastDirection = 0
    private val initialMoveAmount = 5f
    private var moveAmount = initialMoveAmount
    
    fun handleRequestedMovement() {
        for (key in pressedKeys) {
            when (key) {
                WEST, EAST, NORTH, SOUTH -> handleMovementKeys()
                else ->  // Ignore other keys and set lastDirection to 0
                    lastDirection = 0
            }
        }
    }
    
    private fun handleMovementKeys() {
        var moveX = 0f
        var moveY = 0f

        directions.forEach { direction ->
            if (pressedKeys.contains(direction.key)) {
                moveX += direction.moveX * moveAmount / pressedKeys.size
                moveY += direction.moveY * moveAmount / pressedKeys.size
            }
        }

        val currentDirection = directions.filter { pressedKeys.contains(it.key) }.sumOf { it.key }
        
        if (currentDirection != lastDirection) {
            moveAmount = initialMoveAmount
        }
        lastDirection = currentDirection
        
        pattern.move(moveX, moveY)
    }
    
    companion object {
        const val WEST = PApplet.LEFT
        const val EAST = PApplet.RIGHT
        const val NORTH = PApplet.UP
        const val SOUTH = PApplet.DOWN
        
        private val pressedKeys = KeyHandler.pressedKeys

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