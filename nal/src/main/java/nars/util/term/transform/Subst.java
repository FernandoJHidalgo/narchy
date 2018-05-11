package nars.util.term.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;


public interface Subst extends TermTransform {


    /** completely dereferences a term (usually a variable)*/
    default Term resolve(final Term x) {
        Term y, z = x;
        while ((y = xy(z))!=null) {
            assert(y!=z && y!=x);
            z = y;
        }
        return z;
    }

    @Override @Nullable
    default Termed transformAtomic(Term atomic) {
        Term y = resolve(atomic);
        return y != null ? y : atomic;
    }

    @Override @Nullable
    default Term transformCompound(Compound x) {
        Term y = xy(x);
        return y != null ? y : TermTransform.super.transformCompound((Compound)x);
    }

    /**
     * can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations
     */
    boolean isEmpty();

    /**
     * the assigned value for x
     */
    @Nullable Term xy(Term t);


    void clear();


}
