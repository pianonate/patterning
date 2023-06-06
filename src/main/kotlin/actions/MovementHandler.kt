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
        for (direction in directions) {
            val isMoving = pressedKeys.contains(direction[0])
            if (isMoving) {
                moveX += direction[2] * moveAmount / pressedKeys.size
                moveY += direction[1] * moveAmount / pressedKeys.size
            }
        }

        // Check if the direction has changed
        var currentDirection = 0
        for (direction in directions) {
            val isMoving = pressedKeys.contains(direction[0])
            if (isMoving) {
                currentDirection += direction[0]
            }
        }
        if (currentDirection != lastDirection) {
            // Reset moveAmount if direction has changed
            moveAmount = initialMoveAmount
        }
        lastDirection = currentDirection
        drawer.move(moveX, moveY)
    }

    companion object {
        private val pressedKeys = KeyHandler.getPressedKeys()
        private val directions = arrayOf(intArrayOf(PApplet.LEFT, 0, -1), intArrayOf(PApplet.UP, -1, 0), intArrayOf(PApplet.RIGHT, 0, 1), intArrayOf(PApplet.DOWN, 1, 0), intArrayOf(PApplet.UP + PApplet.LEFT, -1, -1), intArrayOf(PApplet.UP + PApplet.RIGHT, -1, 1), intArrayOf(PApplet.DOWN + PApplet.LEFT, 1, -1), intArrayOf(PApplet.DOWN + PApplet.RIGHT, 1, 1))
    }
}
