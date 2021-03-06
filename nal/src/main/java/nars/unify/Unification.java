package nars.unify;

import nars.term.Term;
import nars.unify.unification.DeterministicUnification;

import java.util.List;
import java.util.function.Function;

/**
 * immutable and memoizable unification result (map of variables to terms) useful for substitution
 */
public interface Unification extends Function<Term,Iterable<Term>> {

    int forkKnown();
    //int forkMax();

    /**
     * indicates unsuccessful unification attempt.
     * TODO distinguish between deterministically impossible and those which stopped before exhausting permutations
     */
    Unification Null = new Unification() {
        @Override
        public Iterable<Term> apply(Term x) {
            return List.of();
        }

        @Override
        public int forkKnown() {
            return 0;
        }
    };

    /**
     * does this happen in any cases besides .equals, ex: conj seq
     */
    DeterministicUnification Self = new DeterministicUnification() {

        @Override
        protected boolean equals(DeterministicUnification obj) {
            return this == obj;
        }

        @Override
        public boolean apply(Unify y) {
            return true;
        }

        @Override
        public Term xy(Term x) {
            return null;
        }
    };



}
