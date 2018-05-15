package spacegraph.space2d.widget.tab;

import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;

import java.util.Map;

class TabPaneTest {
    public static void main(String[] args) {
        SpaceGraph.window(new TabPane(Map.of(
                "a", () -> new Sketch2DBitmap(40, 40),
                "b", () -> new PushButton("x"))), 800, 800);
    }
}