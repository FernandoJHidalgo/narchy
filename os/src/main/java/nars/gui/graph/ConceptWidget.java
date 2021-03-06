package nars.gui.graph;

import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.pri.PLink;
import jcog.pri.ScalarValue;
import jcog.util.Flip;
import nars.concept.Concept;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.SphereShape;
import spacegraph.space3d.widget.EDraw;
import spacegraph.space3d.widget.SpaceWidget;
import spacegraph.video.Draw;

import java.util.List;

import static jcog.math.v3.v;
import static nars.gui.graph.DynamicConceptSpace.ConceptVis2.TASKLINK;
import static nars.gui.graph.DynamicConceptSpace.ConceptVis2.TERMLINK;
import static org.eclipse.collections.impl.tuple.Tuples.twin;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;


public class ConceptWidget extends SpaceWidget<Concept> {

    public float pri;

    public final Flip<ConcurrentFastIteratingHashMap<Concept, ConceptEdge>> edges =
            new Flip<>(()->new ConcurrentFastIteratingHashMap<>(new ConceptEdge[0]));

    public ConceptWidget(Concept x) {
        super(x);


    }

    public static float r(float range) {
        return (-0.5f + (float) Math.random()) * 2f * range;
    }

    @Override
    protected CollisionShape newShape() {
        return id.op().atomic ? new SphereShape() : super.newShape() /* cube */;
    }

    @Override
    public Iterable<ConceptEdge> edges() {
        return edges.read().values();
    }

    @Override
    public Body3D newBody(boolean collidesWithOthersLikeThis) {
        Body3D x = super.newBody(collidesWithOthersLikeThis);

        final float initDistanceEpsilon = 50f;


        x.transform.set(
                r(initDistanceEpsilon),
                r(initDistanceEpsilon),
                r(initDistanceEpsilon));


        final float initImpulseEpsilon = 0.25f;
        x.impulse(v(
                r(initImpulseEpsilon),
                r(initImpulseEpsilon),
                r(initImpulseEpsilon)));

        return x;
    }


    public static class ConceptVis1 implements TermVis<ConceptWidget> {

        final float minSize = 0.1f;
        final float maxSize = 6f;


        @Override
        public void accept(List<ConceptWidget> pending) {
            pending.forEach(this::each);
        }

        public void each(ConceptWidget cw) {
            float p = cw.pri;
            p = (p == p) ? p : 0;


            float nodeScale = (float) (minSize + Math.sqrt(p) * maxSize);
            cw.scale(nodeScale, nodeScale, nodeScale);


            Draw.hsb(cw.shapeColor, (cw.id.op().ordinal() / 16f), 0.75f + 0.25f * p, 0.75f, 0.9f);
        }


    }

    static class EdgeComponent extends PLink<ObjectIntPair<Twin<ConceptWidget>>> {

        final int type;

        EdgeComponent(ConceptWidget src, ConceptWidget tgt, int type, float pri) {
            super(pair(twin(src, tgt), type), pri);
            this.type = type;
            pri(pri);
        }

        public ConceptWidget src() {
            return id.getOne().getOne();
        }

        public ConceptWidget tgt() {
            return id.getOne().getTwo();
        }

        @Override
        public boolean isDeleted() {
            if (!src().active() || !tgt().active()) {
                delete();
                return true;
            }
            return super.isDeleted();
        }

    }


    public static class ConceptEdge extends EDraw<ConceptWidget> {

        float termlinkPri, tasklinkPri;
        boolean inactive;
        final static float priTHRESH = ScalarValue.EPSILON;

        public ConceptEdge(ConceptWidget src, ConceptWidget target, float pri) {
            super(src, target, pri);
            inactive = false;
        }

        protected void decay(float rate) {


            termlinkPri *= rate;
            tasklinkPri *= rate;
        }


        public void add(float p, boolean termOrTask) {
            if (termOrTask) {
                termlinkPri += p;
            } else {
                tasklinkPri += p;
            }
        }


        @Override
        public boolean isDeleted() {
            boolean inactive = !src().active() || !tgt().active();
            if (inactive) {
                delete();
                return true;
            }
            return super.isDeleted();
        }

        public void merge(EdgeComponent e) {
            float p = e.priElseZero();
            if (p <= priTHRESH)
                return;

            switch (e.type) {
                case TERMLINK:
                    this.termlinkPri += p;
                    break;
                case TASKLINK:
                    this.tasklinkPri += p;
                    break;
            }
            priMax(p);
            inactive = false;
        }

    }


}
