package nars.gui.graph.run;

import jcog.bag.util.Bagregate;
import jcog.math.MultiStatistics;
import jcog.meter.event.CSVOutput;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import nars.control.Activate;
import nars.exe.FocusedExecutioner;
import nars.gui.NARChart;
import nars.gui.graph.DynamicConceptSpace;
import nars.gui.graph.EdgeDirected;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.gui.Vis.reflect;
import static spacegraph.layout.Grid.col;

public class SimpleConceptGraph1 extends DynamicConceptSpace {

    final AtomicBoolean atomsEnabled = new AtomicBoolean(true);

    public SimpleConceptGraph1(NAR nar, @NotNull Iterable<Activate> concepts, int maxNodes, int bufferedNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super(nar, concepts, maxNodes, bufferedNodes, maxEdgesPerNodeMin, maxEdgesPerNodeMax);
    }

    @Override
    protected boolean include(Term x) {
        return atomsEnabled.get() || !(x instanceof Atomic);
//                return term instanceof Compound &&
//                        term.complexity()==3 && term.toString().endsWith("-->x)");
    }

    public static class TaskTreeChart extends NARChart<ITask> {

        public TaskTreeChart(@NotNull Iterable<ITask> b, int limit, NAR nar) {
            super(new Bagregate(b, limit, 1f), limit, nar);
        }

        @Override
        public void accept(ITask x, ItemVis<ITask> y) {
            float p = x.priElseZero();
            y.update(p, 0.25f + 0.5f * p, 0.25f, 0.25f);
        }
    }

    public static void main(String[] args) throws Narsese.NarseseException, FileNotFoundException {

        Param.DEBUG = true;
        //Param.TRACE = true;
        NAR n = NARS.tmp();
        //n.DEFAULT_BELIEF_PRIORITY = 0.1f;
        //n.DEFAULT_QUESTION_PRIORITY = 0.1f;
        float fps = 1f;

        //csvPriority(n, "/tmp/x.csv");

//        Default n = O.of(new Default.DefaultTermIndex(512, new NARS.ExperimentalConceptBuilder()),
//                new CycleTime(), new BufferedSynchronousExecutor(64, 0.5f)).the(Default.class);

        //Default n = NARBuilder.newMultiThreadNAR(1, new RealTime.DSHalf(true).durSeconds(0.05f));
        //n.nal(1);
//        n.termVolumeMax.setValue(7f);
//        n.DEFAULT_BELIEF_PRIORITY = 0.9f;
//        n.DEFAULT_GOAL_PRIORITY = 0.9f;
//        n.DEFAULT_QUESTION_PRIORITY = 0.01f;
//        n.DEFAULT_QUEST_PRIORITY = 0.01f;

//        n.inputAt(1, "c:a?");
//        n.inputAt(2, "b:a.");
//        n.inputAt(3, "c:b.");

        //new DeductiveChainTest(n, 8,  2048, inh);
        //n.mix.stream("Derive").setValue(0.005f); //quiet derivation
        //n.focus.activationRate.setValue(0.05f);


//                "(x:a ==> x:b).",
//                "(x:b ==> x:c).",
//                "(x:c ==> x:d).",
//                "(x:d ==> x:e).",
//                "(x:e ==> x:f)."
//                "(x:f ==> x:g).",
//                "(x:g ==> x:h)."

//        for (int i = 0; i < 10; i++) {
//            n.inputAt(i * 5 , i % 2 == 0 ? "x:c! :|:" : "--x:c! :|:");
//        }


//        SpaceGraph.window(
//            new TaskTreeChart(Iterables.transform(te.active, (CLink<ITask> x)-> x), 32, n),
//            500, 500
//        );

        SimpleConceptGraph1 cs = new SimpleConceptGraph1(n,
                () -> (((FocusedExecutioner) (n.exe)).concepts)
                        .stream()
                        .map(x -> x instanceof Activate ? ((Activate)x) : null)
                        .filter(Objects::nonNull)
                        .iterator()
                /* TODO */, 64, 64, 7, 15);


        SpaceGraph<Term> s = new SpaceGraph<>(

                cs.with(
//                        new SpaceTransform<Term>() {
//                            @Override
//                            public void update(SpaceGraph<Term> g, AbstractSpace<Term, ?> src, float dt) {
//                                float cDepth = -9f;
//                                src.forEach(s -> {
//                                    ((SimpleSpatial)s).moveZ(
//                                            s.key.volume() * cDepth, 0.05f );
//                                });
//                            }
//                        }

                        //new Flatten()
//                        new Flatten() {
//                            protected void locate(SimpleSpatial s, v3 f) {
//                                f.set(s.x(), s.y(), 10 - ((Term) (s.key)).volume() * 1);
//                            }
//                        }


                        //new Spiral()
//                        //new FastOrganicLayout()
                )
        );

        EdgeDirected fd = new EdgeDirected();
        s.dyn.addBroadConstraint(fd);

        //s.ortho(Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f));


        //Vis.conceptsWindow2D
        s

                //.add(new ZoomOrtho(logConsole(n, 120, 40, new FloatParam(0.25f)).opacity(0.5f)))
                .camPos(0, 0, 90)
                //.ortho( logConsole(n, 40, 10, 0.0f) )
                .show(1300, 900);

        SpaceGraph.window(
                col(
                        reflect(cs),
                        reflect(cs.vis),
                        reflect(fd),
                        reflect(n.exe)
                        //new CheckBox("Atoms", atomsEnabled),
                        //reflect( new CycleView(n) ),
//                        new PushButton("+", () -> {
//                            try {
//                                n.input("x:h! :|:");
//                            } catch (Narsese.NarseseException e) {
//                                e.printStackTrace();
//                            }
//                        }),
//                        new PushButton("-", () -> {
//                            try {
//                                n.input("--x:h! :|:");
//                            } catch (Narsese.NarseseException e) {
//                                e.printStackTrace();
//                            }
//                        })
                ),
                400, 400);

        //n.log();
        //n.loop(2f);

//        for (int i = 1; i < 24; i++)
//            n.inputAt(i*2,"(" + ((char)('a' + i)) + "). :|:");

        //new DeductiveMeshTest(n, new int[]{3, 2}, 16384);
        n.log();
        n.input(
                "(a-->b).", "(b-->c).","(c-->d)."

                //"$.50 at(SELF,{t001}). :|: %1.0;.90%", "$.70 (at(SELF,{t001}) &&+5 open({t001}))! %1.0;.90%" // //goal_ded_2
        );
        //n.log();

        //n.onCycle(nn->{System.out.println(nn.time() + "\n" + n.exe.stats() + "\n\n");});

        n.startFPS(fps).join();
        //n.run(600);



        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        //n.linkFeedbackRate.setValue(0.05f);


        //Param.DEBUG = true;
//        n
//                //.log()
//                //.logSummaryGT(System.out, 0.05f)
//                .input(
////                        "((parent($X,$Y) && parent($Y,$Z)) <=> grandparent($X,$Z)).",
////                        "parent(c, p).",
////                        "parent(p, g).",
////                        "grandparent(p, #g)?"
//                        "x:(a,(b,c))."
////                        "$0.9;0.9;0.9$ (a,(b,(c,(d,e))))."
////
//                );
//                //.run(800);
//


    }

    public static void csvPriority(NAR n, String path) throws FileNotFoundException {

        CSVOutput csv = new CSVOutput(
                //new File(path),
                System.out,
                "time", "ALL_Pri", "NAL_Pri", "ConceptFire_Pri", "busy");

        MultiStatistics<ITask> exeTasks = new MultiStatistics<ITask>()
                .classify("NALTask", x -> x instanceof NALTask)
                .classify("ConceptFire", x -> x instanceof Activate)
                ;
        n.onCycle((nn)->{
            n.exe.forEach(exeTasks::accept);
            csv.out(
                    nn.time(),

                    ((MultiStatistics.BooleanClassifierWithStatistics)exeTasks.cond.get(0)).getSum(),

                    ((MultiStatistics.BooleanClassifierWithStatistics)exeTasks.cond.get(1)).getSum(),

                    ((MultiStatistics.BooleanClassifierWithStatistics)exeTasks.cond.get(2)).getSum(),

                    nn.emotion.busyVol.getSum()
            );
            exeTasks.clear();
        });
    }

}
