package patterning.panel

import kotlinx.coroutines.delay
import patterning.Canvas
import patterning.Drawer
import patterning.Theme
import patterning.actions.KeyCallback
import patterning.actions.KeyCallbackObserver
import patterning.actions.KeyEventNotifier
import patterning.actions.MouseEventNotifier
import patterning.actions.MouseEventObserver
import patterning.panel.Transition.TransitionDirection
import patterning.pattern.DisplayState
import patterning.pattern.RenderingOption
import patterning.state.RunningModeController
import patterning.util.AsyncJobRunner
import processing.core.PImage
import processing.core.PVector
import processing.event.KeyEvent

open class Control protected constructor(builder: Builder) : Panel(builder), KeyCallbackObserver, MouseEventObserver {
    private val keyCallback: KeyCallback
    private val size: Int
    internal var isHighlightFromKeypress = false
    protected var icon: PImage
    private var hoverTextPanel: TextPanel? = null
    private val hoverMessage: String

    /**
     * if the warning about 'leaking this' is bothersome, you'd have to create
     * a method that is invoked after control is fully created (sort of like a build().also())
     * to initialize observers
     */
    init {

        size = builder.size
        fillColor = Theme.controlColor
        icon = loadIcon(builder.iconName)

        keyCallback = builder.callback
        KeyEventNotifier.addControlKeyCallbackObserver(keyCallback, this)
        MouseEventNotifier.addMouseEventObserver(this)

        hoverMessage = keyCallback.usage +
                Theme.PAREN_START +
                keyCallback.toString() +
                Theme.PAREN_END
    }

    protected fun toggleHighlight() {
        if (RunningModeController.isUXAvailable)
            isHighlightFromKeypress = !isHighlightFromKeypress
    }

    protected fun loadIcon(iconName: String): PImage {
        val icon = canvas.loadImage(Theme.ICON_PATH + iconName)
        icon.resize(width - Theme.ICON_MARGIN, height - Theme.ICON_MARGIN)
        return icon
    }

    override fun panelSubclassDraw() {
        panelGraphics.noStroke()
        mouseHover(isMouseOverMe)
        drawHover()
        drawPressed()
        drawIcon()
    }

    private fun drawHover() {
        if (isHovering) {
            drawControlHighlight(Theme.controlHighlightColor)
            if (null == hoverTextPanel) {
                hoverTextPanel = getHoverTextPanel()
                Drawer.add(hoverTextPanel!!)
            }
        } else {
            if (null != hoverTextPanel) {
                Drawer.remove(hoverTextPanel!!)
                hoverTextPanel = null
            }
        }
    }


    private fun getHoverTextPanel(): TextPanel {
        val margin = Theme.HOVER_TEXT_MARGIN
        val hoverTextWidth = Theme.HOVER_TEXT_WIDTH
        var hoverX = parentPanel!!.position.x.toInt()
        var hoverY = parentPanel!!.position.y.toInt()
        var transitionDirection: TransitionDirection? = null

        val localParentPanel = parentPanel as ControlPanel

        val orientation = localParentPanel.orientation

        var offsetAlignRightWidth = false

        when (orientation) {
            Orientation.VERTICAL -> {
                when (localParentPanel.hAlign) {
                    AlignHorizontal.LEFT, AlignHorizontal.CENTER -> {
                        hoverX += size + margin
                        hoverY = (hoverY + position.y).toInt()
                        transitionDirection = TransitionDirection.RIGHT
                    }

                    AlignHorizontal.RIGHT -> {
                        hoverX = hoverX - margin - hoverTextWidth
                        hoverY = (hoverY + position.y).toInt()
                        transitionDirection = TransitionDirection.LEFT
                        offsetAlignRightWidth = true
                    }

                }
            }

            Orientation.HORIZONTAL -> {
                hoverX = (hoverX + position.x).toInt()
                when (localParentPanel.vAlign) {
                    AlignVertical.TOP, AlignVertical.CENTER -> {
                        hoverY = localParentPanel.position.y.toInt() + size + margin
                        transitionDirection = TransitionDirection.DOWN
                    }

                    AlignVertical.BOTTOM -> {
                        hoverY = localParentPanel.position.y.toInt() - margin
                        transitionDirection = TransitionDirection.UP
                    }
                }
            }
        }

        val offsetBottom = (orientation === Orientation.HORIZONTAL && localParentPanel.vAlign === AlignVertical.BOTTOM)

        // the Control parentPanel is a ContainerPanel that has a DrawingInfoSupplier
        // which has a PGraphicsSupplier of the current UXBuffer
        // we can't use the parent Control PGraphicsSupplier as it is provided by the ContainerPanel so that the
        // Control draws itself within the ContainerPanel
        //
        // instead we pass the hover text the parent ContainerPanel's DrawingInfoSupplier which comes from
        // PatternDrawer, i.e., and has a PGraphicsSupplier of the UXBuffer itself - otherwise the hover text
        // would try to draw itself within the control at a microscopic size

        return TextPanel.Builder(
            canvas,
            hoverMessage,
            PVector(hoverX.toFloat(), hoverY.toFloat()),
            offsetBottom = offsetBottom,
            offsetAlignRightWidth = offsetAlignRightWidth,
            AlignHorizontal.LEFT,
            AlignVertical.TOP
        ).apply {
            fill(Theme.controlColor)
            radius(Theme.CONTROL_HIGHLIGHT_CORNER_RADIUS)
            textColor(Theme.hoverTextColor)
            textSize(Theme.HOVER_TEXT_SIZE)
            textWidth(hoverTextWidth)
            wrap()
            keepShortCutTogether() // keeps the last two words on the same line when text wrapping
            transition(transitionDirection, Transition.TransitionType.SLIDE, Theme.SHORT_TRANSITION_DURATION)
        }.build()
    }

    private fun mouseHover(isHovering: Boolean) {
        if (isPressed && !isHovering) {
            // If pressed and not hovering, reset the pressed state
            isPressed = false
        } else if (isHovering != isHoveringPrevious) {
            // Only update isHovering if there is a change in hover state
            this.isHovering = isHovering
            isHoveringPrevious = isHovering
        }
    }

    private fun drawPressed() {
        if (isPressed || isHighlightFromKeypress) {
            drawControlHighlight(Theme.controlMousePressedColor)
        }
    }

    protected open fun getCurrentIcon(): PImage {
        return icon
    }

    private fun drawIcon() {
        val thisIcon = getCurrentIcon()
        val x = (width - thisIcon.width).toFloat() / 2
        val y = (height - thisIcon.height).toFloat() / 2
        panelGraphics.image(thisIcon, x, y)
    }

    private fun drawControlHighlight(color: Int) {
        // highlight the control with a semi-transparent rect
        panelGraphics.fill(color) // Semi-transparent gray
        val roundedRectSize = size.toFloat()
        // Rounded rectangle with radius
        panelGraphics.rect(0f, 0f, roundedRectSize, roundedRectSize, Theme.CONTROL_HIGHLIGHT_CORNER_RADIUS.toFloat())
    }

    override fun onKeyPressed(event: KeyEvent) {
        highlightFromKeyPress()
    }

    // Create AsyncJobRunner with a method that sets isHighlightFromKeypress = false after delay
    private val asyncSetHighlightJob = AsyncJobRunner(
        method = suspend {
            delay(Theme.CONTROL_HIGHLIGHT_DURATION.toLong())
            isHighlightFromKeypress = false
        }
    )

    internal fun highlightFromKeyPress() {
        if (!RunningModeController.isUXAvailable) return

        if (!asyncSetHighlightJob.isActive) { // Only start if not already running
            isHighlightFromKeypress = true
            asyncSetHighlightJob.start()
        }
    }

    override fun onMouseReleased() {
        super.onMouseReleased() // Calls Panel's onMouseReleased
        if (isMouseOverMe && RunningModeController.isUXAvailable) {
            keyCallback.invokeFeature() // Specific to Control
        }
    }

    open class Builder(
        canvas: Canvas,
        val callback: KeyCallback,
        val iconName: String,
        val size: Int
    ) : Panel.Builder(
        canvas, size, size
    ) {
        override fun build() = Control(this)
    }

}