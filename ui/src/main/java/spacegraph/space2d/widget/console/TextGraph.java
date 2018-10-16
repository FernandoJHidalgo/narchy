package spacegraph.space2d.widget.console;

import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Graph2D;
import spacegraph.space2d.widget.meta.ClassReloadingSurface;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.text.VectorLabel;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/** experimental text editor based on character-level graphical representation
 *  see: https://github.com/koo5/new_shit lemon
 */
public class TextGraph extends Graph2D { //extends Graph2D {

    public TextGraph() {
        super();
        render((Graph2DRenderer) (node, graph) -> node.set(new VectorLabel("x")));
        update((g,dt)->{
           //System.out.println(g.nodes());
        });
        add(List.of("x"));
    }


    public static void main(String[] args) throws ClassNotFoundException, IOException, URISyntaxException {

        ClassReloadingSurface c = new ClassReloadingSurface(TextGraph.class);
        SpaceGraph.window(new MetaFrame(c), 800, 800);

//        Loop.of(c::reload).setFPS(0.25f);

//        Class c0 = TextGraph.class;
//
//        ClassReloader r = ClassReloader.inClassPathOf(TextGraph.class); //"/home/me/n/ui/out/production/classes/");
//        //Class<?> c1 = r.loadClassAsReloadable(TextGraph.class);
//        Class<?> c1 = r.reloadClass(TextGraph.class);
//        System.out.println(c1);
//        System.in.read();
//        Class<?> c2 = r.reloadClass(TextGraph.class);
//        System.out.println(c2);
    }
}
