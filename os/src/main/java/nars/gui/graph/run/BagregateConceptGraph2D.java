package nars.gui.graph.run;

import jcog.math.IntRange;
import jcog.pri.bag.util.Bagregate;
import nars.NAR;
import nars.control.DurService;
import nars.term.Term;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;

public class BagregateConceptGraph2D extends ConceptGraph2D {

    private final Bagregate<Term> bag;

    public final IntRange maxNodes = new IntRange(256, 1, 512) {
        @Override
        public void changed() {
            if (bag!=null)
                bag.bag.setCapacity(intValue());
        }
    };

    public static nars.gui.graph.run.BagregateConceptGraph2D get(NAR n) {
        Bagregate<Term> b = new Bagregate(() -> n.conceptsActive().iterator(), 256, 0.5f);

        return new nars.gui.graph.run.BagregateConceptGraph2D(b, n) {
            private DurService updater;

            @Override
            protected void starting() {
                super.starting();
                updater = DurService.on(n, () -> {
                    if (visible())
                        b.commit();
                });
            }

            @Override
            public void stopping() {
                updater.off();
                updater= null;
                super.stopping();
            }
        };
    }

    private BagregateConceptGraph2D(Bagregate<Term> bag, NAR n) {
        super(bag.iterable(), n);
        this.bag = bag;
    }

    @Override
    protected void addControls(Gridding cfg) {
        cfg.add(new ObjectSurface(maxNodes));
    }
}
