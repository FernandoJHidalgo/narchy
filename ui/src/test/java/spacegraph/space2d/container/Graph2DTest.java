package spacegraph.space2d.container;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import org.ujmp.core.Matrix;
import org.ujmp.core.util.matrices.SystemEnvironmentMatrix;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.layout.ForceDirected2D;

public class Graph2DTest {


    static final MapNodeGraph<Object,Object> h = new MapNodeGraph();
    static {
        h.addNode(("x"));
        h.addNode(("y"));
        h.addNode(("z"));
        h.addNode(("w"));

        h.addEdge(("x"), ("xy"), ("y"));
        h.addEdge(("x"), ("xz"), ("z"));
        h.addEdge(("y"), ("yz"), ("z"));
        h.addEdge(("w"), ("wy"), ("y"));
    }
    static class Graph2DTest1 {
        public static void main(String[] args) {



            Graph2D<Node<Object, Object>> sg = new Graph2D<Node<Object, Object>>()

                .update(new ForceDirected2D<>())

                .render(new Graph2D.NodeGraphRenderer<>())

                .set(h.nodes());


            SpaceGraph.window(sg, 800, 800);
        }

    }

    static class Ujmp1 {
        public static void main(String[] args) {


            MapNodeGraph<Object,Object> h = new MapNodeGraph();

            SystemEnvironmentMatrix env = Matrix.Factory.systemEnvironment();
            h.addNode("env");
            env.forEach((k,v)->{
                h.addNode(v);
                h.addEdge("env",k,v);
            });


            Graph2D<Node<Object, Object>> sg = new Graph2D<Node<Object, Object>>()

                    .update(new ForceDirected2D())

                    .render(new Graph2D.NodeGraphRenderer())

                    .set(h.nodes());


            SpaceGraph.window(sg, 800, 800);
        }

    }

}
