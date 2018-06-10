package nars.util.term.transform;

import jcog.Texts;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.util.TermList;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.var.UnnormalizedVariable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * I = input term type, T = transformable subterm type
 */
public interface TermTransform extends Evaluation.TermContext {
    /**
     * general pathway. generally should not be overridden
     */
    @Override
    default @Nullable Term apply(Term x) {
        return x.transform(this);
    }

    /**
     * transform pathway for atomics
     */
    default @Nullable Term transformAtomic(Atomic atomic) {
        //assert (!(atomic instanceof Compound));
        return atomic;
    }

    /**
     * transform pathway for compounds
     */
    default Term transformCompound(Compound x) {
        return transformCompound(x, x.op(), x.dt());
    }

    /**
     * should not be called directly except by implementations of TermTransform
     */
    @Nullable
    default Term transformCompound(Compound x, Op op, int dt) {

        Subterms xx = x.subterms();

        Subterms yy = xx.transformSubs(this);
        return yy == null ? Null : transformedCompound(x, op, dt, xx, yy);

    }

    /** called after subterms transform has been applied */
    @Nullable default Term transformedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
        Term y;
        if (yy != xx) {
            y = the(op, dt, (TermList)yy); //transformed subterms
        } else if (op != x.op()) {
            y = the(op, dt, xx); //same subterms
        } else {
            y = x.dt(dt);
        }

        if (eval()) {
            if ((op = y.op()) == INH) {
                yy = y.subterms();
                if (yy.subs() == 2 && yy.hasAll(Op.PROD.bit | Op.ATOM.bit)) {
                    Term pred;
                    if ((pred = yy.sub(1)) instanceof Functor.InlineFunctor) {
                        Term args = yy.sub(0);
                        if (args.op() == PROD) {
                            Term v = ((Functor.InlineFunctor) pred).applyInline(args);
                            if (v != null)
                                return v;

                        }
                    }
                }
            }
        }

        return y;
    }

    default boolean eval() {
        return true;
    }



    /**
     * constructs a new term for a result
     */
    default Term the(Op op, int dt, TermList t) {
        return the(op, dt, (Subterms)t);
    }

    default Term the(Op op, int dt, Subterms t) {
        return the(op, dt, t.arrayShared());
    }

    default Term the(Op op, int dt, Term[] subterms) {
        return op.compound(dt, subterms);
    }
    /**
     * change all query variables to dep vars by use of Op.imdex
     */
    TermTransform queryToDepVar = variableTransform(VAR_QUERY, VAR_DEP);
    TermTransform indepToDepVar = variableTransform(VAR_INDEP, VAR_DEP);

    private static TermTransform
    variableTransform(Op from, Op to) {
        return new TermTransform() {
            @Override
            public Term transformAtomic(Atomic atomic) {
                if (atomic.op() != from)
                    return atomic;
                else
                    return new UnnormalizedVariable(to, Texts.quote(atomic.toString()));
            }
        };
    }











    /**
     * operates transparently through negation subterms
     */
    interface NegObliviousTermTransform extends TermTransform {

        @Override
        @Nullable
        default Term transformCompound(Compound x) {
            Op op = x.op();
            if (op == NEG) {
                Term xx = x.unneg();
                Termed y = apply(xx);
                if (y == null)
                    return null;
                Term yy = y.term();
                if (yy.equals(xx))
                    return x; 
                else {
                    Term y2 = yy.neg(); 
                    if (y2.equals(x))
                        return x;
                    else
                        return y2;
                }
            } else {
                return transformCompoundUnneg(x);
            }

        }

        /** transforms a compound that has been un-negged */
        @Nullable default Term transformCompoundUnneg(Compound x) {
            return TermTransform.super.transformCompound(x);
        }
    }

}
