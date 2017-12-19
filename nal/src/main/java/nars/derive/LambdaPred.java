package nars.derive;

import nars.$;
import nars.term.Term;
import nars.term.pred.AbstractPred;

import java.util.function.Predicate;


public final class LambdaPred<X> extends AbstractPred<X> {

    private final Predicate<X> test;

    public LambdaPred(Predicate<X> p) {
        this($.p($.the(p.toString())), p);
    }

    public LambdaPred(Term term, Predicate<X> p) {
        super(term);
        this.test = p;
    }

    @Override
    public boolean test(X x) {
        return test.test(x);
    }
}
