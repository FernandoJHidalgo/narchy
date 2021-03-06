package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompound;
import nars.unify.ellipsis.EllipsisMatch;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

public interface TermTransform extends Function<Term,Term> {

    @Override default Term apply(Term x) {
        return (x instanceof Compound) ?
                applyCompound((Compound) x)
                :
                applyAtomic((Atomic) x);
    }

    default boolean apply(Term x, LazyCompound out) {
        if (x instanceof Compound) {
            return transformCompound((Compound) x, out);
        } else {
            @Nullable Term y = applyAtomic((Atomic) x);
            if (y == null || y == Bool.Null)
                return false;
            else {
                if (y instanceof EllipsisMatch) {
                    Subterms s = y.subterms();
                    if (s.subs() > 0) {
                        Subterms s2 = s.transformSubs(this, null);
                        if (s2 != s) {
                            if (s2 == null)
                                return false;
                            y = new EllipsisMatch(s2);
                        }
                    }
                }
                out.append(y);
                if (y != x)
                    out.setChanged(true);
                return true;
            }
        }
    }

    default Term applyAtomic(Atomic a) {
        return a;
    }
    default Term applyCompound(Compound c) { return c; }

    default boolean transformCompound(Compound x, LazyCompound out) {
        boolean c = out.changed();
        int i = out.pos();

        Op o = x.op();
        if (o == NEG) {

            out.negStart();

            if (!apply(x.sub(0), out))
                return false;

            out.compoundEnd(NEG);

        } else {
            out.compoundStart(o, o.temporal ? x.dt() : DTERNAL);

            if (!transformSubterms(x.subterms(), out))
                return false;

            out.compoundEnd(o);
        }

        if (!c && !out.changed()) {
            //remains same; rewind and paste as-is
            out.rewind(i);
            out.append((Compound)x);
        }
        return true;
    }

    default boolean transformSubterms(Subterms s, LazyCompound out) {
        out.subsStart((byte) s.subs());
        if (s.ANDwithOrdered(this::apply, out)) {
            out.subsEnd();
            return true;
        }
        return false;
    }

}
