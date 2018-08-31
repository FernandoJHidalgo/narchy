package nars.eval;

import jcog.data.set.ArrayHashSet;
import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.transform.DirectTermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * discovers functors within the provided term, or the term itself.
 * transformation results should not be interned, that is why DirectTermTransform used here
 */
public class Evaluator implements DirectTermTransform {

    public final Function<Atom, Functor> funcResolver;


//        public final MutableSet<Variable> vars = new UnifiedSet(0);

    protected Evaluator(Function<Atom, Functor> funcResolver) {
        this.funcResolver = funcResolver;
    }

    public Evaluator clone() {
        return new Evaluator(funcResolver);
    }


    @Nullable protected ArrayHashSet<Term> discover(Term x, Evaluation e) {
        if (!x.hasAny(Op.FuncBits))
            return null;
        ArrayHashSet<Term> funcAble = new ArrayHashSet(1);
        x.recurseTerms(s -> s.hasAll(Op.FuncBits), xx -> {
            if (!funcAble.contains(xx)) {
                if (Functor.isFunc(xx)) {
                    Term yy = this.transform(xx);
                    if (yy.sub(1) instanceof Functor) {
                        if (funcAble.add(yy)) {
                            ///changed = true;
                        }
                    }
                }
            }
            return true;
        }, null);
        if(funcAble.isEmpty())
            return null;
        return funcAble;
    }


//    @Override
//    protected void addUnique(Term x) {
//        super.addUnique(x);
//
////            x.sub(0).recurseTerms((Termlike::hasVars), (s -> {
////                if (s instanceof Variable)
////                    vars.add((Variable) s);
////                return true;
////            }), null);
//    }

    @Override
    public @Nullable Term transformAtomic(Atomic x) {
        if (x instanceof Functor) {
            return x;
        }

        if (x instanceof Atom) {
            Functor f = funcResolver.apply((Atom) x);
            if (f != null) {
                return f;
            }
        }
        return x;
    }


//    private Evaluator sortDecreasingVolume() {
//        //TODO only invoke this if the items changed
//        if (size() > 1)
//            ((FasterList<Term>) list).sortThisByInt(Termlike::volume);
//        return this;
//    }

    @Nullable public Evaluation eval(Predicate<Term> each, Term... queries) {
        Evaluation e = new Evaluation(each);

        //iterating at the top level is effectively DFS; a BFS solution is also possible
        for (Term x : queries) {

            e.eval(this, x);
        }
        return e;
    }

    public void print() {


    }
}
