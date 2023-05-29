package ux;

public class PanelTester {
    public PanelTester() {

        createAlignPanel(Panel.HAlign.LEFT, Panel.VAlign.TOP, 0xAA0000FF, Transition.TransitionDirection.RIGHT);
        createAlignPanel(Panel.HAlign.CENTER,Panel.VAlign.TOP, 0xAA0000FF, Transition.TransitionDirection.DOWN);
        createAlignPanel(Panel.HAlign.RIGHT,Panel.VAlign.TOP, 0xAA0000FF, Transition.TransitionDirection.LEFT);
        createAlignPanel(Panel.HAlign.LEFT, Panel.VAlign.CENTER, 0xAAFF0000, Transition.TransitionDirection.DOWN);
        createAlignPanel(Panel.HAlign.CENTER, Panel.VAlign.CENTER, 0xAAFF0000, Transition.TransitionDirection.LEFT);
        //createAlignPanel(Panel.HAlign.RIGHT, Panel.VAlign.CENTER, 0xAAFF0000, Transition.TransitionDirection.LEFT);
        createAlignPanel(Panel.HAlign.LEFT, Panel.VAlign.BOTTOM, 0xAA00FF00,Transition.TransitionDirection.RIGHT);
        createAlignPanel(Panel.HAlign.CENTER, Panel.VAlign.BOTTOM, 0xAA00FF00, Transition.TransitionDirection.RIGHT);
        createAlignPanel(Panel.HAlign.RIGHT, Panel.VAlign.BOTTOM, 0xAA00FF00, Transition.TransitionDirection.LEFT);

        TextPanelWordWrap testText = new TextPanelWordWrap.Builder("expando", Panel.HAlign.LEFT, Panel.VAlign.TOP)
                .textSize(80)
                 .setTransition(Transition.TransitionDirection.RIGHT, Transition.TransitionType.EXPANDO, 1000)
                .displayDuration(3000)
                .build();

        DrawableManager.getInstance().addDrawable(testText);
    }

    private void createAlignPanel(Panel.HAlign hAlign, Panel.VAlign vAlign, int color, Transition.TransitionDirection direction) {

        UXThemeManager theme = UXThemeManager.getInstance();

        Panel panel = new BasicPanel.Builder(hAlign, vAlign,150,250)
                .setFill(color)
                .setTransition(direction, Transition.TransitionType.SLIDE, theme.getLongTransitionDuration())
                .build();

      //  DrawableManager.getInstance().addDrawable(panel);

    }

}
