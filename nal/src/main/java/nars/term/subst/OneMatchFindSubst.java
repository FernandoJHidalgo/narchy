package nars.term.subst;

import nars.NAR;
import nars.Op;
import nars.index.TermIndex;
import nars.nal.meta.PremiseEval;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Created by me on 5/24/16.
 */
public final class OneMatchFindSubst extends FindSubst {

    private @Nullable Term xterm;
    private @Nullable PremiseEval target;

    @Nullable private Term result;

    public OneMatchFindSubst(@NotNull NAR nar) {
        this(nar.concepts, nar.random);
    }

    public OneMatchFindSubst(TermIndex index, Random r) {
        super(index, null, r);
    }

    public OneMatchFindSubst(FindSubst parent) {
        this(parent.index, parent.random);
    }


    /**
     * terminates after the first match
     */
    @Override
    public boolean onMatch() {
        //apply the match before the xy/yx mapping gets reverted after leaving the termutator
        if (xterm != null) {
            if (target != null) {
                target.replaceAllXY(this);
                result = target.resolve(xterm, target);
            } else {
                result = resolve(xterm, this);
            }
        }
        return false;
    }

    public boolean tryMatch(@NotNull Op op, @NotNull Term x, @NotNull Term y) {
        tryMatch(op, null, null, x, y);
        return !xy.isEmpty();
    }

    @Nullable
    public Term tryMatch(@NotNull Op op, @Nullable PremiseEval target, @Nullable Term xterm, @NotNull Term x, @NotNull Term y) {
        this.type = op;
        this.xterm = xterm;
        this.target = target;
        this.result = null;
        unify(x, y, true, true);
        return result;
    }

}
