package nars.gui;

import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Narsese;
import nars.gui.graph.run.ConceptGraph2D;
import nars.term.Termed;
import nars.util.MemorySnapshot;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.KeyValueModel;
import spacegraph.space2d.container.grid.ScrollGrid;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.ConsoleTerminal;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.OmniBox;
import spacegraph.space2d.widget.meta.ServicesTable;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.util.math.Color3f;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;

/**
 * SpaceGraph-based visualization utilities for NARchy
 */
public class NARui {


    public static Surface inputEditor() {
        return new TextEdit(40, 8).surface();
    }

    public static Surface beliefCharts(int window, NAR nar, Object... x) {
        return beliefCharts(window, List.of(x), nar);
    }

    public static Surface beliefCharts(int window, Iterable ii, NAR nar) {
        BeliefChartsGrid g = new BeliefChartsGrid(ii, nar, window);
        return DurSurface.get(g, nar);
    }


    public static Surface bagHistogram(Iterable<? extends Prioritized> bag, int bins, NAR n) {


        float[] d = new float[bins];
        return DurSurface.get(new HistogramChart(
                        () -> d,
                        new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f)),


                n, () -> PriReference.histogram(bag, d));


    }


    public static Label label(Object x) {
        return label(x.toString());
    }

    public static Label label(String text) {
        return new Label(text);
    }

    /**
     * ordering: first is underneath, last is above
     */
    public static Stacking stack(Surface... s) {
        return new Stacking(s);
    }

    public static LabeledPane pane(String k, Surface s) {
        return new LabeledPane(k, s);
    }


    public static Surface top(NAR n) {
        return
                new Bordering(
                        new TabPane(Map.of(
                                "shl", () -> new ConsoleTerminal(new nars.TextUI(n).session(10f)),
                                "nar", () -> new AutoSurface<>(n),
                                "exe", () -> ExeCharts.exePanel(n),
                                "can", () -> ExeCharts.causePanel(n),
                                "grp", () -> new ConceptGraph2D(n).widget(),
                                "svc", () -> new ServicesTable(n.services),
                                "cpt", () -> bagHistogram((Iterable) () -> n.conceptsActive().iterator(), 8, n),
                                "snp", () -> memoryView(n),
                                "mem", () -> ScrollGrid.list(
                                        (x, y, m) -> new PushButton(m.toString()).click((mm) ->

                                                SpaceGraph.window(
                                                        ScrollGrid.list((xx, yy, zm) -> new PushButton(zm.toString()), n.memory.contents(m).collect(toList())), 800, 800, true)
                                        ),
                                        n.memory.roots().collect(toList())
                                )
                        ))
                )
                        .north(ExeCharts.runPanel(n))
                        .south(new OmniBox(new NarseseJShellModel(n)))
                ;
    }

    private static Surface memoryView(NAR n) {

        return new ScrollGrid<>(new KeyValueModel(new MemorySnapshot(n).byAnon),
                (x, y, v)-> {
                    if (x == 0) {
                        return new PushButton(v.toString()).click(() -> {

                        });
                    } else {
                        return new Label(((Collection)v).size() +  " concepts");
                    }
                });
    }

    public static void conceptWindow(String t, NAR n) {
        conceptWindow($$(t), n);
    }

    public static void conceptWindow(Termed t, NAR n) {
        SpaceGraph.window(new ConceptSurface(t, n), 500, 500, true);
    }


    public static class BeliefChartsGrid extends Gridding implements Consumer<NAR> {

        private final int window;

        long[] btRange;

        public BeliefChartsGrid(Iterable<?> ii, NAR nar, int window) {
            super();

            btRange = new long[2];
            this.window = window;

            List<Surface> s = StreamSupport.stream(ii.spliterator(), false)
                    .map(x -> x instanceof Termed ? (Termed) x : null).filter(Objects::nonNull)
                    .map(c -> new MetaFrame(new BeliefTableChart2(nar, c))).collect(toList());

            if (!s.isEmpty()) {
                set(s);
            } else {
                set(label("(empty)"));
            }

        }


        @Override
        public void accept(NAR nar) {
            long now = nar.time();
            int dur = nar.dur();
            btRange[0] = now - (window * dur);
            btRange[1] = now + (window * dur);
        }
    }

    static class NarseseJShellModel extends OmniBox.JShellModel {
        private final NAR nar;

        public NarseseJShellModel(NAR n) {
            this.nar = n;
        }

        @Override
        public void onTextChange(String text, int cursorPos, MutableContainer target) {
            super.onTextChange(text, cursorPos, target);
        }

        @Override
        public void onTextChangeControlEnter(String text, MutableContainer target) {
            text = text.trim();
            if (text.isEmpty())
                return;
            try {
                nar.input(text);
            } catch (Narsese.NarseseException e) {
                super.onTextChangeControlEnter(text, target);
            }
        }

    }
}
