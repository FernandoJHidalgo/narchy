package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.math.MutableEnum;
import jcog.sort.SortedArray;
import jcog.sort.TopN;
import jcog.tree.rtree.rect.RectFloat;
import nars.NAR;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.exe.NARLoop;
import nars.exe.UniExec;
import nars.exe.UniExec.TimedLink;
import nars.time.clock.RealTime;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.TreeMap2D;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.EnumSwitch;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.LoopPanel;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.video.Draw;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static spacegraph.SpaceGraph.window;
import static spacegraph.space2d.container.grid.Gridding.grid;
import static spacegraph.space2d.container.grid.Gridding.row;

public class ExeCharts {

    public static Surface metaGoalPlot(NAR nar) {

        int s = nar.causes.size();

        FloatRange gain = new FloatRange(1f, 0f, 5f);

        BitmapMatrixView bmp = new BitmapMatrixView(i ->
                Util.tanhFast(
                    gain.floatValue() * nar.causes.get(i).value()
                ),
                s, Draw::colorBipolar);

        return Splitting.column(DurSurface.get(bmp, nar), 0.05f, new FloatSlider("Display Gain", gain));
    }

    public static Surface metaGoalControls(NAR n) {
        CheckBox auto = new CheckBox("Auto");
        auto.on(false);

        float min = -1f;
        float max = +1f;

        float[] want = n.emotion.want;
        Gridding g = grid(


                IntStream.range(0, want.length).mapToObj(
                        w -> {
                            return new FloatSlider(want[w], min, max) {

                                @Override
                                protected void paintWidget(RectFloat bounds, GL2 gl) {
                                    if (auto.on()) {
                                        set(want[w]);
                                    }

                                }
                            }
                                    .text(MetaGoal.values()[w].name())
                                    .type(SliderModel.KnobHoriz)
                                    .on((s, v) -> {
                                        if (!auto.on())
                                            want[w] = v;
                                    });
                        }
                ).toArray(Surface[]::new));

        return g;
    }

    public static Surface exePanel(NAR n) {
        int plotHistory = 100;
        Plot2D exeQueue = new Plot2D(plotHistory, Plot2D.BarLanes)
                .add("queueSize", ((UniExec) n.exe)::queueSize);
        Plot2D busy = new Plot2D(plotHistory, Plot2D.BarLanes)
                .add("Busy", n.emotion.busyVol::getSum);
        return grid(
                DurSurface.get(exeQueue, n, exeQueue::update),
                DurSurface.get(busy, n, busy::update)
        );
    }

    public static Surface valuePanel(NAR n) {
        return row(
                metaGoalPlot(n),
                metaGoalControls(n)
        );
    }

    public static Surface taskBufferPanel(NAR n) {
        Plot2D plot = new Plot2D(256, Plot2D.Line).add("load", n.input::volume, 0, 1);
        return DurSurface.get(new Gridding(new ObjectSurface<>(n.input), plot), n, plot::update);
    }

    static class CausableWidget extends Widget {
        private final TimedLink c;
        private final VectorLabel label;

        CausableWidget(TimedLink c) {
            this.c = c;
            label = new VectorLabel(c.get().can.id);
            set(label);

        }

    }

    enum CauseProfileMode implements FloatFunction<TimedLink> {
        Pri() {
            @Override
            public float floatValueOf(TimedLink w) {
                return w.pri();
            }
        },
        Value() {
            @Override
            public float floatValueOf(TimedLink w) {
                return w.value;
            }
        },
        ValueRate() {
            @Override
            public float floatValueOf(TimedLink w) {
                return w.valueRate;
            }
        },
//        Time() {
//            @Override
//            public float floatValueOf(TimedLink w) {
//                return Math.max(0,w.time.get());
//            }
//        }
        ;
        /* TODO
                                //c.accumTimeNS.get()/1_000_000.0 //ms
                                //(c.iterations.getMean() * c.iterTimeNS.getMean())/1_000_000.0 //ms
                                //c.valuePerSecondNormalized
                                //c.valueNext
                                //c.iterations.getN()
                                //c...

                        //,0,1
         */
    }

    public static Surface causeProfiler(NAR nar) {
        SortedArray<TimedLink> cc = ((UniExec) nar.exe).cpu.items;
        int history = 128;
        Plot2D pp = new Plot2D(history,
                //Plot2D.BarLanes
                Plot2D.LineLanes
                //Plot2D.Line
        );

        final MutableEnum<CauseProfileMode> mode = new MutableEnum<>(CauseProfileMode.Pri);

        for (int i = 0, ccLength = cc.size(); i < ccLength; i++) {
            TimedLink c = cc.get(i);
            String label = c.get().toString();
            //pp[i] = new Plot2D(history, Plot2D.Line).addAt(label,
            pp.add(label, ()-> mode.get().floatValueOf(c));
        }

        Surface controls = new Gridding(
                EnumSwitch.newSwitch(mode, "Mode"),
                new PushButton("Print", ()-> {
                    Appendable t = TextEdit.out();
                    nar.exe.print(t);
                    window(t, 400, 400);
                }),
                new PushButton("Clear", ()->pp.series.forEach(Plot2D.Series::clear))
        );
        return DurSurface.get(Splitting.column(pp, 0.1f, controls), nar, pp::update);
    }

    public static Surface focusPanel(NAR nar) {

//        ForceDirected2D<UniExec.TimedLink> fd = new ForceDirected2D<>();
//        fd.repelSpeed.setAt(0.5f);

        Graph2D<TimedLink> s = new Graph2D<TimedLink>()
                .render((node, g) -> {
                    TimedLink c = node.id;

                    final float epsilon = 0.01f;
                    float p = Math.max(c.priElse(epsilon), epsilon);
                    float v = c.get().value();
                    node.color(p, v, 0.25f);


                    //Graph2D G = node.parent(Graph2D.class);
//                float parentRadius = node.parent(Graph2D.class).radius(); //TODO cache ref
//                float r = (float) ((parentRadius * 0.5f) * (sqrt(p) + 0.1f));

                    node.pri = Math.max(epsilon, p);
                })
                //.layout(fd)
                .update(new TreeMap2D<>())
                .build((node) -> {
                    node.set(new Scale(new CausableWidget(node.id), 0.9f));
                });


        return DurSurface.get(
                new Splitting(s,
                        new Gridding(new PushButton("Stats")
                                .click(()->causeSummary(nar, 10))
                                , s.configWidget()), 0.1f),
                nar, () -> {
                    s.set(((UniExec) nar.exe).cpu);
                });
    }

    private static void causeSummary(NAR nar, int top) {
        TopN[] tops = Stream.of(MetaGoal.values()).map(v -> new TopN<>(new Cause[top], (c) ->
                (float)c.credit[v.ordinal()].total)).toArray(TopN[]::new);
        nar.causes.forEach((Cause c) -> {
            for (TopN t : tops)
                t.add(c);
        });

        for (int i = 0, topsLength = tops.length; i < topsLength; i++) {
            TopN t = tops[i];
            System.out.println(MetaGoal.values()[i]);
            t.forEach(tt->{
                System.out.println("\t" + tt);
            });
        }

    }


    /**
     * adds duration control
     */
    static class NARLoopPanel extends LoopPanel {

        private final NAR nar;
        final IntRange durMS = new IntRange(1, 1, 1000);
        private final RealTime time;

        public NARLoopPanel(NARLoop loop) {
            super(loop);
            this.nar = loop.nar();
            durMS.set(nar.dur());
            if (nar.time instanceof RealTime) {
                time = ((RealTime) nar.time);
                add(
                        new IntSlider("Dur(ms)", durMS)
                                .on(durMS->nar.time.dur(Math.max((int)Math.round(durMS), 1))),
                        new FloatSlider("Throttle", loop.throttle)
                );
            } else {

                time = null;
            }
        }

        @Override
        public void update() {

            super.update();
            if (loop.isRunning()) {
            }

        }
    }

    public static Surface runPanel(NAR n) {
        BitmapLabel nameLabel;
        LoopPanel control = new NARLoopPanel(n.loop);
        Surface p = new Splitting(
                nameLabel = new BitmapLabel(n.self().toString()),
                control,
                false, 0.25f
        );
        return DurSurface.get(p, n, control::update);
    }


}
