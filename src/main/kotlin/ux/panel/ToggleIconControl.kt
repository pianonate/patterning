package ux.panel

import actions.KeyCallback
import actions.KeyObservable
import processing.core.PImage
import ux.UXThemeManager.Companion.instance
import ux.informer.DrawingInfoSupplier
import java.util.*

class ToggleIconControl(builder: Builder) : Control(builder) {
    private var toggledIcon // right now only used with play / pause
            : PImage
    private var iconToggled = false
    private var singleMode = false
    private lateinit var currentIcon: PImage
    private var modeChangeCallback: KeyCallback

    init {
        toggledIcon = loadIcon(builder.toggledIconName)
        modeChangeCallback = builder.modeChangeCallback
        modeChangeCallback.addObserver(this)
    }

    override fun afterInit() {
        currentIcon = icon
    }

    override fun getCurrentIcon(): PImage {
        return if (iconToggled) toggledIcon else super.getCurrentIcon()
    }

    override fun notifyKeyPress(observer: KeyObservable) {
        if (observer.invokeModeChange()) {
            toggleMode()
            if (!iconToggled) toggleIcon()
        } else toggleIcon()
    }

    override fun onMouseReleased() {
        super.onMouseReleased()
        toggleIcon()
    }

    private fun toggleIcon() {
        currentIcon = if (iconToggled) icon else toggledIcon
        iconToggled = !iconToggled
        if (singleMode && !iconToggled) {
            Timer().schedule(
                object : TimerTask() {
                    override fun run() {
                        toggleIcon()
                    }
                },
                instance.singleModeToggleDuration
                    .toLong()
            )
        }
    }

    private fun toggleMode() {
        singleMode = !singleMode
    }

    class Builder(
        drawingInformer: DrawingInfoSupplier?,
        callback: KeyCallback?,
        val modeChangeCallback: KeyCallback,
        iconName: String?,
        val toggledIconName: String,
        size: Int
    ) : Control.Builder(drawingInformer, callback!!, iconName!!, size) {
        override fun self(): Builder {
            return this
        }

        override fun build(): ToggleIconControl {
            return ToggleIconControl(this)
        }
    }
}