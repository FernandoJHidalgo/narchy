package nars.eval;

import jcog.data.set.ArrayHashSet;
import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.LazyCompound;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.transform.DirectTermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.Null;

/**
 * discovers functors within the provided target, or the target itself.
 * transformation results should not be interned, that is why DirectTermTransform used here
 */
public class Evaluator extends DirectTermTransform /*extends LazyCompound*/ {

    public final Function<Atom, Functor> funcResolver;

    public Evaluator(Function<Atom, Functor> funcResolver) {
        this.funcResolver = funcResolver;
    }

    public Evaluator clone() {
        return new Evaluator(funcResolver);
    }


    /**
     * discover evaluable clauses in the provided term
     */
    @Nullable
    protected ArrayHashSet<Term> clauses(Term x, Evaluation e) {
        if (!x.hasAny(Op.FuncBits))
            return null;

//        if (funcAble!=null)
//            funcAble.clear();
        final ArrayHashSet<Term>[] clauses = new ArrayHashSet[]{null};

        x.recurseTerms(s -> s.hasAll(Op.FuncBits), X -> {
            if (Functor.isFunc(X)) {
                if (clauses[0] != null && clauses[0].contains(X))
                    return true;

                LazyCompound y = new NonEvalLazyCompound().append(X);
                final int[] functors = {0};
                y.updateMap(g -> {
                    if (g instanceof Functor)
                        functors[0]++;
                    else if (g instanceof Atom) {
                        Functor f = funcResolver.apply((Atom) g);
                        if (f != null) {
                            functors[0]++;
                            return f;
                        }
                    }
                    return g;
                });

                if (functors[0] > 0) {

                    Term yy = y.get(HeapTermBuilder.the); //TEMPORARY
                    if (yy.sub(1) instanceof Functor) {
                        if (clauses[0] == null)
                            clauses[0] = new ArrayHashSet<>(1);

                        clauses[0].add(yy);
                    }
                }
            }
            return true;
        }, null);

        return clauses[0];
    }


    @Override
    public @Nullable Term applyAtomic(Atomic x) {
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


    public void eval(Predicate<Term> each, boolean includeTrues, boolean includeFalses, Term... queries) {

        assert (queries.length > 0);

        Evaluation e = new EvaluationTrueFalseFiltered(each, includeTrues, includeFalses);

        //iterating at the top level is effectively DFS; a BFS solution is also possible
        for (Term x : queries) {
            e.evalTry(x, this);
        }

    }

    public void print() {


    }

    private static final class EvaluationTrueFalseFiltered extends Evaluation {
        private final boolean includeTrues, includeFalses;

        public EvaluationTrueFalseFiltered(Predicate<Term> each, boolean includeTrues, boolean includeFalses) {
            super(each);
            this.includeTrues = includeTrues;
            this.includeFalses = includeFalses;
        }

        @Override
        protected Term boolTrue(Term x) {
            return includeTrues ? super.boolTrue(x) : Null;
        }

        @Override
        protected Term boolFalse(Term x) {
            return includeFalses ? super.boolFalse(x) : Null;
        }
    }

    private static class NonEvalLazyCompound extends LazyCompound {
        @Override
        protected boolean evalInline() {
            return false; //TEMPORARY
        }
    }
}
