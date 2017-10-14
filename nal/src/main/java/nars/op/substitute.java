package nars.op;

import nars.$;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;


public class substitute extends Functor {

    //TODO use special symbol encoding to avoid collision with equivalent normal input
    final static Term STRICT = Atomic.the("strict");
    final static Term INDEP_VAR = $.quote("$");
    final static Term DEP_VAR = $.quote("#");
    final static Term SAME = Atomic.the("same");


    public static final substitute the = new substitute();

    private substitute() {
        super((Atom) $.the("substitute"));
    }

    @Nullable @Override public Term apply( TermContainer xx) {

        final Term input = xx.sub(0); //term to possibly transform

        final Term x = xx.sub(1); //original term (x)

        boolean strict = xx.subEquals(3, STRICT);

        Term y = xx.sub(2); //replacement term (y)

        Term result;
        if (x.equals(y) || !input.containsRecursively(x)) {
            result = strict ? Null : input; //no change would be applied
        }else if (input.equals(x)) { //direct replacement
            result = y;
        } else {
            result = input.transform(new MapSubst1(x, y));
        }

        if (!(result instanceof Bool && !result.equals(input))) {
            //add mapping in parent
            if (!onChange(input, x, y, result))
                return Null;
        }

        return result;
    }

    /** called if substitution was successful */
    protected boolean onChange(Term from, Term x, Term y, Term to) {
        return true;
    }

}
