package nars.unify.unification;

import nars.term.Term;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.Unification;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * an individual solution
 */
abstract public class DeterministicUnification implements Unification {

    public DeterministicUnification() {
        super();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof DeterministicUnification)
            return equals((DeterministicUnification) obj);
        return false;
    }

    abstract protected boolean equals(DeterministicUnification obj);

    @Override
    public final int forkKnown() {
        return 1;
    }

    @Override
    public final Iterable<Term> apply(Term x) {
        return List.of(transform(x));
    }

    public final Term transform(Term x) {
        return AbstractTermTransform.transform(x, transform());
    }

    protected Unify.UnifyTransform.LambdaUnifyTransform transform() {
        return new Unify.UnifyTransform.LambdaUnifyTransform(this::xy);
    }

    @Nullable
    abstract public Term xy(Term t);

    /**
     * sets the mappings in a target unify
     * @return true if successful
     */
    public abstract boolean apply(Unify y);
}
