package nars.term.subst;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.term.Term;

import java.util.function.Predicate;

/** not thread safe, use 1 per thread (do not interrupt matchAll) */
public class UnifySubst extends Unify {

    //static final Logger logger = LoggerFactory.getLogger(UnifySubst.class);

    /*@NotNull*/
    public final NAR nar;


    final Predicate<Term> target;
    private Term a;


    public UnifySubst(Op varType, /*@NotNull*/ NAR n, Predicate<Term> target, int ttl) {
        super(varType, n.random(), Param.UnificationStackMax, ttl);
        this.nar = n;
        this.target = target;
    }

    @Override
    public boolean unify(/*@NotNull*/ Term x, /*@NotNull*/ Term y, boolean finish) {
        this.a = x;
        return super.unify(x, y, finish);
    }




    @Override public void tryMatch() {

        //TODO combine these two blocks to use the same sub-method

        //try {


        //try {

        //        } catch (InvalidTermException e) {
//            return null;
//        }
            Term aa = a.transform(new MapSubst(xy));
            if (aa!=null)
                target.test(aa);


//        }
//        catch (InvalidTermException e) {
//            if (Param.DEBUG)
//                logger.warn("{}",e);
//
//        }

//        if ((aa == null) ||
//        //Op aaop = aa.op();
//        //only set the values if it will return true, otherwise if it returns false the callee can expect its original values untouched
//            ((a.op() == Op.VAR_QUERY) && (aa.op().in(Op.VarDepOrIndep)))
//         ) {
//            return false;
//        }

//        Term bb = applySubstituteAndRenameVariables(b, yx);
//        if (bb == null) return false;
//        Op bbop = bb.op();
//        if (bbop == Op.VAR_QUERY && (bbop == Op.VAR_INDEP || bbop == Op.VAR_DEP))
//            return false;


        //return matches < maxMatches; //determines how many
    }


}
