package nars.term.var;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_DEP;

/**
 * normalized dep var
 */
public final class VarDep extends AbstractVariable {

    public VarDep(int id) {
        super(VAR_DEP, id);
    }

    final static int RANK = Term.opX(VAR_DEP, 0);
    @Override public int opX() { return RANK;    }

    @NotNull
    @Override
    public final Op op() {
        return VAR_DEP;
    }

    @Override
    public final int vars() {
        return 1;
    }

    @Override
    public final int varDep() {
        return 1;
    }


}
