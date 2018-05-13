package nars.gui.graph.run;

import jcog.data.graph.MapNodeGraph;
import nars.$;
import nars.time.TimeGraph;
import spacegraph.space2d.hud.SubOrtho;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.widget.SimpleGraph3D;
import spacegraph.space3d.widget.SpaceWidget;

import static nars.time.Tense.ETERNAL;
import static spacegraph.space2d.container.grid.Gridding.grid;

public class TestTimeGraphVis extends SimpleGraph3D<TimeGraph.Event> {

    public TestTimeGraphVis() {
        super((SpaceWidget<TimeGraph.Event> n)->{
            defaultVis.each(n);

            if (n.id instanceof TimeGraph.Absolute) {

            } else {
                n.color(0.5f, 0.5f, 0.5f, 0.5f);
            }
        });
    }

    static MapNodeGraph dt() {
        TimeGraph A = new TimeGraph();
        A.know($.$$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($.$$("one"), 1);
        A.know($.$$("two"), 20);
        A.solve($.$$("\"(one &&+- two)\""), false, (x)->true);
        return A;
    }
    public static void main(String[] args) {

        //NAR n = NARS.threadSafe();


        TestTimeGraphVis cs = new TestTimeGraphVis();


        SpaceGraphPhys3D sg = cs.show(1400, 1000, true);


        sg.add(new SubOrtho(grid(
                //new AutoSurface<>(sg.dyn.broadConstraints.get(0) /* FD hack */),
                new AutoSurface<>(cs.vis)
        )).posWindow(0, 0, 1f, 0.2f));

        cs.commit(dt(/*..*/));

//        {
//            MapNodeGraph<Object, Object> h = new MapNodeGraph<>();
//            h.addEdge(h.addNode("y"), "yx", h.addNode("x"));
//
//            ObjectGraph o = new ObjectGraph(h);
//            cs.commit(o);
//        }
    }

}
