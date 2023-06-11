package ux.panel

import actions.KeyCallback
import actions.KeyObservable
import ux.informer.DrawingInfoSupplier

class ToggleHighlightControl private constructor(builder: Builder?) : Control(
    builder!!
) {
    override fun onMouseReleased() {
        super.onMouseReleased()
        isHighlightFromKeypress = !isHighlightFromKeypress
    }

    override fun notifyKeyPress(observer: KeyObservable) {
        // Specific behavior for ToggleHighlightControl
        isHighlightFromKeypress = !isHighlightFromKeypress
    }

    class Builder(drawingInformer: DrawingInfoSupplier?, callback: KeyCallback?, iconName: String?, size: Int) :
        Control.Builder(drawingInformer, callback!!, iconName!!, size) {
        override fun build(): ToggleHighlightControl {
            return ToggleHighlightControl(this)
        }

        override fun self(): Builder {
            return this
        }
    }
}