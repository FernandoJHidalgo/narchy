package nars.concept.dynamic;

import jcog.Util;
import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {


    @Nullable
    public DynTruth eval(Term superterm, boolean beliefOrGoal, long start, long end, boolean stamp, NAR n) {

        int DT = superterm.dt();
        assert(DT!=XTERNAL);

        Term[] inputs = components(superterm), outputs = null;
        assert (inputs.length > 0) : this + " yielded no dynamic components for superterm " + superterm;

        DynTruth d = new DynTruth(superterm, stamp ? new FasterList(inputs.length) : null);
        d.freq = d.conf = 1f;

        final float confMin = 0; //n.confMin.floatValue();

        int dur = n.dur();
        boolean evi = d.e != null;

        for (int i = 0; i < inputs.length; i++) {
            Term it = inputs[i];

            boolean negated = it.op() == Op.NEG;
            if (negated)
                it = it.unneg();

            Concept subConcept = n.concept(it);
            if (subConcept == null)
                return null; //ok just missing

            int dt;
            if (superterm.op() == CONJ) {
                dt = superterm.subTimeSafe(it);
//                if (dt == DTERNAL) {
//                    if (sdt != DTERNAL && sdt != XTERNAL) {
//                        return null; //dt = 0; //TODO maybe this should never happen, and if it does there is an error
//                    }
//                }
            } else {
                dt = DTERNAL; //
            }


            //TODO check these times
            long subStart, subEnd;
            if (start == ETERNAL) {
                subStart = subEnd = ETERNAL;
            } else {
                if (dt == DTERNAL) {
                    subStart = subEnd = start;
                } else {
                    subStart = start + dt;
                    subEnd = end + dt + it.dtRange();
                }
            }



            Task bt;
            Term ot;
            Truth nt;
            BeliefTable table = (BeliefTable) ((BaseConcept) subConcept).table(beliefOrGoal ? BELIEF : GOAL);
            if (evi) {
                //task

                bt = table.match(subStart, subEnd, it, n);
                if (bt == null)
                    return null;
                nt = bt.truth(subStart, subEnd, dur, 0f); //project to target time if task isnt at it
                if (nt == null)
                    return null;

                ot = bt.term();
                if (ot.hasXternal())
                    throw new RuntimeException("xternal");
            } else {
                //truth only
                bt = null;
                nt = table.truth(subStart, subEnd, n);
                if (nt == null)
                    return null;
                ot = null;
            }

            if (!add(i, d, nt.negIf(negated), confMin))
                return null;

            if (evi) {
                d.e.add(bt);

                if (!inputs[i].equals(ot)) {
                    //template has changed
                    if (outputs == null)
                        outputs = inputs.clone();
                    outputs[i] = ot;
                }
            }
        }

        if (evi) {
            if (d.e.isEmpty())
                throw new RuntimeException("no evidence");
            if (outputs != null) {
                d.concrete = superterm.op().the(DT, outputs);
            } else {
                d.concrete = superterm;
            }
            if (d.concrete.hasXternal()) {
                //eval(superterm, beliefOrGoal, start, end, stamp, n); //HACK
                throw new RuntimeException("xternal");
            }
        }

//        //if (template instanceof Compound) {
        return commit(d);
    }


    /**
     * override for postprocessing
     */
    protected DynTruth commit(DynTruth d) {
        return d;
    }

    abstract public  Term[] components(Term superterm);

    //protected abstract DynTruth eval(Compound template, long when, boolean stamp, NAR n);

    protected abstract boolean add(int subterm, DynTruth d, Truth t, float confMin);


    /**
     * conf is multiplied, freq is OR'd
     */
    public static class Union extends DynamicTruthModel.Intersection {

        public Union( Term... comp) {
            super(comp);
            assert (comp.length > 1);
        }

        @Override
        protected float f(float freq) {
            return 1f - freq;
        }

        @Override
        protected DynTruth commit(DynTruth d) {
            d.freq = 1f - d.freq;
            return d;
        }
    }

    public static class Intersection extends DynamicTruthModel {

        /** for use with conjunctions whose subterms may change as its DT changes */
        public final static Intersection conj = new Intersection(null);
        
        private final Term[] comp;

        public Intersection( Term... comp) {
            this.comp = comp;
        }

        @Override
        protected final boolean add(int subterm, DynTruth d, Truth truth, float confMin) {

            float f0 = d.freq;
            d.freq *= f(truth.freq());

            //HACK for subterms beyond 2, if the frequency has not changed, do not decrease confidence
            if (subterm < 2 || !Util.equals(f0, d.freq, Param.TRUTH_EPSILON)) {
                d.conf *= truth.conf();
                if (d.conf < confMin)
                    return false;
            }


            return true;

        }

        protected float f(float freq) {
            return freq;
        }


        
        @Override
        public Term[] components(Term superterm) {
            return comp!=null ? comp : superterm.subterms().arrayShared();
        }

    }

    public static class Difference extends DynamicTruthModel {
        private final Term[] components;

        public Difference(Term[] xy) {
            super();

//            assert (!(xy[0] instanceof Bool) && !(xy[1] instanceof Bool));
//            assert (!(xy[0] instanceof Variable) && !(xy[1] instanceof Variable)) :
//                    xy[0] + " or " + xy[1] + " is a variable";

            this.components = xy;
        }

        public Difference(Term x, Term y) {
            this(new Term[]{x, y});
        }

        
        @Override
        public Term[] components(Term superterm) {
            return components;
        }

        @Override
        protected boolean add(int subterm, DynTruth d, Truth t, float confMin) {
            float c = t.conf();
            if (subterm == 0) {
                if (c < confMin)
                    return false;
                d.conf = c;
                d.freq = t.freq();
            } else {

                if ((d.conf *= c) < confMin)
                    return false;

                d.freq *= (1f - t.freq());

//                if (t.conf() * tx.conf() < confMin) //early termination check
//                    return false;
            }

            return true;
        }
    }

    public static class Identity extends DynamicTruthModel {

        
        private final Term[] components;

        public Identity(Compound proxy, Compound base) {
            this.components = new Term[]{base};
        }

        
        @Override
        public Term[] components(Term superterm) {
            return components;
        }

        @Override
        protected boolean add(int subterm,  DynTruth d,  Truth t, float confMin) {
            float c = t.conf();
            if (c >= confMin) {
                d.conf = c;
                d.freq = t.freq();
                return true;
            }
            return false;
        }
    }
}