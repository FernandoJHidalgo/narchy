package nars.term.atom;

import nars.Op;
import nars.The;
import nars.term.Term;
import nars.unify.Unify;

import static nars.Op.BOOL;


/** special/reserved/keyword representing fundamental absolute boolean truth states:
 *      True - absolutely true
 *      False - absolutely false
 *      Null - absolutely nonsense
 *
 *  these represent an intrinsic level of truth that exist within the context of
 *  an individual term.  not to be confused with Task-level Truth
 */
abstract public class Bool extends AtomicConst implements The {

    private final String id;

    protected Bool(String id) {
        super(BOOL, id);
        this.id = id;
    }

    @Override
    abstract public boolean equalsNegRoot(Term t);

    @Override
    public final boolean equalsRoot(Term x) {
        return equals(x);
    }


    @Override
    public /*@NotNull*/ Op op() {
        return BOOL;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    abstract public Term unneg();

    @Override
    public final boolean equals(Object u) {
        return u == this;
    }

    @Override
    abstract public boolean equalsNeg(Term t);

    @Override
    public final Term concept() {
        //return Null;
        throw new UnsupportedOperationException();
    }


    @Override
    public final boolean unify(Term y, Unify subst) {
        return this == y;
    }

    @Override
    public final Term dt(int dt) {
        //return this;
        throw new UnsupportedOperationException();
    }






}
