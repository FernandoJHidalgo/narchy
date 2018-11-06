package nars.gui.graph.run;

import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.gui.NARui;
import nars.term.ProxyTerm;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.layout.ForceDirected2D;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.*;

/** TODO edge capacity limiting */
public class ConceptGraph2D extends Graph2D<Concept> {

    private final NAR nar;
    private DurService on;

    Iterable<Concept> source;


    public class Controls {
        public final AtomicBoolean update = new AtomicBoolean(true);
    }

    public final Controls controls = new Controls();

    public ConceptGraph2D(Iterable<Concept> source, NAR n) {
        super();

        this.nar = n;
        this.source = source;

        build((nn)->{
           nn.set(
               new Scale(
                   new PushButton(new VectorLabel(nn.id.toString()))
                       .click(()-> {
                           Concept t = nn.id;
                           if (t!=null)
                            NARui.conceptWindow(t, nar);
                       }),
                0.8f
               )
           );
        });

        this.update(getLayout())
//        layout(new TreeMap2D<>() {
//            @Override
//            public void layout(Graph2D<Concept> g, int dtMS) {
//
//                g.forEachValue(nn -> {
//                    if (nn.showing())
//                        updateNode(nn);
//                });
//                super.layout(g, dtMS);
//            }
//        })
                .render(
                        Graph2D.InvalidateEdges,
  //                      new TermlinkVis(n),
                        new SubtermVis(n),
                        new TasklinkVis(n),
                        new StatementVis(n)
                );
    }


    public Graph2DUpdater<Concept> getLayout() {
        return new ForceDirected2D<>() {

            @Override
            public void init(Graph2D<Concept> g, NodeVis<Concept> newNode) {
                updateNode(newNode);
                super.init(g, newNode);
            }

            @Override
            public void update(Graph2D<Concept> g, int dtMS) {

                g.forEachValue(nn -> {
                        updateNode(nn);
                });

                super.update(g, dtMS);

            }
        };
    }

    void updateNode(NodeVis<Concept> nn) {
        Concept i = nn.id;
        if (i!=null && nn.visible()) {

            float pri = Math.max(nar.attn.pri(i, 0f), ScalarValue.EPSILON);

            nn.color(pri, pri / 2f, 0f);

            nn.pri = pri;
        }
    }

    /** updates the source */
    public ConceptGraph2D source(Iterable<Concept> source) {
        this.source = source;
        return this;
    }



    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            on = DurService.on(nar, this::commit);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            on.off();
            on = null;
            return true;
        }
        return false;
    }

    protected void commit() {
        if (showing() && controls.update.get()) {
            set(source);
        }
    }






    final static float WEIGHT_UPDATE_RATE = 0.1f;
    final static float COLOR_UPDATE_RATE = 0.5f;
    final static float COLOR_FADE_RATE = 0.05f;

//    private static class TermlinkVis implements Graph2DRenderer<Concept> {
//        public final AtomicBoolean termlinks = new AtomicBoolean(true);
//        final NAR n;
//
//        private TermlinkVis(NAR n) {
//            this.n = n;
//        }
//
//        @Override
//        public void node(NodeVis<Concept> node, GraphEditing<Concept> graph) {
//            if (!termlinks.get())
//                return;
//
//            Concept id = node.id;
//            if (id!=null) {
//                id.termlinks().forEach(l -> {
//                    Graph2D.EdgeVis<Concept> e = graph.edge(node, wrap(l.get()));
//                    if (e != null) {
//                        float p = l.priElseZero();
//                        e.weightLerp(p, WEIGHT_UPDATE_RATE)
//                                .colorLerp((0.9f * p) + 0.1f, Float.NaN, Float.NaN, COLOR_UPDATE_RATE)
//                                .colorLerp(Float.NaN,0,0,COLOR_FADE_RATE)
//                        ;
//
//                    }
//                });
//            }
//        }
//    }

    /** bad HACK to avoid a term/concept equality issue */
    @Deprecated public static ProxyTerm wrap(Term l) {
        return new ProxyTerm(l);
    }

    private static class TasklinkVis implements Graph2D.Graph2DRenderer<Concept> {
        public final AtomicBoolean tasklinks = new AtomicBoolean(true);
        final NAR n;



        private TasklinkVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphEditing<Concept> graph) {
            if (!tasklinks.get())
                return;
            Concept n = node.id;
            if (n==null)
                return;

//            Term sourceTerm = n.term();
            n.tasklinks().forEach(l -> {

                Term targetTerm = l.term();
//                if (targetTerm.equals(sourceTerm.term()))
//                    return; //ignore

                Graph2D.EdgeVis<Concept> e = graph.edge(node, wrap(targetTerm));
                if (e != null) {
                    float p = l.priElseZero();
                    e.weightLerp(p, WEIGHT_UPDATE_RATE);
                    int r, g, b;
                    /*
                    https://www.colourlovers.com/palette/848743/(_%E2%80%9D_)
                    BELIEF   Red     189,21,80
                    QUESTION Orange  233,127,2
                    GOAL     Green   138,155,15
                    QUEST    Yellow  248,202,0
                    */
                    switch (l.punc()) {
                        case BELIEF: r = 189; g = 21; b = 80; break;
                        case QUESTION: r = 233; g = 127; b = 2; break;
                        case GOAL: r = 138; g = 155; b = 15; break;
                        case QUEST: r = 248; g = 202; b = 0; break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    e//.colorLerp(0,0,0,COLOR_FADE_RATE).colorAdd
                     .colorLerp
                             (r/256f, g/256f, b/256f, COLOR_UPDATE_RATE);


                }

            });

        }
    }

    private class SubtermVis implements Graph2D.Graph2DRenderer<Concept> {
        public final AtomicBoolean subterms = new AtomicBoolean(true);

        public final FloatRange strength = new FloatRange(0.1f, 0, 1f);

        final NAR n;

        private SubtermVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphEditing<Concept> graph) {
            if (!subterms.get())
                return;
            Concept n = node.id;
            if (n==null)
                return;

            float p = strength.floatValue();

            int v = n.volume();
            n.linker().targets().forEach(s -> {
                if (s.op().conceptualizable) {
                    Concept t = nar.concept(s);
                    if (t!=null) {
                        @Nullable EdgeVis<Concept> e = graph.edge(n, t);
                        if (e!=null) {
                            e.weightLerp(p, WEIGHT_UPDATE_RATE)
                                    .colorAdd(p, p, p, COLOR_UPDATE_RATE/v)
                                    .colorLerp(0,0,0,COLOR_FADE_RATE/v);
                        }
                    }
                }
            });



        }
    }


    private static class StatementVis implements Graph2DRenderer<Concept> {
        public final AtomicBoolean statements = new AtomicBoolean(true);
        final NAR n;

        private StatementVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphEditing<Concept> graph) {

            if (!statements.get())
                return;

            Concept t = node.id;

            if (t!=null && t.op().statement) {

                @Nullable EdgeVis<Concept> e = graph.edge((t.sub(0)), (t.sub(1)));
                if (e != null) {
                    float p = 0.5f;
                    e.weightLerp(p, WEIGHT_UPDATE_RATE)
                            .colorLerp(Float.NaN, Float.NaN, (0.9f * p) + 0.1f, COLOR_UPDATE_RATE)
                            .colorLerp(0,0,Float.NaN,COLOR_FADE_RATE)
                    ;
                }

            }

        }
    }

}
