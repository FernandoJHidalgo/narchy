package nars.op;

import nars.subterm.Subterms;
import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;


/**
 * if STRICT is 4th argument, then there will only be a valid result
 * if the input has changed (not if nothing changed, and not if the attempted change had no effect)
 */
public class Subst extends Functor implements Functor.InlineFunctor {

    
    final static Term STRICT = Atomic.the("strict");


    public static final Subst replace = new Subst("replace");

    protected Subst(String id) {
        this((Atom)Atomic.the(id));
    }
    protected Subst(Atom id) {
        super(id);
    }

    @Nullable @Override public Term apply(Evaluation e, Subterms xx) {

        final Term input = xx.sub(0); 

        final Term x = xx.sub(1); 

        final Term y = xx.sub(2); 

        return apply(xx, input, x, y);
    }

    public @Nullable Term apply(Subterms xx, Term input, Term x, Term y) {
        Term result = input.replace(x, y);
        if (xx.subEquals(3, STRICT) && input.equals(result))
            return Null;







        return result;
    }






}
