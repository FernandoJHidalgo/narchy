package nars.term.compound;

import nars.Op;
import nars.The;
import nars.term.Term;

import static nars.Op.*;


/** 1-element Compound impl */
public class CachedUnitCompound extends UnitCompound implements The {

    private final byte op;

    private final Term sub;

    /** hash including this compound's op (cached) */
    transient private final int chash;

    /** structure including this compound's op (cached) */
    transient private final int cstruct;
    private final short volume;


    public CachedUnitCompound(/*@NotNull*/ Op op, /*@NotNull*/ Term sub) {
        assert(op!=NEG); //makes certain assumptions that it's not NEG op, use Neg.java for that

        this.sub = sub;
        this.op = op.id;
        this.chash = super.hashCode();
        this.cstruct = super.structure();

        int v = sub.volume() + 1;
        assert(v < Short.MAX_VALUE);
        this.volume = (short) v;
    }

    @Override
    public final int volume() {
        return volume;
    }

    @Override
    public final Term unneg() {
        return this;
    }

    @Override
    public final Term sub() {
        return sub;
    }


    @Override
    public final int structure() {
        return cstruct;
    }


    @Override
    public final int hashCode() {
        return chash;
    }


    @Override
    public final /*@NotNull*/ Op op() {
        return Op.ops[op];
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > volume-1;
    }

    @Override
    public boolean isTemporal() {
        return hasAny(Op.Temporal) && sub.isTemporal();
    }

    @Override
    public boolean hasXternal() {
        return hasAny(Op.Temporal) && sub.hasXternal();
    }

    @Override
    public int varPattern() {
        return hasAny(Op.VAR_PATTERN) ? sub().varPattern() : 0;
    }

    @Override
    public int varDep() {
        return hasAny(Op.VAR_DEP) ? sub().varDep() : 0;
    }

    @Override
    public int varIndep() {
        return hasAny(Op.VAR_INDEP) ? sub().varIndep() : 0;

    }

    @Override
    public int varQuery() {
        return hasAny(Op.VAR_QUERY) ? sub().varQuery() : 0;
    }

    @Override
    public int vars() {
        return hasVars() ? sub().vars() : 0;
    }

    @Override
    public boolean hasVars() {
        return hasAny(VAR_PATTERN.bit | VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit);
    }
}
