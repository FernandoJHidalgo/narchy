package nars.op.data;

import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Int;
import nars.term.atom.Intlike;

public class intersect extends Functor.BinaryFunctor {

    public static final intersect the = new intersect();

    intersect() {
        super("intersect");
    }

    @Override
    public boolean validOp(Op o) {
        return o.commutative;
    }

    @Override
    public Term apply(Term a, Term b) {
//        Op aop = a.op();
//        if (b.op() == aop)

        if (a instanceof Int.IntRange && b instanceof Intlike) {
           return ((Int.IntRange)a).intersect(b);
        }

            return Terms.intersect(a.op(), a.subterms(), b.subterms());
//        else
//            return null;
    }


}
