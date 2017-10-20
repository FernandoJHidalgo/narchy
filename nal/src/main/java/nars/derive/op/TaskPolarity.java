package nars.derive.op;

import nars.Op;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import nars.truth.Truth;

/**
 * task truth is postiive
 */
abstract public class TaskPolarity extends AbstractPred<Derivation> {

    public static final PrediTerm<Derivation> beliefContainsTask = new TaskPolarity("BeliefContainsTask") {
        @Override
        public boolean test(Derivation m) {
            return m.beliefTerm.contains(m.taskTerm) || (m.beliefTerm.hasAny(Op.NEG) && m.beliefTerm.contains(m.taskTerm.neg()));
        }

        @Override
        public float cost() {
            return 0.5f;
        }
    };
    public static final PrediTerm<Derivation> taskContainsBelief = new TaskPolarity("TaskContainsBelief") {
        @Override
        public boolean test(Derivation m) {
            return m.taskTerm.contains(m.beliefTerm) || (m.taskTerm.hasAny(Op.NEG) && m.taskTerm.contains(m.beliefTerm.neg()));
        }

        @Override
        public float cost() {
            return 0.5f;
        }
    };
    public static final PrediTerm<Derivation> taskContainsBeliefRecursively = new TaskPolarity("TaskContainsBelief") {
        @Override
        public boolean test(Derivation m) {
            return m.taskTerm.containsRecursively(m.beliefTerm);
        }

        @Override
        public float cost() {
            return 0.5f;
        }
    };
    public static final PrediTerm<Derivation> taskPos = new TaskPolarity("TaskPos") {
        @Override
        public boolean test(Derivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() >= 0.5f);
        }

    };
    public static final PrediTerm<Derivation> taskNeg = new TaskPolarity("TaskNeg") {
        @Override
        public boolean test(Derivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() < 0.5f);
        }
    };
    public static final PrediTerm<Derivation> beliefPos = new TaskPolarity("BeliefPos") {
        @Override
        public boolean test(Derivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() >= 0.5f;
        }
    };
    public static final PrediTerm<Derivation> beliefNeg = new TaskPolarity("BeliefNeg") {
        @Override
        public boolean test(Derivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() < 0.5f;
        }
    };

    @Override
    public float cost() {
        return 0.1f;
    }

    protected TaskPolarity(String x) {
        super(x);
    }
}
