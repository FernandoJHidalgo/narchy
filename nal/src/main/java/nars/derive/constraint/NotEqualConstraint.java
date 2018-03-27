package nars.derive.constraint;

import nars.term.Term;


public final class NotEqualConstraint extends MatchConstraint.RelationConstraint {

    public NotEqualConstraint(Term target, Term other) {
        super(target, other, "neq");
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean invalid(Term x, Term y) {
        return
                //Terms.equalAtemporally(y, canNotEqual);
                //y.equals(canNotEqual);
                y.equalsRoot(x);
    }


    /** compares term equality, unnegated */
    public static final class NotEqualUnnegConstraint extends RelationConstraint {


        public NotEqualUnnegConstraint(Term target, Term y) {
            super(target, y, "neqUnneg");
        }

        @Override
        public float cost() {
            return 0.2f;
        }

        @Override
        public boolean invalid(Term x, Term y) {

            return
                    //Terms.equalAtemporally(y, canNotEqual);
                    //y.equals(canNotEqual);
                    y.unneg().equalsRoot(x.unneg());
        }


    }

}
