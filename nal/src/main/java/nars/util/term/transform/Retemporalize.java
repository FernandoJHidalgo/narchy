package nars.util.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import static nars.Op.IMPL;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

@FunctionalInterface
public interface Retemporalize extends TermTransform.NegObliviousTermTransform {

    
    
    Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);
    /**
     * un-temporalize
     */
    Retemporalize root = new Retemporalize() {

        @Override
        public boolean requiresTransform(Termlike x) {
            return x.isTemporal();
        }

        @Override
        public @Nullable Term transformCompound(Compound x, Op op, int dt) {



            throw new UnsupportedOperationException("apparently this is never called");
        }

        @Override
        public Term transformTemporal(Compound x, int dtNext) {
            Term y = Retemporalize.super.transformTemporal(x, dtNext);
            return y != x ? xternalIfNecessary(x, y, dtNext) : x;
        }

        Term xternalIfNecessary(Compound x, Term y, int dtNext) {
            if (corrupted(x, y, dtNext)) {
                
                return Retemporalize.super.transformCompound(x, x.op(), dtNext == DTERNAL ? XTERNAL : DTERNAL);
            }
            return y;
        }

        /** verifies events remain unchanged */
        private boolean corrupted(Term x, Term y, int dtNext) {

            if (y == null || y instanceof Bool || (y.dt() != dtNext))
                return true;

            Op xop;
            if ((xop = x.op()) != y.op())
                return true;

            if (x.subterms().equals(y.subterms()))
                return false; 

            switch (xop) {
                case CONJ:
                    return conjCorrupted(x, y);
                case IMPL:
                    return (y.op() != IMPL || x.structure() != y.structure() || x.volume() != y.volume());
                default:
                    return (x.subs() != y.subs()) || (y.volume() != x.volume()) || y.structure() != x.structure(); 
            }

        }

        private boolean conjCorrupted(Term x, Term y) {
            if (y.structure() != x.structure()) {
                return true;
            } else {

                switch (x.dt()) {
                    case 0:
                    case DTERNAL:
                    case XTERNAL:
                        return (x.subs() != y.subs() || x.volume() != y.volume());

                    default:



                        return recursiveEvents(x) != recursiveEvents(y);
                }
            }
        }

        int recursiveEvents(Term x) {
            return x.intifyShallow((s, t) -> {
                switch (t.op()) {
                    case CONJ:
                        return s + recursiveEvents(t); 
                    default:
                        return s + 1;
                }
            }, 0);
        }

        @Override
        public int dt(Compound x) {
            return DTERNAL;
        }
    };

    int dt(Compound x);

    @Override
    default boolean eval() {
        return false;
    }

    @Nullable
    @Override
    default Term transformCompound(final Compound x) {
        if (requiresTransform(x)) {
            return transformTemporal(x, dt(x));
        } else {
            return x;
        }
    }


    default Term transformTemporal(Compound x, int dtNext) {
        if (x.dt() == dtNext && !requiresTransform(x.subterms()))
            return x; 
        else
            return TermTransform.NegObliviousTermTransform.super.transformCompound(x, x.op(), dtNext);
    }

    /**
     * conditions on which recursive descent is required; this is the most general case.
     * some implementations will have more specific cases that can elide the
     * need for descent. ex: isTemporal() is narrower than x.hasAny(Op.Temporal)
     */
    default boolean requiresTransform(Termlike x) {
        return x.hasAny(Op.Temporal);
    }

    @Deprecated
    final class RetemporalizeAll implements Retemporalize {

        final int targetDT;

        public RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

        @Override
        public final int dt(Compound x) {
            return targetDT;
        }
    }

    final class RetemporalizeFromTo implements Retemporalize {

        final int from, to;

        public RetemporalizeFromTo(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int dt(Compound x) {
            int dt = x.dt();
            return dt == from ? to : dt;
        }
    }






























}


