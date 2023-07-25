package patterning.ux.panel

import patterning.actions.*
import patterning.ux.Drawer
import patterning.ux.Theme
import patterning.ux.informer.DrawingInfoSupplier
import patterning.ux.panel.Transition.TransitionDirection
import processing.core.PImage
import processing.core.PVector
import java.util.*

open class Control protected constructor(builder: Builder) : Panel(builder), KeyObserver, MouseEventReceiver {
    private val callback: KeyCallback
    private val size: Int
    var isHighlightFromKeypress = false
    protected var icon: PImage
    private var hoverTextPanel: TextPanel? = null
    private val hoverMessage: String

    open fun afterInit() {
        // todo: hack for ToggleIconControl which eventually we'll fix
    }

    init {
        callback = builder.callback
        size = builder.size
        fill = Theme.controlColor
        callback.addObserver(this)
        MouseEventManager.instance!!.addReceiver(this)
        icon = loadIcon(builder.iconName)
        val keyCombos = callback.toString()
        hoverMessage = callback.getUsageText() + Theme.shortcutParenStart + keyCombos + Theme.shortcutParenEnd
        this.afterInit()
    }


    protected fun loadIcon(iconName: String): PImage {
        val icon = panelBuffer.parent.loadImage(Theme.iconPath + iconName)
        icon.resize(width - Theme.iconMargin, height - Theme.iconMargin)
        return icon
    }

    override fun panelSubclassDraw() {
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
        val margin = Theme.hoverTextMargin
        val hoverTextWidth = Theme.hoverTextWidth
        var hoverX = parentPanel!!.position!!.x.toInt()
        var hoverY = parentPanel!!.position!!.y.toInt()
        var transitionDirection: TransitionDirection? = null

        val localParentPanel = parentPanel
        val orientation = (localParentPanel as ControlPanel).orientation

        when (orientation) {
            Orientation.VERTICAL -> {
                when (localParentPanel.hAlign) {
                    AlignHorizontal.LEFT, AlignHorizontal.CENTER -> {
                        hoverX += size + margin
                        hoverY = (hoverY + position!!.y).toInt()
                        transitionDirection = TransitionDirection.RIGHT
                    }

                    AlignHorizontal.RIGHT -> {
                        hoverX = hoverX - margin - hoverTextWidth
                        hoverY = (hoverY + position!!.y).toInt()
                        transitionDirection = TransitionDirection.LEFT
                    }

                    else -> {
                        // Handle all other cases here.
                    }
                }
            }

            Orientation.HORIZONTAL -> {
                hoverX = (hoverX + position!!.x).toInt()
                when (localParentPanel.vAlign) {
                    AlignVertical.TOP, AlignVertical.CENTER -> {
                        hoverY = localParentPanel.position!!.y.toInt() + size + margin
                        transitionDirection = TransitionDirection.DOWN
                    }

                    AlignVertical.BOTTOM -> {
                        hoverY = localParentPanel.position!!.y.toInt() - margin
                        transitionDirection = TransitionDirection.UP
                    }

                    else -> {
                        // Handle all other cases here.
                    }
                }
            }
        }

        // the Control parentPanel is a ContainerPanel that has a DrawingInfoSupplier
        // which has a PGraphicsSupplier of the current UXBuffer
        // we can't use the parent Control PGraphicsSupplier as it is provided by the ContainerPanel so that the
        // Control draws itself within the ContainerPanel
        //
        // instead we pass the hover text the parent ContainerPanel's DrawingInfoSupplier which comes from
        // PatternDrawer, i.e., and has a PGraphicsSupplier of the UXBuffer itself - otherwise the hover text
        // would try to draw itself within the control at a microscopic size
        val hoverText = TextPanel.Builder(
            localParentPanel.drawingInformer,
            hoverMessage,
            PVector(hoverX.toFloat(), hoverY.toFloat()),
            AlignHorizontal.LEFT,
            AlignVertical.TOP
        )
            .fill(Theme.controlHighlightColor)
            .radius(Theme.controlHighlightCornerRadius)
            .textSize(Theme.hoverTextSize)
            .textWidth(hoverTextWidth)
            .wrap()
            .keepShortCutTogether() // keeps the last two words on the same line when text wrapping
            .transition(transitionDirection, Transition.TransitionType.SLIDE, Theme.shortTransitionDuration)
            .build()

        // hover text is word wrapped and sized to fit
        // we pass in the max and set up the position to display
        // for RIGHT aligned VERTICAL control panels, we need to change the x position to make it appear
        // next to the control.  Not a problem for TOP, LEFT, BOTTOM controls
        // we could put this logic into TextPanel so that it adjusts
        // its own x position based on the alignment of this control but that would clutter TextPanel
        //
        // maybe a generic capability of aligning controls to each other
        // could be added in the future if it becomes a common need -for now, we just do it here
        if (orientation === Orientation.VERTICAL && localParentPanel.hAlign === AlignHorizontal.RIGHT) {
            hoverText.position!!.x += (hoverTextWidth - hoverText.width).toFloat()
        }

        // similar treatment for HORIZONTAL aligned BOTTOM control panels
        if (orientation === Orientation.HORIZONTAL && localParentPanel.vAlign === AlignVertical.BOTTOM) {
            hoverText.position!!.y -= hoverText.height.toFloat()
        }

        // if the text won't display, make it possible to display
        val screenWidth = localParentPanel.drawingInformer.supplyPGraphics().width
        if (hoverText.position!!.x + hoverText.width > screenWidth) {
            hoverText.position!!.x = (screenWidth - hoverText.width).toFloat()
        }
        return hoverText
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
        panelBuffer.image(thisIcon, x, y)
    }

    private fun drawControlHighlight(color: Int) {
        // highlight the control with a semi-transparent rect
        panelBuffer.fill(color) // Semi-transparent gray
        val roundedRectSize = size.toFloat()
        // Rounded rectangle with radius
        panelBuffer.rect(0f, 0f, roundedRectSize, roundedRectSize, Theme.controlHighlightCornerRadius.toFloat())
    }

    override fun notifyKeyPress(observer: KeyObservable) {
        highlightFromKeyPress()
    }

    internal fun highlightFromKeyPress() {
        isHighlightFromKeypress = true
        Timer().schedule(
            object : TimerTask() {
                override fun run() {
                    isHighlightFromKeypress = false
                }
            },
            Theme.controlHighlightDuration
                .toLong()
        )
    }

    override fun onMouseReleased() {
        super.onMouseReleased() // Calls Panel's onMouseReleased
        if (isMouseOverMe) {
            callback.invokeFeature() // Specific to Control
        }
    }

    open class Builder(
        drawingInformer: DrawingInfoSupplier?,
        val callback: KeyCallback,
        val iconName: String,
        val size: Int
    ) : Panel.Builder<Builder?>(
        drawingInformer!!, size, size
    ) {
        public override fun self(): Builder {
            return this
        }

        override fun build(): Control? {
            return Control(this)
        }
    }

}