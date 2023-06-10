package ux.panel

import processing.core.PApplet
import processing.core.PGraphics
import ux.UXThemeManager
import ux.informer.DrawingInfoSupplier

class Transition @JvmOverloads constructor(
    private val drawingInformer: DrawingInfoSupplier,
    private val direction: TransitionDirection,
    private val type: TransitionType,
    private val duration: Int = UXThemeManager.getInstance().shortTransitionDuration
) {
    private var transitionStartTime: Long = -1

    /*    public void reset() {
        transitionStartTime = -1;
    }*/ var isTransitioning = true
        private set

    fun image(transitionBuffer: PGraphics, x: Float, y: Float) {
        if (transitionStartTime == -1L) {
            transitionStartTime = System.currentTimeMillis()
        }
        val UXBuffer = drawingInformer.pGraphics
        val elapsed = System.currentTimeMillis() - transitionStartTime
        val transitionProgress = PApplet.constrain(elapsed.toFloat() / duration, 0f, 1f)
        when (type) {
            TransitionType.EXPANDO -> drawExpandoTransition(UXBuffer, transitionBuffer, transitionProgress, x, y)
            TransitionType.SLIDE -> drawSlideTransition(UXBuffer, transitionBuffer, transitionProgress, x, y)
            TransitionType.DIAGONAL -> drawDiagonalTransition(UXBuffer, transitionBuffer, transitionProgress, x, y)
        }

        // let it do its last transition above otherwise you get a little screen flicker on the transition
        // from getting cut off too soon apparently
        if (transitionProgress == 1f) {
            isTransitioning = false
        }
    }

    private fun drawExpandoTransition(
        UXBuffer: PGraphics,
        transitionBuffer: PGraphics,
        animationProgress: Float,
        x: Float,
        y: Float
    ) {
        when (direction) {
            TransitionDirection.LEFT -> {
                val visibleWidth = (transitionBuffer.width * animationProgress).toInt()
                val revealPointX = (x + transitionBuffer.width - visibleWidth).toInt()
                UXBuffer.image(
                    transitionBuffer,
                    revealPointX.toFloat(),
                    y,
                    visibleWidth.toFloat(),
                    transitionBuffer.height.toFloat()
                )
            }

            TransitionDirection.RIGHT -> UXBuffer.image(
                transitionBuffer, x, y, (transitionBuffer.width * animationProgress).toInt()
                    .toFloat(), transitionBuffer.height.toFloat()
            )

            TransitionDirection.UP -> {
                val visibleHeight = (transitionBuffer.height * animationProgress).toInt()
                val revealPointY = (y + transitionBuffer.height - visibleHeight).toInt()
                UXBuffer.image(
                    transitionBuffer,
                    x,
                    revealPointY.toFloat(),
                    transitionBuffer.width.toFloat(),
                    visibleHeight.toFloat()
                )
            }

            TransitionDirection.DOWN -> UXBuffer.image(
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
        UXBuffer: PGraphics,
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
                UXBuffer.image(visiblePart, revealPointX.toFloat(), y)
            }

            TransitionDirection.RIGHT -> {
                val visibleWidth = (transitionBuffer.width * animationProgress).toInt()
                val revealPointX = x.toInt()
                val visiblePart =
                    transitionBuffer[transitionBuffer.width - visibleWidth, 0, visibleWidth, transitionBuffer.height]
                UXBuffer.image(visiblePart, revealPointX.toFloat(), y)
            }

            TransitionDirection.UP -> {
                val visibleHeight = (transitionBuffer.height * animationProgress).toInt()
                val revealPointY = (y + (transitionBuffer.height - visibleHeight)).toInt()
                val visiblePart = transitionBuffer[0, 0, transitionBuffer.width, visibleHeight]
                UXBuffer.image(visiblePart, x, revealPointY.toFloat())
            }

            TransitionDirection.DOWN -> {
                val visibleHeight = (transitionBuffer.height * animationProgress).toInt()
                val revealPointY = y.toInt()
                val visiblePart =
                    transitionBuffer[0, transitionBuffer.height - visibleHeight, transitionBuffer.width, visibleHeight]
                UXBuffer.image(visiblePart, x, revealPointY.toFloat())
            }
        }
    }

    private fun drawDiagonalTransition(
        UXBuffer: PGraphics,
        transitionBuffer: PGraphics,
        animationProgress: Float,
        x: Float,
        y: Float
    ) {
        when (direction) {
            TransitionDirection.LEFT -> {
                val visibleWidth = (transitionBuffer.width * animationProgress).toInt()
                val revealPointX = (x + (transitionBuffer.width - visibleWidth)).toInt()
                UXBuffer.image(
                    transitionBuffer[transitionBuffer.width - visibleWidth, (transitionBuffer.height * (1 - animationProgress)).toInt(), visibleWidth, (transitionBuffer.height * animationProgress).toInt()],
                    revealPointX.toFloat(),
                    y
                )
            }

            TransitionDirection.RIGHT -> UXBuffer.image(
                transitionBuffer[(transitionBuffer.width - transitionBuffer.width * animationProgress).toInt(), (transitionBuffer.height - transitionBuffer.height * animationProgress).toInt(), (transitionBuffer.width * animationProgress).toInt(), (transitionBuffer.height * animationProgress).toInt()],
                x,
                y
            )

            TransitionDirection.UP -> UXBuffer.image(
                transitionBuffer[0, 0, (transitionBuffer.width * animationProgress).toInt(), (transitionBuffer.height * animationProgress).toInt()],
                x + transitionBuffer.width * (1 - animationProgress),
                y + transitionBuffer.height * (1 - animationProgress)
            )

            TransitionDirection.DOWN -> UXBuffer.image(
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