package patterning.panel

import patterning.Theme
import patterning.informer.DrawingInfoSupplier
import processing.core.PApplet
import processing.core.PGraphics

class Transition(
    private val drawingInformer: DrawingInfoSupplier,
    private val direction: TransitionDirection,
    private val type: TransitionType,
    private val duration: Int = Theme.shortTransitionDuration
) {
    private var transitionStartTime: Long = -1

 var isTransitioning = true
        private set

    fun image(transitionBuffer: PGraphics, x: Float, y: Float) {
        if (transitionStartTime == -1L) {
            transitionStartTime = System.currentTimeMillis()
        }
        val uxBuffer = drawingInformer.supplyPGraphics()
        val elapsed = System.currentTimeMillis() - transitionStartTime
        val transitionProgress = PApplet.constrain(elapsed.toFloat() / duration, 0f, 1f)
        when (type) {
            TransitionType.EXPANDO -> drawExpandoTransition(uxBuffer, transitionBuffer, transitionProgress, x, y)
            TransitionType.SLIDE -> drawSlideTransition(uxBuffer, transitionBuffer, transitionProgress, x, y)
            TransitionType.DIAGONAL -> drawDiagonalTransition(uxBuffer, transitionBuffer, transitionProgress, x, y)
        }

        // let it do its last transition above otherwise you get a little screen flicker on the transition
        // from getting cut off too soon apparently
        if (transitionProgress == 1f) {
            isTransitioning = false
        }
    }

    private fun drawExpandoTransition(
        uxBuffer: PGraphics,
        transitionBuffer: PGraphics,
        animationProgress: Float,
        x: Float,
        y: Float
    ) {
        when (direction) {
            TransitionDirection.LEFT -> {
                val visibleWidth = (transitionBuffer.width * animationProgress).toInt()
                val revealPointX = (x + transitionBuffer.width - visibleWidth).toInt()
                uxBuffer.image(
                    transitionBuffer,
                    revealPointX.toFloat(),
                    y,
                    visibleWidth.toFloat(),
                    transitionBuffer.height.toFloat()
                )
            }

            TransitionDirection.RIGHT -> uxBuffer.image(
                transitionBuffer, x, y, (transitionBuffer.width * animationProgress).toInt()
                    .toFloat(), transitionBuffer.height.toFloat()
            )

            TransitionDirection.UP -> {
                val visibleHeight = (transitionBuffer.height * animationProgress).toInt()
                val revealPointY = (y + transitionBuffer.height - visibleHeight).toInt()
                uxBuffer.image(
                    transitionBuffer,
                    x,
                    revealPointY.toFloat(),
                    transitionBuffer.width.toFloat(),
                    visibleHeight.toFloat()
                )
            }

            TransitionDirection.DOWN -> uxBuffer.image(
                transitionBuffer,
                x,
                y,
                transitionBuffer.width.toFloat(),
                (transitionBuffer.height * animationProgress).toInt()
                    .toFloat()
            )
        }
    }

    private fun drawSlideTransition(
        uxBuffer: PGraphics,
        transitionBuffer: PGraphics,
        animationProgress: Float,
        x: Float,
        y: Float
    ) {
        when (direction) {
            TransitionDirection.LEFT -> {
                val visibleWidth = (transitionBuffer.width * animationProgress).toInt()
                val revealPointX = (x + (transitionBuffer.width - visibleWidth)).toInt()
                val visiblePart = transitionBuffer[0, 0, visibleWidth, transitionBuffer.height]
                uxBuffer.image(visiblePart, revealPointX.toFloat(), y)
            }

            TransitionDirection.RIGHT -> {
                val visibleWidth = (transitionBuffer.width * animationProgress).toInt()
                val revealPointX = x.toInt()
                val visiblePart =
                    transitionBuffer[transitionBuffer.width - visibleWidth, 0, visibleWidth, transitionBuffer.height]
                uxBuffer.image(visiblePart, revealPointX.toFloat(), y)
            }

            TransitionDirection.UP -> {
                val visibleHeight = (transitionBuffer.height * animationProgress).toInt()
                val revealPointY = (y + (transitionBuffer.height - visibleHeight)).toInt()
                val visiblePart = transitionBuffer[0, 0, transitionBuffer.width, visibleHeight]
                uxBuffer.image(visiblePart, x, revealPointY.toFloat())
            }

            TransitionDirection.DOWN -> {
                val visibleHeight = (transitionBuffer.height * animationProgress).toInt()
                val revealPointY = y.toInt()
                val visiblePart =
                    transitionBuffer[0, transitionBuffer.height - visibleHeight, transitionBuffer.width, visibleHeight]
                uxBuffer.image(visiblePart, x, revealPointY.toFloat())
            }
        }
    }

    private fun drawDiagonalTransition(
        uxBuffer: PGraphics,
        transitionBuffer: PGraphics,
        animationProgress: Float,
        x: Float,
        y: Float
    ) {
        when (direction) {
            TransitionDirection.LEFT -> {
                val visibleWidth = (transitionBuffer.width * animationProgress).toInt()
                val revealPointX = (x + (transitionBuffer.width - visibleWidth)).toInt()
                uxBuffer.image(
                    transitionBuffer[transitionBuffer.width - visibleWidth, (transitionBuffer.height * (1 - animationProgress)).toInt(), visibleWidth, (transitionBuffer.height * animationProgress).toInt()],
                    revealPointX.toFloat(),
                    y
                )
            }

            TransitionDirection.RIGHT -> uxBuffer.image(
                transitionBuffer[(transitionBuffer.width - transitionBuffer.width * animationProgress).toInt(), (transitionBuffer.height - transitionBuffer.height * animationProgress).toInt(), (transitionBuffer.width * animationProgress).toInt(), (transitionBuffer.height * animationProgress).toInt()],
                x,
                y
            )

            TransitionDirection.UP -> uxBuffer.image(
                transitionBuffer[0, 0, (transitionBuffer.width * animationProgress).toInt(), (transitionBuffer.height * animationProgress).toInt()],
                x + transitionBuffer.width * (1 - animationProgress),
                y + transitionBuffer.height * (1 - animationProgress)
            )

            TransitionDirection.DOWN -> uxBuffer.image(
                transitionBuffer[0, (transitionBuffer.height * (1 - animationProgress)).toInt(), (transitionBuffer.width * animationProgress).toInt(), (transitionBuffer.height * animationProgress).toInt()],
                x,
                y
            )
        }
    }

    enum class TransitionDirection {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    enum class TransitionType {
        EXPANDO,
        SLIDE,
        DIAGONAL
    }
}