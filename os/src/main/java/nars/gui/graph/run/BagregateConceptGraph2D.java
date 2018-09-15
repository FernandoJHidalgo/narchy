package nars.gui.graph.run;

import jcog.math.IntRange;
import jcog.pri.bag.util.Bagregate;
import nars.NAR;
import nars.control.DurService;
import nars.link.Activate;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;

public class BagregateConceptGraph2D extends ConceptGraph2D {

    private final Bagregate<Activate> bag;

    public final IntRange maxNodes = new IntRange(256, 1, 512) {
        @Override
        public void set(int value) {

            super.set(value);

            if (bag!=null) {
                synchronized (bag.bag) {
                    bag.bag.setCapacity(value);
                }
            }

        }
    };

    public static nars.gui.graph.run.BagregateConceptGraph2D get(NAR n) {
        Bagregate<Activate> b = new Bagregate(() -> n.conceptsActive().iterator(), 256, 0.001f);

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

    private BagregateConceptGraph2D(Bagregate<Activate> bag, NAR n) {
        super(bag.iterable(activate -> activate.id), n);
        this.bag = bag;
    }

    @Override
    protected void addControls(Gridding cfg) {
        cfg.add(new ObjectSurface(maxNodes));
    }
}
