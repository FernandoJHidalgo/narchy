package nars.term.compound;

import jcog.Util;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.transform.Retemporalize;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;


/**
 * on-heap, caches many commonly used methods for fast repeat access while it survives
 */
abstract public class CachedCompound extends SeparateSubtermsCompound implements The {

    /**
     * subterm vector
     */
    private final Subterms subterms;


    /**
     * content hash
     */
    protected final int hash;

    public final Op op;

    private final short _volume;
    private final int _structure;

    public static Compound newCompound(Op op, int dt, Subterms subterms) {
//        if (subterms instanceof DisposableTermList)
//            throw new WTF();
//        boolean hasTemporal = op.temporal || subterms.hasAny(Op.Temporal);
//        boolean isNormalized = subterms.isNormalized();


        Compound c;
        if (!op.temporal && !subterms.hasAny(Op.Temporal)) {
            assert (dt == DTERNAL);
            if (subterms.isNormalized())
                c = new SimpleCachedCompound(op, subterms);
            else
                c = new UnnormalizedCachedCompound(op, subterms);
        } else {
            c = new TemporalCachedCompound(op, dt, subterms);
        }

        return c;
    }

    /** non-temporal but unnormalized */
    public static class UnnormalizedCachedCompound extends CachedCompound {

        UnnormalizedCachedCompound(Op op, Subterms subterms) {
            super(op, DTERNAL, subterms);
        }

        @Override
        public final int dt() {
            return DTERNAL;
        }

        @Override
        public final Term temporalize(Retemporalize r) {
            return this;
        }

        @Override
        public final boolean hasXternal() {
            return false;
        }

        @Override
        public final int eventRange() {
            return 0;
        }

        @Override
        public final Term eventFirst() {
            return this;
        }

        @Override
        public final Term eventLast() {
            return this;
        }


        /** @see Term.eventsWhile */
        @Override public final boolean eventsWhile(LongObjectPredicate<Term> each, long offset, boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal) {
            return each.accept(offset, this);
        }


    }

    public final static class SimpleCachedCompound extends UnnormalizedCachedCompound {

        SimpleCachedCompound(Op op, Subterms subterms) {
            super(op, subterms);
        }

        @Override
        public final Term root() {
            return this;
        }

        @Override
        public final Term concept() {
            return this;
        }

        @Override
        public final boolean equalsRoot(Term x) {
            return equals(x) || equals(x.root());
        }
    }




    /**
     * caches a reference to the root for use in terms that are inequal to their root
     */
    public static class TemporalCachedCompound extends CachedCompound {

        protected final int dt;

        public TemporalCachedCompound(Op op, int dt, Subterms subterms) {
            super(op, dt, subterms);


            this.dt = dt;
        }

        @Override
        public int dt() {
            return dt;
        }

    }


    private CachedCompound(/*@NotNull*/ Op op, int dt, Subterms subterms) {

        int h = (this.subterms = subterms).hashWith(this.op = op);
        this.hash = (dt == DTERNAL) ? h : Util.hashCombine(h, dt);


        this._structure = subterms.structure() | op.bit;

        this._volume = (short) subterms.volume();
    }


    abstract public int dt();

    /**
     * since Neg compounds are disallowed for this impl
     */
    @Override
    public final Term unneg() {
        return this;
    }

    @Override
    public final int volume() {
        return _volume;
    }

    @Override
    public final int structure() {
        return _structure;
    }

    @Override
    public final Subterms subterms() {
        return subterms;
    }

    @Override
    public final int vars() {
        return hasVars() ? super.vars() : 0;
    }

    @Override
    public final int varPattern() {
        return hasVarPattern() ? subterms().varPattern() : 0;
    }

    @Override
    public final int varQuery() {
        return hasVarQuery() ? subterms().varQuery() : 0;
    }

    @Override
    public final int varDep() {
        return hasVarDep() ? subterms().varDep() : 0;
    }

    @Override
    public final int varIndep() {
        return hasVarIndep() ? subterms().varIndep() : 0;
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Override
    public final Op op() {
        return op;
    }

    @Override
    public final int opBit() {
        return op.bit;
    }

    @Override
    public final String toString() {
        return Compound.toString(this);
    }


    @Override
    public final boolean equals(@Nullable Object that) {
        return Compound.equals(this, that, true);
    }


}
