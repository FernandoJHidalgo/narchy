package nars.term.compound;

import nars.IO;
import nars.Op;
import nars.Param;
import nars.nal.Tense;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.DTERNAL;


public class GenericCompound<T extends Term> implements Compound<T> {

    /**
     * subterm vector
     */
    @NotNull
    public final TermVector subterms;


    /**
     * numeric (term or "dt" temporal relation)
     */
    public final int dt;

    /**
     * content hash
     */
    public final int hash;

    @NotNull
    public final Op op;

    public transient boolean normalized;


    public GenericCompound(@NotNull Op op, @NotNull TermContainer subterms) {
        this(op, Tense.DTERNAL, subterms);
    }

    public GenericCompound(@NotNull Op op, int dt, @NotNull TermContainer _subterms) {

        TermVector subterms = (TermVector) _subterms; //HACK for future support of alternate TermContainer impls

        if (Param.DEBUG && dt != DTERNAL) {
            if (!((op.isImage() && ((dt >= 0) || (dt < subterms.size()))) ||
                    (Op.isTemporal(op, dt, subterms.size()))))
                throw new InvalidTermException(op, dt, subterms.terms(), "Invalid dt value for operator " + op);
        }

        this.subterms = subterms;

        this.normalized = (subterms.vars == 0) && (subterms.varPatterns == 0) /* not included in the count */;
        this.op = op;

        this.dt = dt;

        this.hash = Util.hashCombine(subterms.hash, op.ordinal(), dt);
    }

    @NotNull
    @Override
    public final Op op() {
        return op;
    }


    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }


    @NotNull
    @Override
    public final TermContainer subterms() {
        return subterms;
    }

    @Override
    public final boolean equals(@Nullable Object that) {

        if (this == that)
            return true;


        Compound cthat;
        if (that instanceof Compound) {
            cthat = (Compound)that;
        } else if (that instanceof Termed) {
            Term tthat = ((Termed) that).term();
            if (tthat instanceof Compound) {
                cthat = (Compound) tthat;
            } else {
                return false;
            }
        } else {
            return false;
        }

        if (hash != that.hashCode())
            return false;

        if (op == cthat.op())
            if (dt == cthat.dt())
                if (subterms.equals(cthat.subterms()))
                    return true;

        return false;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean isNormalized() {
        return normalized;
    }

    @Override
    public final int dt() {
        return dt;
    }


    /**
     * do not call this manually, it will be set by VariableNormalization only
     */
    public final void setNormalized() {
        this.normalized = true;
    }


}
