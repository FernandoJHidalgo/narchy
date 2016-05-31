package nars.term.subst.choice;

import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * appended to the end of termuator execution chains to invoke
 * any accumulated termutations occurring during the match
 * or onMatch() if it was stable
 **/
public final class Termunator extends Termutator {

    @NotNull
    private final FindSubst f;
    private final List<Termutator> termutes;
    @NotNull
    final Termutator[] onlyThis;

    public Termunator(@NotNull FindSubst f) {
        super(".");
        this.f = f;
        this.termutes = f.termutes;
        this.onlyThis = new Termutator[] { this };
    }

    /**
     * should be be synchronized if threadsafe necessary
     */
    @Override
    public final void run(@NotNull FindSubst f, Termutator[] ignored, int ignoredAlwaysNegativeOne) {
        if (termutes.isEmpty()) {
            f.onMatch();
        } else {
            Termutator.next(f, next(), -1);
        }
    }

    @NotNull
    private final Termutator[] next() {
        List<Termutator> t = termutes;
        int n = t.size();
        if (n == 0) {
            return onlyThis; //TERMINATE TERMUTATION
        } else {
            t.add(this);
            Termutator[] tt = t.toArray(new Termutator[n]);
            t.clear();
            return tt;
        }
    }

    @Override
    public int getEstimatedPermutations() {
        return 0;
    }
}
