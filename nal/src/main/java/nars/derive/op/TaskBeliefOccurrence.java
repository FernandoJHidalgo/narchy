package nars.derive.op;

import nars.Task;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * fast task/belief occurrence tests
 */
abstract public class TaskBeliefOccurrence extends AbstractPred<Derivation> {

    TaskBeliefOccurrence(@NotNull String x) {
        super(x);
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Nullable
    public static final PrediTerm bothEvents = new TaskBeliefOccurrence("bothEvents") {

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            return b != null && !b.isEternal() && !m.task.isEternal();
        }
    };

    /**
     * same eternality
     */
    public static final PrediTerm eventsOrEternals = new TaskBeliefOccurrence("eventsOrEternals") {

        @Override
        public boolean test(Derivation m) {
            Task b = m.belief;
            if (b == null)
                return false;
            return m.task.isEternal() == b.isEternal();
        }
    };

    /**
     * both task and belief are temporal or belief precedes task
     */
    public static final PrediTerm after = new TaskBeliefOccurrence("after") {

        @Override
        public boolean test(Derivation m) {
            Task b = m.belief;
            if (b == null)
                return false;
            Task t = m.task;
            return (!t.isEternal() && !b.isEternal()) && (t.start() >= b.start());
        }
    };

    /**
     * both task and belief are eternal, or belief precedes task
     */
    public static final PrediTerm afterOrEternals = new TaskBeliefOccurrence("afterOrEternals") {

        @Override
        public boolean test(Derivation m) {
            Task b = m.belief;
            if (b == null)
                return false;
            Task t = m.task;
            boolean ete = t.isEternal();
            return (ete == b.isEternal()) && (ete || t.start() >= b.end());
        }
    };

}