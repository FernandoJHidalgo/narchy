package nars.term.util.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

@FunctionalInterface
public interface Retemporalize extends TermTransform.NegObliviousTermTransform {


    Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
//    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);


    /**
     * un-temporalize
     */
    Retemporalize root = new Retemporalize() {

        @Override
        public Term transformTemporal(Compound x, int dtNext) {
            Op xo = x.op();
            return Retemporalize.super.transformCompound(x, xo, xo.temporal ? XTERNAL : DTERNAL); // && dtNext == DTERNAL ? XTERNAL : DTERNAL);

            //Term y = Retemporalize.super.transformTemporal(x, dtNext);
            //return y != x ? xternalIfNecessary(x, y, dtNext) : x;
        }

        @Override
        public int dt(Compound x) {
            return x.op().temporal ? XTERNAL : DTERNAL;
        }
    };


    int dt(Compound x);

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
        int xdt = x.dt();
        if (xdt == dtNext && !requiresTransform(x.subterms()))
            return x;
        else {
            Op xo = x.op();
            int n = xo.temporal ? dtNext : DTERNAL;
            if (n == xdt)
                return NegObliviousTermTransform.super.transformCompound(x); //fast fail if dt doesnt change
            else {
                return NegObliviousTermTransform.super.transformCompound(x, xo, n);
            }
        }
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

