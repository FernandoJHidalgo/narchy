package nars.derive.op;

import nars.$;
import nars.derive.PrediTerm;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/21/16.
 */
public final class MatchOneSubtermPrototype extends UnificationPrototype {

    /**
     * either 0 (task) or 1 (belief)
     */
    private final int subterm;

    private final boolean finish;

    public MatchOneSubtermPrototype(@NotNull Term x, int subterm, boolean finish) {
        super( $.func("unifyTask", $.the(subterm==0 ? "task" : "belief"), x), x );
        this.subterm = subterm;
        this.finish = finish;
    }

    @Override @NotNull
    protected PrediTerm build(@Nullable PrediTerm eachMatch) {
        return new MatchOneSubterm( subterm, pattern, finish ? eachMatch : null);
    }



}