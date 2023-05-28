package ux;

public class BasicPanel extends Panel {
    protected BasicPanel(Builder builder) {
        super(builder);
    }

    public static class Builder extends Panel.Builder<Builder> {
  /*      public Builder(int x, int y, int width, int height) {
            super(x, y, width, height);
        }*/

        public Builder(HAlign hAlign, VAlign vAlign, int width, int height) {
            super(hAlign, vAlign, width, height);
        }

   /*     public Builder(HAlign hAlign, VAlign vAlign) {
            super(hAlign, vAlign);
        }*/

        @Override
        public BasicPanel build() {
            return new BasicPanel(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
