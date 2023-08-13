package patterning

import patterning.actions.KeyHandler
import patterning.actions.KeyObserver
import patterning.actions.MouseEventManager
import patterning.panel.AlignHorizontal
import patterning.panel.AlignVertical
import patterning.panel.ControlPanel
import patterning.panel.Orientation
import patterning.panel.TextPanel
import patterning.panel.Transition
import patterning.pattern.KeyCallbackFactory
import patterning.pattern.Movable
import patterning.pattern.NumberedPatternLoader
import patterning.pattern.Pattern
import patterning.pattern.Rewindable
import patterning.pattern.Steppable
import processing.core.PApplet
import processing.event.KeyEvent

class UX(
    private val pApplet: PApplet,
    private val canvas: Canvas,
    private val pattern: Pattern
) : KeyObserver {
    
    private val keyCallbackFactory = KeyCallbackFactory(pApplet = pApplet, pattern = pattern, canvas = canvas)
    private val ux = canvas.getNamedGraphicsReference(Theme.uxGraphics)
    private val hudText: TextPanel
    private val countdownText: TextPanel
    
    init {
        KeyHandler.addKeyObserver(this)
        
        keyCallbackFactory.setupSimpleKeyCallbacks()
        setupControls()
        
        hudText = TextPanel.Builder(
            canvas = canvas,
            hAlign = AlignHorizontal.RIGHT,
            vAlign = AlignVertical.BOTTOM
        )
            .textSize(Theme.hudTextSize)
            .wrap()
            .build().also { Drawer.add(it) }
        
        // startup text
        TextPanel.Builder(canvas, Theme.startupText, AlignHorizontal.RIGHT, AlignVertical.TOP)
            .textSize(Theme.startupTextSize)
            .fadeInDuration(Theme.startupTextFadeInDuration)
            .fadeOutDuration(Theme.startupTextFadeOutDuration)
            .displayDuration(Theme.startupTextDisplayDuration)
            .transition(
                Transition.TransitionDirection.LEFT,
                Transition.TransitionType.SLIDE,
                Theme.startupTextTransitionDuration
            )
            .build().also { Drawer.add(it) }
        
        countdownText = TextPanel.Builder(
            canvas,
            Theme.countdownText,
            AlignHorizontal.CENTER,
            AlignVertical.CENTER
        )
            .runMethod {
                RunningState.run()
            }
            .fadeInDuration(2000)
            .countdownFrom(3)
            .wrap()
            .build()
    }
    
    override fun onKeyEvent(event: KeyEvent) {
        if (event.action == KeyEvent.PRESS) {
            if (Drawer.isManaging(countdownText)) {
                Drawer.remove(countdownText)
                pattern.handlePlay()
            }
        }
    }
    
    fun draw() {
        ux.graphics.apply {
            beginDraw()
            clear()
        }
        
        hudText.message = pattern.getHUDMessage()
        Drawer.drawAll()
        
        ux.graphics.endDraw()
        pApplet.image(ux.graphics, 0f, 0f)
        
    }
    
    fun newPattern(patternName: String) {
        displayPatternName(patternName)
        startCountdown()
    }
    
    private fun startCountdown() {
        if (RunningState.runningMode != RunningMode.TESTING) {
            if (Drawer.isManaging(countdownText)) {
                Drawer.remove(countdownText)
            }
            countdownText.startDisplay()
            Drawer.add(countdownText)
        }
    }
    
    private fun displayPatternName(patternName: String) {
        
        TextPanel.Builder(canvas, patternName, AlignHorizontal.LEFT, AlignVertical.TOP)
            .textSize(Theme.startupTextSize)
            .fadeInDuration(Theme.startupTextFadeInDuration)
            .fadeOutDuration(Theme.startupTextFadeOutDuration)
            .displayDuration(Theme.startupTextDisplayDuration)
            .transition(
                Transition.TransitionDirection.RIGHT,
                Transition.TransitionType.SLIDE,
                Theme.startupTextTransitionDuration
            )
            .build().also { Drawer.add(it) }
    }
    
    private fun setupControls() {
        
        val panelTop: ControlPanel
        val panelRight: ControlPanel
        val transitionDuration = Theme.controlPanelTransitionDuration
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
                        addControl("zoomIn.png", keyCallbackFactory.callbackZoomInCenter)
                        addControl("zoomOut.png", keyCallbackFactory.callbackZoomOutCenter)
                        addControl("fitToScreen.png", keyCallbackFactory.callbackFitUniverseOnScreen)
                        addControl("center.png", keyCallbackFactory.callbackCenterView)
                        addControl("undo.png", keyCallbackFactory.callbackUndoMovement)
                        
                    }.build()
            )
        }
        
        panels.add(
            
            ControlPanel.Builder(canvas, AlignHorizontal.CENTER, AlignVertical.TOP)
                .apply {
                    transition(
                        Transition.TransitionDirection.DOWN,
                        Transition.TransitionType.SLIDE,
                        transitionDuration
                    )
                    setOrientation(Orientation.HORIZONTAL)
                    
                    if (pattern is NumberedPatternLoader) addControl(
                        "random.png",
                        keyCallbackFactory.callbackRandomPattern
                    )
                    if (pattern is Steppable) addControl("stepSlower.png", keyCallbackFactory.callbackStepSlower)
                    
                    // .addControl("drawSlower.png", keyFactory.callbackDrawSlower)
                    addPlayPauseControl(
                        "play.png",
                        "pause.png",
                        keyCallbackFactory.callbackPause,
                    )
                    
                    //.addControl("drawFaster.png", keyFactory.callbackDrawFaster)
                    if (pattern is Steppable) addControl("stepFaster.png", keyCallbackFactory.callbackStepFaster)
                    if (pattern is Rewindable) addControl("rewind.png", keyCallbackFactory.callbackRewind)
                    
                }.build()
        )
        
        panels.add(
            ControlPanel.Builder(canvas, AlignHorizontal.RIGHT, AlignVertical.CENTER)
                .apply {
                    transition(Transition.TransitionDirection.LEFT, Transition.TransitionType.SLIDE, transitionDuration)
                    setOrientation(Orientation.VERTICAL)
                    if (pattern is Movable) addToggleHighlightControl("boundary.png", keyCallbackFactory.callbackDrawBounds)
                    addToggleHighlightControl("darkmode.png", keyCallbackFactory.callbackThemeToggle)
                    addToggleHighlightControl("ghost.png", keyCallbackFactory.callbackGhostMode)
                    addToggleHighlightControl("singleStep.png", keyCallbackFactory.callbackSingleStep)
                }.build()
        )
        
        MouseEventManager.addAll(panels)
        Drawer.addAll(panels)
    }
}