package ux;

public class PanelTester {
    public static void makeSomePanels(PGraphicsSupplier graphicsSupplier, boolean makeBasicPanels, boolean makeExpando) {

        if (makeBasicPanels) {

            createAlignPanel(graphicsSupplier, AlignHorizontal.LEFT, AlignVertical.TOP, 0xAA0000FF, Transition.TransitionDirection.RIGHT);
            createAlignPanel(graphicsSupplier, AlignHorizontal.CENTER, AlignVertical.TOP, 0xAA0000FF, Transition.TransitionDirection.DOWN);
            createAlignPanel(graphicsSupplier, AlignHorizontal.RIGHT, AlignVertical.TOP, 0xAA0000FF, Transition.TransitionDirection.LEFT);
            createAlignPanel(graphicsSupplier, AlignHorizontal.LEFT, AlignVertical.CENTER, 0xAAFF0000, Transition.TransitionDirection.DOWN);
            createAlignPanel(graphicsSupplier, AlignHorizontal.CENTER, AlignVertical.CENTER, 0xAAFF0000, Transition.TransitionDirection.LEFT);
            createAlignPanel(graphicsSupplier, AlignHorizontal.RIGHT, AlignVertical.CENTER, 0xAAFF0000, Transition.TransitionDirection.LEFT);
            createAlignPanel(graphicsSupplier, AlignHorizontal.LEFT, AlignVertical.BOTTOM, 0xAA00FF00, Transition.TransitionDirection.RIGHT);
            createAlignPanel(graphicsSupplier, AlignHorizontal.CENTER, AlignVertical.BOTTOM, 0xAA00FF00, Transition.TransitionDirection.RIGHT);
            createAlignPanel(graphicsSupplier, AlignHorizontal.RIGHT, AlignVertical.BOTTOM, 0xAA00FF00, Transition.TransitionDirection.LEFT);
        }

        if (makeExpando) {
            TextPanel testText = new TextPanel.Builder(graphicsSupplier, "expando", AlignHorizontal.LEFT, AlignVertical.TOP)
                    .textSize(50)
                    .transition(Transition.TransitionDirection.RIGHT, Transition.TransitionType.EXPANDO, 1000)
                    .displayDuration(5000)
                    .build();

            DrawableManager.getInstance().add(testText);
        }
    }

    private static void createAlignPanel(PGraphicsSupplier graphicsSupplier, AlignHorizontal hAlign, AlignVertical vAlign, int color, Transition.TransitionDirection direction) {

        UXThemeManager theme = UXThemeManager.getInstance();

        Panel panel = new BasicPanel.Builder(graphicsSupplier, hAlign, vAlign, 150, 250)
                .fill(color)
                .transition(direction, Transition.TransitionType.SLIDE, theme.getLongTransitionDuration())
                .build();
         DrawableManager.getInstance().add(panel);

    }

}
