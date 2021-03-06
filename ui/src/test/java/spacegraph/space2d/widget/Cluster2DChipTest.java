package spacegraph.space2d.widget;

import spacegraph.space2d.widget.chip.Cluster2DChip;
import spacegraph.space2d.widget.chip.NoiseVectorChip;
import spacegraph.space2d.widget.windo.GraphEdit;

class Cluster2DChipTest {

    public static void main(String... args) {
        GraphEdit w = GraphEdit.window(1000, 1000);
        w.add(new Cluster2DChip()).pos(100, 100, 300, 300);
        w.add(new NoiseVectorChip()).pos(300, 300, 500, 500);

    }
}