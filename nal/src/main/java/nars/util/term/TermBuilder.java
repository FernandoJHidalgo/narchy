package nars.util.term;

import jcog.WTF;
import nars.Op;
import nars.subterm.ArrayTermVector;
import nars.subterm.Neg;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.subterm.util.DisposableTermList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.anon.AnonVector;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

import static nars.Op.Null;
import static nars.time.Tense.DTERNAL;

/**
 * interface for term and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another term or True/False/Null
 */
public abstract class TermBuilder {

    public final Term compound(Op o, Term... u) {
        return compound(o, DTERNAL, u);
    }

    public final Term compound(Op o, int dt, Term[] u) {
        if (Op.hasNull(u))
            return Null;
        return newCompound(o, dt, o.sortedIfNecessary(dt, u));
    }

    protected Term resolve(Term x){
        return x;
    }

    abstract protected Term newCompound(Op o, int dt, Term[] u);


    public final Subterms newSubterms(Term... s) {
        return newSubterms(null, s);
    }

    abstract public Subterms newSubterms(@Nullable Op inOp, Term... s);


    public Subterms subterms(Collection<Term> s) {
        return newSubterms(s.toArray(Op.EmptyTermArray));
    }

    public Subterms subtermsInstance(Term... t) {
        final int tLength = t.length;
        if (tLength == 0)
            return Op.EmptySubterms;

        boolean purelyAnon = true;
        for (int i = 0; i < tLength; i++) {
            Term x = t[i];
            if (x instanceof EllipsisMatch)
                throw new RuntimeException("ellipsis match should not be a subterm of ANYTHING");

            if (purelyAnon) {
                if (!(x instanceof AnonID)) {
                    Term ux = x.unneg();
                    if (x != ux && ux instanceof AnonID) {
                        
                        
                    } else {
                        purelyAnon = false;
                    }
                }
            }
        }

        if (!purelyAnon) {
            switch (t.length) {
                case 0:
                    throw new UnsupportedOperationException();
                case 1:
                    
                    return new UnitSubterm(t[0]);
                
                
                default:
                    return new ArrayTermVector(t);
            }
        } else {
            return new AnonVector(t);
        }

    }



    public Term compoundInstance(Op o, int dt, Term[] u) {
        assert (!o.atomic) : o + " is atomic, with subterms: " + (u);

        boolean hasEllipsis = false;

        for (Term x : u) {
            if (!hasEllipsis && (x instanceof Ellipsislike))
                hasEllipsis = true;
//            if (x == Null)
//                return Null;
        }

        int s = u.length;
        assert (o.maxSize >= s) :
                "subterm overflow: " + o + ' ' + Arrays.toString(u);
        assert (o.minSize <= s || hasEllipsis) :
                "subterm underflow: " + o + ' ' + Arrays.toString(u);

        if (s == 1) {
            Term x = resolve(u[0]);
            switch (o) {
                case NEG:
                    return Neg.the(x);
                default:
                    return new CachedUnitCompound(o, x);
            }
        } else {
            return theCompound(o, dt, newSubterms(o, u));
        }
    }


    public Compound theCompound(Op op, Subterms subterms) {
        return theCompound(op, DTERNAL, subterms);
    }

    public Compound theCompound(Op op, int dt, Subterms subterms) {
        if (subterms instanceof DisposableTermList)
            throw new WTF();
        if (!op.temporal && !subterms.isTemporal()) {
//            if (dt!=DTERNAL) {
//                throw new WTF();
//            }
            assert(dt == DTERNAL);
            return new CachedCompound.SimpleCachedCompound(op, subterms);
        } else {
            return new CachedCompound.TemporalCachedCompound(op, dt, subterms);
        }
    }


}
