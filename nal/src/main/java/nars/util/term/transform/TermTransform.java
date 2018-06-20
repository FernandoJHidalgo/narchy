package nars.util.term.transform;

import jcog.Texts;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.util.TermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.anon.AnonVector;
import nars.term.atom.Atomic;
import nars.term.compound.LazyCompound;
import nars.term.var.UnnormalizedVariable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * I = input term type, T = transformable subterm type
 */
public interface TermTransform {

    default Term transform(Term x) {
        return (x instanceof Compound) ?
                transformCompound((Compound)x)
                :
                transformAtomic((Atomic)x);
    }

    default boolean transform(Term x, LazyCompound out) {
        if (x instanceof Compound) {
            return transformCompound((Compound)x, out);
        } else {
            @Nullable Term y = transformAtomic((Atomic) x);
            if (y == null)
                return false;
            out.add(y);
            return true;
        }
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

        //Subterms yy = x.subterms(this /* op, dt, eval */);
        Subterms xx = x.subterms();
        Subterms yy = xx.transformSubs(this);

        return yy == null ? Null : transformedCompound(x, op, dt, xx, yy);
    }


//    /** default lazy implementation, doesnt offer any benefit by just calling the non-lazy */
//    default boolean transformCompound(Compound x, LazyCompound out) {
//        Term y = transformCompound(x, x.op(), x.dt());
//        if (y == null)
//            return false;
//
//        out.add(y);
//        return true;
//    }

    default boolean transformCompound(Compound x, LazyCompound out) {
        out.compound(x.op(), x.dt());
        return transformSubterms(x.subterms(), out);
    }

    default boolean transformSubterms(Subterms x, LazyCompound out) {
        out.subs((byte)x.subs());
        return x.AND(sub -> transform(sub, out));
    }

    /** called after subterms transform has been applied */
    @Nullable default Term transformedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
        Term y;
        //Subterms xx = x.subterms();
        if (yy != xx) {
            y = the(op, dt, ((TermList)yy).arraySharedKeep()); //transformed subterms
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
        return op.the(dt, subterms);
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
            Term xx = x.unneg();
            if (xx!=x) {
                Term yy = transform(xx);
                if (yy == null)
                    return null;
                if (yy==xx)
                    return x; 
                else {
                    return yy.neg();
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
