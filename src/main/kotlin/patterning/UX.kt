package patterning

import patterning.actions.KeyCallbackObserver
import patterning.actions.KeyEventNotifier
import patterning.actions.MouseEventNotifier
import patterning.panel.AlignHorizontal
import patterning.panel.AlignVertical
import patterning.panel.ControlPanel
import patterning.panel.Orientation
import patterning.panel.TextPanel
import patterning.panel.Transition
import patterning.pattern.Colorful
import patterning.pattern.KeyCallbackFactory
import patterning.pattern.Movable
import patterning.pattern.NumberedPatternLoader
import patterning.pattern.Pattern
import patterning.pattern.Rewindable
import patterning.pattern.Steppable
import patterning.pattern.ThreeDimensional
import patterning.state.RunningModeController
import processing.core.PApplet
import processing.event.KeyEvent

class UX(
    private val pApplet: PApplet,
    private val canvas: Canvas,
    private val pattern: Pattern
) : KeyCallbackObserver {

    private val keyCallbackFactory =
        KeyCallbackFactory(pApplet = pApplet, pattern = pattern, canvas = canvas)
    private val ux = canvas.getNamedGraphicsReference(Theme.UX_GRAPHICS)
    private val hudText: TextPanel
    private val countdownText: TextPanel

    init {

        KeyEventNotifier.addGlobalKeyCallbackObserver(this)

        keyCallbackFactory.setupSimpleKeyCallbacks()
        setupControls()

        hudText = TextPanel.Builder(
            canvas = canvas,
            hAlign = AlignHorizontal.RIGHT,
            vAlign = AlignVertical.BOTTOM
        )
            .textSize(Theme.HUD_TEXT_SIZE)
            .wrap()
            .build().also { Drawer.add(it) }

        // startup text
        TextPanel.Builder(canvas, Theme.STARTUP_TEXT, AlignHorizontal.RIGHT, AlignVertical.TOP)
            .textSize(Theme.STARTUP_TEXT_SIZE)
            .fadeInDuration(Theme.STARTUP_TEXT_FADE_IN_DURATION)
            .fadeOutDuration(Theme.STARTUP_TEXT_FADE_OUT_DURATION)
            .displayDuration(Theme.STARTUP_TEXT_DISPLAY_DURATION)
            .transition(
                Transition.TransitionDirection.LEFT,
                Transition.TransitionType.SLIDE,
                Theme.STARTUP_TEXT_TRANSITION_DURATION
            )
            .build().also { Drawer.add(it) }

        countdownText = TextPanel.Builder(
            canvas,
            Theme.COUNTDOWN_TEXT,
            AlignHorizontal.CENTER,
            AlignVertical.CENTER
        )
            .runMethod {
                RunningModeController.play()
            }
            .fadeInDuration(Theme.STARTUP_TEXT_FADE_IN_DURATION)
            .countdownFrom(Theme.COUNTDOWN_FROM)
            .wrap()
            .build()
    }

    /**
     * only invoke handlePlay if it's not the key that already toggles play mode
     * it doesn't feel like good design to have to explicitly check for this key
     */
    override fun onKeyPressed(event: KeyEvent) {
        // it doesn't feel great that we're both listening to all keys and also
        // have a special case for the one key that would also handle play/pause as it's actual function
        handleCountdownInterrupt()
    }

    private fun handleCountdownInterrupt() {
        if (Drawer.isManaging(countdownText)) {
            Drawer.remove(countdownText)
            RunningModeController.pause()
        }
    }

    fun draw() {

        hudText.message = pattern.getHUDMessage()

        ux.graphics.beginDraw()
        ux.graphics.clear()
        Drawer.drawAll()
        ux.graphics.endDraw()
        pApplet.image(ux.graphics, 0f, 0f)
    }

    fun newPattern(patternName: String) {
        displayPatternName(patternName)
        startCountdown()
    }

    private fun startCountdown() {
        if (Drawer.isManaging(countdownText)) {
            Drawer.remove(countdownText)
        }

        // don't need countdowns for testing
        if (RunningModeController.isTesting) {
            return
        }

        countdownText.startDisplay()
        Drawer.add(countdownText)
    }

    private fun displayPatternName(patternName: String) {

        TextPanel.Builder(canvas, patternName, AlignHorizontal.LEFT, AlignVertical.TOP)
            .textSize(Theme.STARTUP_TEXT_SIZE)
            .fadeInDuration(Theme.STARTUP_TEXT_FADE_IN_DURATION)
            .fadeOutDuration(Theme.STARTUP_TEXT_FADE_OUT_DURATION)
            .displayDuration(Theme.STARTUP_TEXT_DISPLAY_DURATION)
            .transition(
                Transition.TransitionDirection.RIGHT,
                Transition.TransitionType.SLIDE,
                Theme.STARTUP_TEXT_TRANSITION_DURATION
            )
            .build().also { Drawer.add(it) }
    }

    private fun setupControls() {

        val transitionDuration = Theme.CONTROL_PANEL_TRANSITION_DURATION
        val panels = mutableListOf<ControlPanel>()

        if (pattern is Movable) {
            panels.add(
                ControlPanel.Builder(canvas, AlignHorizontal.LEFT, AlignVertical.CENTER)
                    .apply {
                        transition(
                            Transition.TransitionDirection.RIGHT,
                            Transition.TransitionType.SLIDE,
                            transitionDuration
                        )
                        setOrientation(Orientation.VERTICAL)
                        if (pattern is NumberedPatternLoader) addControl(
                            "random.png",
                            keyCallbackFactory.callbackRandomPattern
                        )
                        if (pattern is Steppable) {
                            addToggleHighlightControl(
                                iconName = "singleStep.png",
                                callback = keyCallbackFactory.callbackSingleStep
                            )
                            addControl("stepSlower.png", keyCallbackFactory.callbackStepSlower)
                        }

                        // .addControl("drawSlower.png", keyFactory.callbackDrawSlower)
                        addPlayPauseControl(
                            "play.png",
                            "pause.png",
                            keyCallbackFactory.callbackPlayPause,
                        )

                        //.addControl("drawFaster.png", keyFactory.callbackDrawFaster)
                        if (pattern is Steppable) addControl("stepFaster.png", keyCallbackFactory.callbackStepFaster)
                        if (pattern is Rewindable) addControl("rewind.png", keyCallbackFactory.callbackRewind)

                        addControl("zoomIn.png", keyCallbackFactory.callbackZoomInCenter)
                        addControl("zoomOut.png", keyCallbackFactory.callbackZoomOutCenter)
                        addControl("fitToScreen.png", keyCallbackFactory.callbackCenterAndFit)
                        addControl("center.png", keyCallbackFactory.callbackCenter)
                        addControl("undo.png", keyCallbackFactory.callbackUndoMovement)

                    }.build()
            )
        }

        panels.add(
            ControlPanel.Builder(canvas, AlignHorizontal.RIGHT, AlignVertical.CENTER)
                .apply {
                    transition(Transition.TransitionDirection.LEFT, Transition.TransitionType.SLIDE, transitionDuration)
                    setOrientation(Orientation.VERTICAL)
                    addControl("camera.png", keyCallbackFactory.callbackSaveImage)
                    if (pattern is Movable) {
                        addToggleHighlightControl("boundary.png", keyCallbackFactory.callbackDrawBoundary)
                        addToggleHighlightControl("boundaryOnly.png", keyCallbackFactory.callbackDrawBoundaryOnly)
                    }
                    if (pattern is Colorful) addToggleHighlightControl("heart.png", keyCallbackFactory.callbackColorful)
                    addToggleHighlightControl("darkmode.png", keyCallbackFactory.callbackThemeToggle)
                    addToggleHighlightControl("ghost.png", keyCallbackFactory.callbackGhostMode,)
                    addToggleHighlightControl("fade.png", keyCallbackFactory.callbackFadeAway,)
                    if (pattern is ThreeDimensional) {
                        addToggleHighlightControl(
                            "cube.png",
                            keyCallbackFactory.callbackThreeDBoxes,
                        )
                        addToggleHighlightControl(
                            "yaw.png",
                            keyCallbackFactory.callbackThreeDYaw,
                        )
                        addToggleHighlightControl(
                            "pitch.png",
                            keyCallbackFactory.callbackThreeDPitch,
                            )
                        addToggleHighlightControl(
                            "roll.png",
                            keyCallbackFactory.callbackThreeDRoll,
                        )
                        addToggleHighlightControl(
                            "infinity.png",
                            keyCallbackFactory.callbackAlwaysRotate,
                        )
                    }
                }.build()
        )

        MouseEventNotifier.addAll(panels)
        Drawer.addAll(panels)
    }

    fun mouseReleased() {
        handleCountdownInterrupt()
    }
}