package nars.unify;

import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Termed;
import nars.util.TimeAware;

import java.util.function.Predicate;

/**
 * not thread safe, use 1 per thread (do not interrupt matchAll)
 */
public class UnifySubst extends Unify {

    

    /*@NotNull*/
    public final TimeAware timeAware;


    final Predicate<Term> target;
    private Term a;
    int matches = 0;

    public UnifySubst(Op varType, /*@NotNull*/ TimeAware n, Predicate<Term> target, int ttl) {
        super(varType, n.random(), Param.UnificationStackMax, ttl);
        this.timeAware = n;
        this.target = target;
    }

    @Override
    public UnifySubst unify(/*@NotNull*/ Term x, /*@NotNull*/ Term y, boolean finish) {
        this.a = y;
        super.unify(x, y, finish);
        return this;
    }


    @Override
    public void tryMatch() {

        

        


        

        


        Termed aa = apply(a); 
        if (aa != null) {
            Term aaa = aa.term();
            if (aaa.op().conceptualizable) {
                matches++;
                if (!target.test(aaa)) {
                    stop();
                }
            }
        }
























        
    }

    public int matches() {
        return matches;
    }

}
